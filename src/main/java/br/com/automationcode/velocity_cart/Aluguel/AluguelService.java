package br.com.automationcode.velocity_cart.Aluguel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import br.com.automationcode.velocity_cart.Audio.TextToSpeechService;
import br.com.automationcode.velocity_cart.Fila.FilaService;
import br.com.automationcode.velocity_cart.Produto.Produto;
import br.com.automationcode.velocity_cart.Produto.ProdutoService;
import br.com.automationcode.velocity_cart.Venda.VendaService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;

@Service
public class AluguelService {

    private static final Logger log = LoggerFactory.getLogger(AluguelService.class);

    private final AluguelRepository aluguelRepository;
    private final Map<Long, Aluguel> alugueisAtivos = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    private ApplicationContext context;

    @Autowired
    private VendaService vendaService;
    @Autowired
    private FilaService filaService;
    @Autowired
    private ProdutoService produtoService;
    @Autowired
    private TextToSpeechService textToSpeechService;

    @Autowired
    public AluguelService(AluguelRepository aluguelRepository) {
        this.aluguelRepository = aluguelRepository;
        log.debug("AluguelService inicializado");
    }

    public List<Aluguel> getAlugueisFinalizadosByFlag() {
        return aluguelRepository.findByFlagView(true);
    }

    @Transactional
    public void setFlagView(Long aluguelId) {
        if (aluguelId == null) {
            log.warn("ID do aluguel é nulo. Operação cancelada.");
            return;
        }

        try {
            Optional<Aluguel> optionalAluguel = aluguelRepository.findById(aluguelId);

            if (optionalAluguel.isEmpty()) {
                log.warn("Aluguel com ID {} não encontrado no banco para atualizar a Flag", aluguelId);
                return;
            }

            Aluguel atual = optionalAluguel.get();
            atual.setFlagView(false);
            aluguelRepository.save(atual);

            log.info("FlagView do aluguel {} atualizada para false", aluguelId);

        } catch (Exception e) {
            log.error("Erro ao atualizar FlagView do aluguel {}: {}", aluguelId, e.getMessage(), e);
        }
    }

    @PostConstruct
    public void iniciarVerificacao() {
        log.info("Iniciando verificação automática de aluguéis...");

        aluguelRepository.findAll().stream()
                .filter(a -> a.getFim() == null && "iniciado".equals(a.getEstado())
                        || a.getFim() == null && "pausado".equals(a.getEstado()))
                .forEach(a -> {
                    alugueisAtivos.put(a.getId(), a);
                    log.debug("Aluguel ativo restaurado: {} - {}", a.getId(), a.getNomeResponsavel());
                });

        scheduler.scheduleWithFixedDelay(() -> {
            try {
                verificarAlugueis();
            } catch (Exception e) {
                log.error("Erro ao verificar aluguéis", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void verificarAlugueis() {
        log.trace("Verificando aluguéis ativos ({} ativos)...", alugueisAtivos.size());

        List<Aluguel> snapshot = new ArrayList<>(alugueisAtivos.values());

        for (Aluguel a : snapshot) {
            try {
                if (!a.isPausado() && a.getTempoRestante() <= 0 && a.getFim() == null) {
                    log.debug("Aluguel expirado detectado: {} - {}", a.getId(), a.getProduto().getNome());
                    AluguelService self = context.getBean(AluguelService.class);
                    self.finalizarAluguelInterno(a);
                }

                if (!a.isPausado() && a.getTempoRestante() <= 60 && a.getTempoRestante() > 59) {
                    log.debug("Aluguel com 60 segundos restantes: {}", a.getId());
                    reproduzirMensagem(a, 1);
                }
            } catch (Exception e) {
                log.error("Erro ao processar aluguel ID {}", a.getId(), e);
            }
        }
    }

    @Transactional
    public void finalizarAluguelInterno(Aluguel a) {
        log.info("Finalizando aluguel interno ID {}", a.getId());
        try {
            // Busca a versão mais recente do banco
            Aluguel atual = aluguelRepository.findByIdForUpdate(a.getId());
            if (atual == null) {
                log.warn("Aluguel {} não encontrado no banco para finalização", a.getId());
                alugueisAtivos.remove(a.getId());
                return;
            }

            // Atualiza o estado e salva
            atual.setFim(LocalDateTime.now());
            atual.setEstado("finalizado");
            atual.setFlagView(true);
            Aluguel salvo = salvarAluguelSeguramente(atual);

            // Remove dos alugueis ativos
            alugueisAtivos.remove(salvo.getId());
            log.debug("Aluguel {} removido de alugueisAtivos", salvo.getId());

            // Executa ações pós-finalização
            reproduzirMensagem(salvo, 0);

            filaService.liberarProximoDaFila(salvo.getProduto().getId()).ifPresent(proximo -> {
                log.info("Próximo da fila encontrado para produto {}: {}", salvo.getProduto().getId(), proximo.getId());
                AluguelService self = context.getBean(AluguelService.class);
                self.iniciarAluguelFila(proximo);
            });

        } catch (Exception e) {
            log.error("Erro ao finalizar aluguel {}", a.getId(), e);
        }
    }

    @Transactional
    public void iniciarAluguelFila(Aluguel proximo) {
        log.info("Iniciando aluguel a partir da fila: {}", proximo.getId());
        proximo.setEstado("iniciado");
        proximo.setInicio(LocalDateTime.now());
        proximo.setUltimaPausa(LocalDateTime.now());
        proximo.setPausado(true);

        Aluguel salvo = salvarAluguelSeguramente(proximo);
        alugueisAtivos.put(salvo.getId(), salvo);
        log.debug("Aluguel {} adicionado à lista de ativos", proximo.getId());
        reproduzirMensagem(salvo, 2);
    }

    @Transactional
    public Aluguel salvarAluguelSeguramente(Aluguel aluguel) {
        log.trace("Salvando aluguel com segurança: {}", aluguel.getId());
        try {
            Aluguel salvo = aluguelRepository.save(aluguel);
            log.debug("Aluguel {} salvo com sucesso", salvo.getId());
            return salvo;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Conflito de versão no aluguel {}, recarregando do banco", aluguel.getId());
            return aluguelRepository.findById(aluguel.getId()).orElse(aluguel);
        }
    }

    @Transactional
    public Aluguel criarAluguel(Aluguel aluguel) {
        log.info("Criando aluguel para produto {}", aluguel.getProduto() != null ? aluguel.getProduto().getId() : null);

        if (aluguel.getProduto() == null)
            throw new IllegalArgumentException("Produto não pode ser nulo");

        Produto produto = produtoService.buscarPorId(aluguel.getProduto().getId());

        if (produto == null)
            throw new IllegalArgumentException("Produto não encontrado: " + aluguel.getProduto().getId());

        if (brinquedoDisponivel(produto.getId())) {
            log.debug("Produto {} disponível. Iniciando aluguel imediatamente.", produto.getId());
            return iniciarAluguel(aluguel, produto);
        } else {
            log.debug("Produto {} ocupado. Adicionando aluguel à fila.", produto.getId());
            return adicionarNaFila(aluguel, produto);
        }

    }

    @Transactional
    private Aluguel iniciarAluguel(Aluguel aluguel, Produto produto) {
        log.info("Iniciando aluguel para produto {}", produto.getId());
        aluguel.setInicio(LocalDateTime.now());
        aluguel.setUltimaPausa(LocalDateTime.now());
        aluguel.setFim(null);
        aluguel.setProduto(produto);
        aluguel.setPausado(false);
        aluguel.setEstado("iniciado");
        aluguel.setTempoRestanteAntesPausa(aluguel.getTempoEscolhido() * 60);

        Aluguel salvo = salvarAluguelSeguramente(aluguel);
        alugueisAtivos.put(salvo.getId(), salvo);
        log.debug("Aluguel {} adicionado a alugueisAtivos", salvo.getId());
        vendaService.registrarAluguel(salvo);
        return salvo;
    }

    @Transactional
    private Aluguel adicionarNaFila(Aluguel aluguel, Produto produto) {
        log.info("Adicionando aluguel à fila para produto {}", produto.getId());
        aluguel.setEstado("fila");
        aluguel.setPausado(false);
        aluguel.setFim(null);
        aluguel.setProduto(produto);
        aluguel.setTempoRestanteAntesPausa(aluguel.getTempoEscolhido() * 60);

        Aluguel salvo = salvarAluguelSeguramente(aluguel);
        vendaService.registrarAluguel(salvo);
        filaService.adicionarNaFila(salvo);
        return salvo;
    }

    @Transactional
    public void pausarAluguel(Long aluguelId) {
        log.info("Pausando aluguel {}", aluguelId);

        // Sempre recarrega a instância atualizada do banco
        Aluguel a = aluguelRepository.findByIdForUpdate(aluguelId);

        if (a == null) {
            log.warn("Aluguel {} não encontrado no banco para pausar.", aluguelId);
            return;
        }

        if (!a.isPausado()) {
            log.debug("Atualizando informações do aluguel {} para pausa.", aluguelId);

            a.setTempoRestanteAntesPausa(a.getTempoRestante());
            a.setUltimaPausa(LocalDateTime.now());
            a.setPausado(true);
            a.setEstado("pausado");

            Aluguel salvo = salvarAluguelSeguramente(a);

            // Atualiza o cache com a nova instância do banco
            alugueisAtivos.put(salvo.getId(), salvo);

            log.info("Aluguel {} pausado com sucesso e cache atualizado.", aluguelId);
        } else {
            log.debug("Aluguel {} já está pausado, nenhuma ação necessária.", aluguelId);
        }
    }

    @Transactional
    public void retomarAluguel(Long aluguelId) {
        log.info("Retomando aluguel {}", aluguelId);

        // Busca a versão mais recente do banco
        Aluguel a = aluguelRepository.findByIdForUpdate(aluguelId);
        if (a == null) {
            log.warn("Aluguel {} não encontrado no banco de dados para retomada", aluguelId);
            return;
        }

        if (a.isPausado()) {
            a.setPausado(false);

            a.setUltimaPausa(
                    LocalDateTime.now().minusSeconds(a.getTempoEscolhido() * 60 - a.getTempoRestanteAntesPausa()));

            a.setEstado("iniciado");

            Aluguel salvo = salvarAluguelSeguramente(a);
            alugueisAtivos.put(salvo.getId(), salvo);

            log.debug("Aluguel {} retomado com sucesso", aluguelId);
        } else {
            log.debug("Aluguel {} não estava pausado, nenhuma ação necessária", aluguelId);
        }
    }

    @Transactional
    public Aluguel finalizarAluguel(Long id) {
        log.info("Finalizando aluguel manualmente {}", id);

        // Busca a versão mais recente do banco
        Aluguel a = aluguelRepository.findByIdForUpdate(id);
        if (a == null) {
            log.warn("Tentativa de finalizar aluguel inexistente: {}", id);
            return null;
        }

        a.setFim(LocalDateTime.now());
        a.setEstado("finalizado");

        Aluguel salvo = salvarAluguelSeguramente(a);

        // Remove dos alugueis ativos
        alugueisAtivos.remove(salvo.getId());

        log.debug("Aluguel {} finalizado e removido dos ativos com sucesso", id);
        return salvo;
    }

    public List<Aluguel> getTodosAlugueis() {
        log.trace("Listando todos os alugueis ativos");
        return List.copyOf(alugueisAtivos.values());
    }

    public boolean brinquedoDisponivel(Long produtoId) {
        boolean disponivel = alugueisAtivos.values().stream()
                .noneMatch(a -> produtoId.equals(a.getProduto().getId()) && a.getFim() == null);
        log.debug("Produto {} disponível? {}", produtoId, disponivel);
        return disponivel;
    }

    public int tempoRestanteByProduto(Long produtoId) {
        return alugueisAtivos.values().stream()
                .filter(a -> produtoId.equals(a.getProduto().getId()) && a.getFim() == null)
                .mapToInt(Aluguel::getTempoRestante)
                .findFirst()
                .orElse(0);
    }

    public void reproduzirMensagem(Aluguel a, int codigo) {
        log.debug("Reproduzindo mensagem (codigo {}) para aluguel {}", codigo, a.getId());
        executor.submit(() -> {
            try {
                switch (codigo) {
                    case 0:
                        for (int i = 0; i < 3; i++) {
                            log.trace("Mensagem finalização para {}", a.getNomeResponsavel());
                            textToSpeechService.speak(a.getNomeResponsavel() + ", o aluguel do brinquedo "
                                    + a.getProduto().getNome() + " terminou.");
                        }
                        break;
                    case 1:
                        textToSpeechService.speak(a.getNomeResponsavel() + ", O aluguel do brinquedo "
                                + a.getProduto().getNome() + " irá terminar em menos de 60 segundos.");
                        break;
                    case 2:
                        textToSpeechService.speak(a.getNomeResponsavel() + ", o brinquedo "
                                + a.getProduto().getNome() + " já está disponível para "
                                + a.getNomeCrianca() + " brincar. Por favor, dirija-se ao balcão.");
                        break;
                    default:
                        log.warn("Código de mensagem desconhecido: {}", codigo);
                        break;
                }
            } catch (Exception e) {
                log.error("Erro ao reproduzir mensagem para aluguel {}", a.getId(), e);
            }
        });
    }

    public void atualizarTempoParaFila() {
        log.trace("Atualizando tempo para fila...");
        List<Aluguel> ativos = getTodosAlugueis();
        filaService.getTodosFilas().forEach(fila -> {
            ativos.stream()
                    .filter(a -> Objects.equals(a.getProduto().getId(), fila.getAluguel().getProduto().getId()))
                    .findFirst()
                    .ifPresent(a -> {
                        fila.setTempoParaIniciar(a.getTempoRestante());
                        filaService.save(fila);
                        log.debug("Fila {} atualizada com tempo restante do aluguel {}", fila.getId(), a.getId());
                    });
        });
    }

    @PreDestroy
    public void shutdownExecutor() {
        System.out.println("Encerrando executores do AluguelService...");

        scheduler.shutdown();
        executor.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Executores finalizados com sucesso!");
    }

}
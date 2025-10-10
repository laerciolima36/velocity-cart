package br.com.automationcode.velocity_cart.Aluguel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import br.com.automationcode.velocity_cart.Audio.TextToSpeechService;
import br.com.automationcode.velocity_cart.Fila.FilaService;
import br.com.automationcode.velocity_cart.Produto.Produto;
import br.com.automationcode.velocity_cart.Produto.ProdutoService;
import br.com.automationcode.velocity_cart.Venda.VendaService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

@Service
public class AluguelService {

    private final AluguelRepository aluguelRepository;
    private final Map<Long, Aluguel> alugueisAtivos = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
    }

    @PostConstruct
    public void iniciarVerificacao() {
        // Inicializa alugueis ativos do banco
        aluguelRepository.findAll().stream()
                .filter(a -> a.getFim() == null && "iniciado".equals(a.getEstado()))
                .forEach(a -> alugueisAtivos.put(a.getId(), a));

        // Agendar verificação periódica
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                verificarAlugueis();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void verificarAlugueis() {
        System.out.println("Verificando alugueis ativos: " + alugueisAtivos.size());

        List<Aluguel> snapshot = new ArrayList<>(alugueisAtivos.values());

        for (Aluguel a : snapshot) {
            // Finalizar alugueis expirados
            if (!a.isPausado() && a.getTempoRestante() <= 0 && a.getFim() == null) {
                finalizarAluguelInterno(a);
            }

            // Mensagem de alerta para 60 segundos restantes
            if (!a.isPausado() && a.getTempoRestante() <= 60 && a.getTempoRestante() > 59) {
                reproduzirMensagem(a, 1);
            }
        }
    }

    private void finalizarAluguelInterno(Aluguel a) {
        try {
            a.setFim(LocalDateTime.now());
            a.setEstado("finalizado");
            salvarAluguelSeguramente(a);
            alugueisAtivos.remove(a.getId());

            reproduzirMensagem(a, 0);

            filaService.liberarProximoDaFila(a.getProduto().getId()).ifPresent(proximo -> {
                iniciarAluguelFila(proximo);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void iniciarAluguelFila(Aluguel proximo) {
        proximo.setEstado("iniciado");
        proximo.setInicio(LocalDateTime.now());
        proximo.setUltimaPausa(LocalDateTime.now());
        proximo.setPausado(true);

        salvarAluguelSeguramente(proximo);
        alugueisAtivos.put(proximo.getId(), proximo);
        reproduzirMensagem(proximo, 2);
    }

    @Transactional
    public Aluguel salvarAluguelSeguramente(Aluguel aluguel) {
        try {
            return aluguelRepository.save(aluguel);
        } catch (ObjectOptimisticLockingFailureException e) {
            System.out.println("Aluguel já atualizado por outra transação: " + aluguel.getId());
            // Recarrega do banco para sincronizar estado
            return aluguelRepository.findById(aluguel.getId()).orElse(aluguel);
        }
    }

    public Aluguel criarAluguel(Aluguel aluguel) {
        if (aluguel.getProduto() == null)
            throw new IllegalArgumentException("Produto não pode ser nulo");

        Produto produto = produtoService.buscarPorId(aluguel.getProduto().getId());

        return brinquedoDisponivel(produto.getId())
                ? iniciarAluguel(aluguel, produto)
                : adicionarNaFila(aluguel, produto);
    }

    private Aluguel iniciarAluguel(Aluguel aluguel, Produto produto) {
        aluguel.setInicio(LocalDateTime.now());
        aluguel.setUltimaPausa(LocalDateTime.now());
        aluguel.setFim(null);
        aluguel.setProduto(produto);
        aluguel.setPausado(false);
        aluguel.setEstado("iniciado");
        aluguel.setTempoRestanteAntesPausa(aluguel.getTempoEscolhido() * 60);

        Aluguel salvo = salvarAluguelSeguramente(aluguel);
        alugueisAtivos.put(salvo.getId(), salvo);
        vendaService.registrarVenda(salvo);
        return salvo;
    }

    private Aluguel adicionarNaFila(Aluguel aluguel, Produto produto) {
        aluguel.setEstado("fila");
        aluguel.setPausado(false);
        aluguel.setFim(null);
        aluguel.setProduto(produto);
        aluguel.setTempoRestanteAntesPausa(aluguel.getTempoEscolhido() * 60);

        Aluguel salvo = salvarAluguelSeguramente(aluguel);
        vendaService.registrarVenda(salvo);
        filaService.adicionarNaFila(salvo);
        return aluguel;
    }

    public void pausarAluguel(Long aluguelId) {
        Aluguel a = alugueisAtivos.get(aluguelId);
        if (a != null && !a.isPausado()) {
            a.setTempoRestanteAntesPausa(a.getTempoRestante());
            a.setUltimaPausa(LocalDateTime.now());
            a.setPausado(true);
            a.setEstado("pausado");
            salvarAluguelSeguramente(a);
        }
    }

    public void retomarAluguel(Long aluguelId) {
        Aluguel a = alugueisAtivos.get(aluguelId);
        if (a != null && a.isPausado()) {
            a.setPausado(false);
            a.setUltimaPausa(LocalDateTime.now()
                    .minusSeconds(a.getTempoEscolhido() * 60 - a.getTempoRestanteAntesPausa()));
            a.setEstado("iniciado");
            salvarAluguelSeguramente(a);
        }
    }

    public Aluguel finalizarAluguel(Long id) {
        Aluguel a = alugueisAtivos.get(id);
        if (a != null) {
            a.setFim(LocalDateTime.now());
            a.setEstado("finalizado");
            salvarAluguelSeguramente(a);
            alugueisAtivos.remove(id);
            return a;
        }
        return null;
    }

    public List<Aluguel> getTodosAlugueis() {
        return List.copyOf(alugueisAtivos.values());
    }

    public boolean brinquedoDisponivel(Long produtoId) {
        return alugueisAtivos.values().stream()
                .noneMatch(a -> produtoId.equals(a.getProduto().getId()) && a.getFim() == null);
    }

    public void reproduzirMensagem(Aluguel a, int codigo) {
        executor.submit(() -> {
            try {
                switch (codigo) {
                    case 0:
                        for (int i = 0; i < 3; i++)
                            textToSpeechService
                                    .speak(a.getNomeResponsavel() + ", o aluguel do brinquedo "
                                            + a.getProduto().getNome()
                                            + " terminou.");
                        break;
                    case 1:
                        textToSpeechService.speak(a.getNomeResponsavel() + ", O aluguel do brinquedo "
                                + a.getProduto().getNome() + " irá terminar em menos 60 segundos.");
                        break;
                    case 2:
                        textToSpeechService.speak(a.getNomeResponsavel() + ", o brinquedo "
                                + a.getProduto().getNome()
                                + " já está disponível para " + a.getNomeCrianca() + " brincar. Por favor, dirija-se ao balcão.");
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Atualiza tempo restante para cada fila
    public void atualizarTempoParaFila() {
        List<Aluguel> ativos = getTodosAlugueis();
        filaService.getTodosFilas().forEach(fila -> {
            ativos.stream()
                    .filter(a -> Objects.equals(a.getProduto().getId(), fila.getAluguel().getProduto().getId()))
                    .findFirst()
                    .ifPresent(a -> {
                        fila.setTempoParaIniciar(a.getTempoRestante());
                        filaService.save(fila);
                    });
        });
    }
}

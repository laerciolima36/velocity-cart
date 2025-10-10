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
import br.com.automationcode.velocity_cart.Fila.Fila;
import br.com.automationcode.velocity_cart.Fila.FilaService;
import br.com.automationcode.velocity_cart.Produto.Produto;
import br.com.automationcode.velocity_cart.Produto.ProdutoService;
import br.com.automationcode.velocity_cart.Venda.VendaService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

@Service
public class AluguelService {

    private final AluguelRepository aluguelRepository;
    private final Map<Long, Aluguel> alugueisAtivos = new ConcurrentHashMap<>(); // Map para aluguéis ativos
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(); // para verificar
                                                                                                     // aluguéis ativos
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // para mensagens de voz

    @Autowired
    VendaService vendaService;

    @Autowired
    FilaService filaService;

    @Autowired
    ProdutoService produtoService;

    @Autowired
    TextToSpeechService textToSpeechService;

    @Autowired
    public AluguelService(AluguelRepository aluguelRepository) {
        this.aluguelRepository = aluguelRepository;

        // Reinicia timers para aluguéis ainda ativos
        aluguelRepository.findAll().forEach(a -> {
            if (a.getFim() == null && a.getEstado().equals("iniciado")) {
                alugueisAtivos.put(a.getId(), a);
            }
        });
    }

    @PostConstruct
    public void iniciarVerificacao() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                verificarAlugueis();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void verificarAlugueis() {
        System.out.println("Verificando aluguéis ativos...");

        // Criar uma cópia para iterar, evitando problemas ao remover enquanto itera
        List<Aluguel> alugueisSnapshot = new ArrayList<>(alugueisAtivos.values());

        for (Aluguel a : alugueisSnapshot) {
            // Verificação de tempo com tolerância
            if (!a.isPausado() && a.getTempoRestante() <= 0 && a.getFim() == null) {
                try {
                    // Atualiza estado de forma segura
                    a.setFim(LocalDateTime.now());
                    a.setEstado("finalizado");

                    // Salva dentro de uma transação separada
                    salvarAluguelSeguramente(a);

                    // Remove do mapa após salvar
                    alugueisAtivos.remove(a.getId());

                    // Reproduz mensagem de finalização de forma sequencial
                    reproduzirMensagem(a, 0);

                    // Libera próximo da fila
                    filaService.liberarProximoDaFila(a.getProduto().getId()).ifPresent(proximo -> {
                        proximo.setEstado("iniciado");
                        proximo.setInicio(LocalDateTime.now());
                        proximo.setUltimaPausa(LocalDateTime.now());
                        proximo.setPausado(true);

                        // Salva o próximo aluguel de forma segura
                        salvarAluguelSeguramente(proximo);

                        // Adiciona ao mapa depois de salvar
                        alugueisAtivos.put(proximo.getId(), proximo);

                        // Reproduz mensagem para o próximo aluguel
                        reproduzirMensagem(proximo, 2);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Mensagem de alerta para 60 segundos restantes (usa range tolerante)
            if (!a.isPausado() && a.getTempoRestante() <= 60 && a.getTempoRestante() > 59) {
                reproduzirMensagem(a, 1);
            }
        }
    }

    // Método auxiliar para salvar alugueis de forma segura em JPA
    @Transactional
    public void salvarAluguelSeguramente(Aluguel aluguel) {
        try {
            aluguelRepository.save(aluguel);
        } catch (ObjectOptimisticLockingFailureException e) {
            System.out.println("Aluguel já atualizado por outra transação: " + aluguel.getId());
            // Aqui você pode recarregar e tentar novamente, se necessário
        }
    }

    public Aluguel criarAluguel(Aluguel aluguel) {
        if (aluguel.getProduto() == null) {
            throw new IllegalArgumentException("Produto não pode ser nulo");
        }

        Produto produto = produtoService.buscarPorId(aluguel.getProduto().getId());

        if (brinquedoDisponivel(aluguel.getProduto().getId())) {
            return iniciarAluguel(aluguel, produto);
        } else {
            return adicionarNaFila(aluguel, produto);
        }
    }

    private Aluguel iniciarAluguel(Aluguel aluguel, Produto produto) {
        aluguel.setInicio(LocalDateTime.now()); // para referencia no banco de dados, para saber que horas comecou
        aluguel.setUltimaPausa(LocalDateTime.now()); // usado como referencia para calcular o tempo restante
        aluguel.setFim(null);
        aluguel.setProduto(produto);
        aluguel.setPausado(false);
        aluguel.setEstado("iniciado");
        aluguel.setTempoRestanteAntesPausa(aluguel.getTempoEscolhido() * 60);

        Aluguel aluguelSalvo = aluguelRepository.save(aluguel);
        alugueisAtivos.put(aluguelSalvo.getId(), aluguelSalvo);
        vendaService.registrarVenda(aluguelSalvo);
        return aluguelSalvo;
    }

    private Aluguel adicionarNaFila(Aluguel aluguel, Produto produto) {
        aluguel.setEstado("fila");
        aluguel.setPausado(false);
        aluguel.setFim(null);
        aluguel.setProduto(produto);
        aluguel.setTempoRestanteAntesPausa(aluguel.getTempoEscolhido() * 60);

        Aluguel aluguelSalvo = aluguelRepository.save(aluguel);

        vendaService.registrarVenda(aluguelSalvo);

        filaService.adicionarNaFila(aluguelSalvo);

        return aluguel;
    }

    public void pausarAluguel(Long aluguelId) {
        Aluguel a = alugueisAtivos.get(aluguelId);
        if (a != null && !a.isPausado()) {
            a.setTempoRestanteAntesPausa(a.getTempoRestante());
            a.setUltimaPausa(LocalDateTime.now());
            a.setPausado(true);
            a.setEstado("pausado");
            aluguelRepository.save(a);
        }
    }

    public void retomarAluguel(Long aluguelId) {
        Aluguel a = alugueisAtivos.get(aluguelId);
        if (a != null && a.isPausado()) {
            a.setPausado(false);
            a.setUltimaPausa(
                    LocalDateTime.now().minusSeconds(a.getTempoEscolhido() * 60 - a.getTempoRestanteAntesPausa()));
            a.setEstado("iniciado");
            // Ajusta o início considerando o tempo restante
            // a.setInicio(LocalDateTime.now().minusSeconds(a.getTempoEscolhido() * 60 -
            // a.getTempoRestanteAntesPausa()));
            aluguelRepository.save(a);
        }
    }

    public Aluguel finalizarAluguel(Long id) {
        Aluguel a = alugueisAtivos.get(id);
        if (a != null) {
            a.setFim(LocalDateTime.now());
            a.setEstado("finalizado");
            aluguelRepository.save(a);
            alugueisAtivos.remove(id);
            return a;
        }
        return null;
    }

    public List<Aluguel> getTodosAlugueis() {
        return List.copyOf(alugueisAtivos.values());
    }

    public boolean brinquedoDisponivel(Long produtoId) { // verificar se o brinquedo esta disponivel
        for (Aluguel a : alugueisAtivos.values()) {
            if (Objects.equals(produtoId, a.getProduto().getId())
                    && a.getFim() == null) {
                return false; // brinquedo nao disponivel
            }
        }
        return true; // brinquedo disponivel
    }

    public void reproduzirMensagem(Aluguel a, int codigo) {

        executor.submit(() -> {
            try {
                switch (codigo) {
                    case 0:
                        textToSpeechService
                                .speak(a.getNomeResponsavel() + ", o aluguel do brinquedo "
                                        + a.getProduto().getNome()
                                        + " terminou.");
                        textToSpeechService
                                .speak(a.getNomeResponsavel() + ", o aluguel do brinquedo "
                                        + a.getProduto().getNome()
                                        + " terminou.");
                        textToSpeechService
                                .speak(a.getNomeResponsavel() + ", o aluguel do brinquedo "
                                        + a.getProduto().getNome()
                                        + " terminou.");
                        break;

                    case 1:
                        textToSpeechService
                                .speak(a.getNomeResponsavel() + ", O aluguel do brinquedo " + a.getProduto().getNome()
                                        + " irá terminar em 60 segundos.");
                        break;

                    case 2:
                        textToSpeechService
                                .speak(a.getNomeResponsavel() + ", o brinquedo " + a.getProduto().getNome()
                                        + " já está disponível para você brincar. Por favor, dirija-se ao balcão.");
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // chamar apenas quando for listar a fila
    private void tempoParaIniciarFila() {
        List<Aluguel> alugueisAtivos = getTodosAlugueis();

        List<Fila> todosFilas = filaService.getTodosFilas();

        for (Fila fila : todosFilas) {
            for (Aluguel aluguel : alugueisAtivos) {
                if (fila.getAluguel().getProduto().getId().equals(aluguel.getProduto().getId())) {
                    long tempoRestante = aluguel.getTempoRestante();
                    fila.setTempoParaIniciar(tempoRestante);
                    filaService.save(fila);
                }
            }
        }
    }
}
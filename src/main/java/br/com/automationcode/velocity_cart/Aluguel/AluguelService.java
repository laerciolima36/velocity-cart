package br.com.automationcode.velocity_cart.Aluguel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.automationcode.velocity_cart.Fila.FilaService;
import br.com.automationcode.velocity_cart.Produto.Produto;
import br.com.automationcode.velocity_cart.Produto.ProdutoService;
import br.com.automationcode.velocity_cart.Venda.VendaService;

@Service
public class AluguelService {

    private final AluguelRepository aluguelRepository;
    private final Map<Long, Aluguel> alugueisAtivos = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    VendaService vendaService;

    @Autowired
    FilaService filaService;

    @Autowired
    ProdutoService produtoService;

    // @Autowired
    // TextToSpeechService textToSpeechService;

    @Autowired
    public AluguelService(AluguelRepository aluguelRepository) {
        this.aluguelRepository = aluguelRepository;

        // Reinicia timers para aluguéis ainda ativos
        aluguelRepository.findAll().forEach(a -> {
            if (a.getFim() == null && a.getEstado().equals("iniciado")) {
                alugueisAtivos.put(a.getId(), a);
            }
        });

        // Scheduler que verifica periodicamente se algum aluguel terminou
        scheduler.scheduleAtFixedRate(this::verificarAlugueis, 0, 1, TimeUnit.SECONDS);
    }

    public Aluguel criarAluguel(Aluguel aluguel) {
        if (aluguel.getProduto() == null) {
            throw new IllegalArgumentException("Produto não pode ser nulo");
        }

        Produto produto = produtoService.buscarPorId(aluguel.getProduto().getId());

        if (brinquedoDisponivel(aluguel.getProduto().getId())) {

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

        } else {
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
    }

    public void pausarAluguel(Long id) {
        Aluguel a = alugueisAtivos.get(id);
        if (a != null && !a.isPausado()) {
            a.setTempoRestanteAntesPausa(a.getTempoRestante());
            a.setUltimaPausa(LocalDateTime.now());
            a.setPausado(true);
            a.setEstado("pausado");
            aluguelRepository.save(a);
        }
    }

    public void retomarAluguel(Long id) {
        Aluguel a = alugueisAtivos.get(id);
        if (a != null && a.isPausado()) {
            a.setPausado(false);
            a.setUltimaPausa(LocalDateTime.now().minusSeconds(a.getTempoEscolhido() * 60 - a.getTempoRestanteAntesPausa()));
            a.setEstado("iniciado");
            // Ajusta o início considerando o tempo restante
            // a.setInicio(LocalDateTime.now().minusSeconds(a.getTempoEscolhido() * 60 -
            // a.getTempoRestanteAntesPausa()));
            aluguelRepository.save(a);
        }
    }

    public void verificarAlugueis() {
        System.out.println("Verificando aluguéis ativos...");
        alugueisAtivos.values().forEach(a -> {
            if (!a.isPausado() && a.getTempoRestante() <= 0 && a.getFim() == null) {
                System.out.println("Entrou no if do verificarAlugueis");
                a.setFim(LocalDateTime.now());
                a.setEstado("finalizado");
                aluguelRepository.save(a);
                alugueisAtivos.remove(a.getId());
                
                filaService.liberarProximoDaFila(a.getProduto().getId()).ifPresent(proximo -> {
                    proximo.setEstado("iniciado");
                    proximo.setInicio(LocalDateTime.now());
                    proximo.setUltimaPausa(LocalDateTime.now());
                    alugueisAtivos.put(proximo.getId(), proximo);
                    proximo.setPausado(true);
                    aluguelRepository.save(proximo);
                });
            }
        });
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

    public void reproduzirMensagem(String mensagem) {
        try {
            //textToSpeechService.synthesizeAndPlay(mensagem);
        } catch (Exception e) {
            System.err.println("Erro ao reproduzir mensagem: " + e.getMessage());
        }
    }
}

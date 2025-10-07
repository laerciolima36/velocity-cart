package br.com.automationcode.velocity_cart.Fila;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.automationcode.velocity_cart.Aluguel.Aluguel;
import br.com.automationcode.velocity_cart.Aluguel.AluguelService;

@Service
public class FilaService {

    @Autowired
    AluguelService aluguelService;

    private final FilaRepository filaRepository;

    public FilaService(FilaRepository filaRepository) {
        this.filaRepository = filaRepository;
    }

    @Transactional
    public List<Fila> getTodosFilas() {
        tempoParaIniciarFila();
        return filaRepository.findAll();
    }

    @Transactional
    public Fila adicionarNaFila(Aluguel aluguel) {
        Fila fila = new Fila(aluguel);
        return filaRepository.save(fila);
    }

    public List<Fila> listarFilaPorProduto(Long produtoId) {
        return filaRepository.findByAluguel_Produto_IdOrderByDataEntradaAsc(produtoId);
    }

    @Transactional
    public Optional<Aluguel> liberarProximoDaFila(Long produtoId) {
        List<Fila> filaDoProduto = listarFilaPorProduto(produtoId);
        if (!filaDoProduto.isEmpty()) {
            Fila proximo = filaDoProduto.get(0);
            filaRepository.delete(proximo);
            return Optional.of(proximo.getAluguel());
        }
        return Optional.empty();
    }

    public long contarFilaPorProduto(Long produtoId) {
        return filaRepository.countByAluguel_Produto_Id(produtoId);
    }

    // chamar apenas quando for listar a fila
    private void tempoParaIniciarFila() {
        List<Aluguel> alugueisAtivos = aluguelService.getTodosAlugueis();

        List<Fila> todosFilas = filaRepository.findAll();

        for (Fila fila : todosFilas) {
            for (Aluguel aluguel : alugueisAtivos) {
                if (fila.getAluguel().getProduto().getId().equals(aluguel.getProduto().getId())) {
                    long tempoRestante = aluguel.getTempoRestante();
                    fila.setTempoParaIniciar(tempoRestante);
                    filaRepository.save(fila);
                }
            }
        }
    }
}
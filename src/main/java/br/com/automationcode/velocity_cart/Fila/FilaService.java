package br.com.automationcode.velocity_cart.Fila;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.automationcode.velocity_cart.Aluguel.Aluguel;

@Service
public class FilaService {

    private final FilaRepository filaRepository;

    public FilaService(FilaRepository filaRepository) {
        this.filaRepository = filaRepository;
    }

    @Transactional
    public List<Fila> getTodosFilas() {
        // tempoParaIniciarFila();
        return filaRepository.findAll();
    }

    public Fila deleteById(Long id) {
        Optional<Fila> filaOptional = filaRepository.findById(id);
        if (filaOptional.isPresent()) {
            Fila fila = filaOptional.get();
            filaRepository.delete(fila);
            return fila;
        } else {
            throw new IllegalArgumentException("Fila não encontrada com o ID: " + id);
        }
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

    public void save(Fila fila) {
        filaRepository.save(fila);
    }

}
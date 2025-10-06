package br.com.automationcode.velocity_cart.Venda;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.automationcode.velocity_cart.Aluguel.Aluguel;
import br.com.automationcode.velocity_cart.Produto.ProdutoService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VendaService {

    @Autowired
    VendaRepository vendaRepository;

    @Autowired
    ProdutoService produtoService;

    public Venda save(Venda venda) {
        try {
            venda.getItens().forEach(item -> {
                produtoService.diminuirQuantidadeEstoque(item.getProduto().getId(), item.getQuantidade());
            });
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar a venda: " + e.getMessage());
        }

        return vendaRepository.save(venda);
    }

    public List<Venda> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return vendaRepository.findByDataVendaBetween(inicio, fim);
    }

    public void registrarVenda(Aluguel aluguel) { //ANALISAR
        ItemVenda item = new ItemVenda();
        item.setProduto(aluguel.getProduto());
        item.setQuantidade(1); // Sempre 1 unidade por aluguel
    }

}
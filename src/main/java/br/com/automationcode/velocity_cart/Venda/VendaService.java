package br.com.automationcode.velocity_cart.Venda;

import java.math.BigDecimal;
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
        // try {
        //     venda.getItens().forEach(item -> {
        //         produtoService.diminuirQuantidadeEstoque(item.getProduto().getId(), item.getQuantidade());
        //     });
        // } catch (Exception e) {
        //     throw new RuntimeException("Erro ao salvar a venda: " + e.getMessage());
        // }

        return vendaRepository.save(venda);
    }

    public List<Venda> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return vendaRepository.findByDataVendaBetween(inicio, fim);
    }

    public void registrarVenda(Aluguel aluguel) { // ANALISAR

    }

    public void registrarAluguel(Aluguel aluguel) {
        Venda venda = new Venda();
        venda.setDataVenda(LocalDateTime.now());
        venda.setDesconto(BigDecimal.ZERO);
        venda.setAtendente(aluguel.getAtendente());
        venda.setFormaPagamento(aluguel.getFormaPagamento());

        ItemVenda item = new ItemVenda();
        item.setProduto(aluguel.getProduto());
        item.setQuantidade(aluguel.getTempoEscolhido()); // a quantidade é o tempo em minutos
        item.setPrecoUnitario(aluguel.getProduto().getPrecoVenda()); // o preco venda e o preco do minuto
        item.setVenda(venda);

        venda.setItens(List.of(item));


        BigDecimal subtotal = venda.getItens().stream()
                .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        venda.setSubTotal(subtotal);
        venda.setValorTotal(subtotal.subtract(venda.getDesconto()));

        
        this.save(venda);
    }

}
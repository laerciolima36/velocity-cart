package br.com.automationcode.velocity_cart.Aluguel;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.automationcode.velocity_cart.Fila.Fila;
import br.com.automationcode.velocity_cart.Fila.FilaService;
import br.com.automationcode.velocity_cart.Produto.ProdutoService;

@Service
public class InformacoesService {

    @Autowired
    ProdutoService produtoService;

    @Autowired
    FilaService filaService;

    @Autowired
    AluguelService aluguelService;

    public List<InfoDTO> getInfo() {
        List<InfoDTO> listaInfo = new ArrayList<>();

        try {
            produtoService.listarTodos().forEach(produto -> {
                try {
                    boolean disponivel = aluguelService.brinquedoDisponivel(produto.getId());
                    long quantidadeEsperando = filaService.contarFilaPorProduto(produto.getId());
                    int tempoEspera = filaService.calcularTempoEspera(produto.getId());
                    int tempoRestante = aluguelService.tempoRestanteByProduto(produto.getId()) / 60;
                    int tempoTotalEspera = tempoEspera + tempoRestante;
                    List<Fila> fila = filaService.listarFilaPorProduto(produto.getId());

                    InfoDTO info = new InfoDTO(
                        produto.getId(),
                        produto.getNome(),
                        produto.getFotoProduto(),
                        disponivel,
                        quantidadeEsperando,
                        tempoTotalEspera,
                        fila
                    );

                    listaInfo.add(info);
                } catch (Exception e) {
                    // Log and skip this produto if any exception occurs
                    System.err.println("Erro ao processar produto ID " + produto.getId() + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            // Log and return empty list if a major error occurs
            System.err.println("Erro ao listar produtos: " + e.getMessage());
        }

        return listaInfo;
    }

}
package br.com.automationcode.velocity_cart.Aluguel;

import java.util.List;

import br.com.automationcode.velocity_cart.Fila.Fila;

public record InfoDTO(Long produtoId, String nomeProduto, String fotoProduto, boolean disponivel, long quantidadeNaFila, int tempoEspera, List<Fila> fila) {

}

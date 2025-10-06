package br.com.automationcode.velocity_cart.Fila;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FilaRepository extends JpaRepository<Fila, Long> {

    // Agora busca a fila pelo produto do aluguel
    List<Fila> findByAluguel_Produto_IdOrderByDataEntradaAsc(Long produtoId);

    long countByAluguel_Produto_Id(Long produtoId);
}

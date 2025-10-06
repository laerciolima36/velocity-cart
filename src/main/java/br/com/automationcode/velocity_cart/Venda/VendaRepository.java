package br.com.automationcode.velocity_cart.Venda;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long>{
    List<Venda> findByDataVendaBetween(LocalDateTime inicio, LocalDateTime fim);

}

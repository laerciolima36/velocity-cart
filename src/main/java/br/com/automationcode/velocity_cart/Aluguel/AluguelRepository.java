package br.com.automationcode.velocity_cart.Aluguel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface AluguelRepository extends JpaRepository<Aluguel, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Aluguel a WHERE a.id = :id")
    Aluguel findByIdForUpdate(@Param("id") Long id);
}
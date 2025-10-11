package br.com.automationcode.velocity_cart.Audio;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VozRepository extends JpaRepository<Voz, Long> {
    Voz findByAtivaTrue();

}

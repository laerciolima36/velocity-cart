package br.com.automationcode.velocity_cart.Fila;

import java.time.LocalDateTime;

import br.com.automationcode.velocity_cart.Aluguel.Aluguel;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fila")
public class Fila {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aluguel_id")
    private Aluguel aluguel;

    private LocalDateTime dataEntrada = LocalDateTime.now();

    private long tempoParaIniciar; // em minutos

    public Fila() {
    }

    public Fila(Aluguel aluguel) {
        this.aluguel = aluguel;
        this.dataEntrada = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public Aluguel getAluguel() {
        return aluguel;
    }

    public void setAluguel(Aluguel aluguel) {
        this.aluguel = aluguel;
    }

    public LocalDateTime getDataEntrada() {
        return dataEntrada;
    }

    public void setDataEntrada(LocalDateTime dataEntrada) {
        this.dataEntrada = dataEntrada;
    }

    public long getTempoParaIniciar() {
        return tempoParaIniciar;
    }

    public void setTempoParaIniciar(long tempoParaIniciar) {
        this.tempoParaIniciar = tempoParaIniciar;
    }
}

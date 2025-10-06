package br.com.automationcode.velocity_cart.Aluguel;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import br.com.automationcode.velocity_cart.Produto.Produto;

@Entity
public class Aluguel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeResponsavel;
    private String nomeCrianca;
    private int tempoEscolhido; // em minutos
    private boolean pago;

    private LocalDateTime inicio;
    private LocalDateTime fim;

    private boolean pausado;
    private LocalDateTime ultimaPausa; // momento em que entrou em pausa
    private int tempoRestanteAntesPausa; // segundos restantes quando pausou

    private String estado;

    @ManyToOne
    private Produto produto;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeResponsavel() {
        return nomeResponsavel;
    }

    public void setNomeResponsavel(String nomeResponsavel) {
        this.nomeResponsavel = nomeResponsavel;
    }

    public String getNomeCrianca() {
        return nomeCrianca;
    }

    public void setNomeCrianca(String nomeCrianca) {
        this.nomeCrianca = nomeCrianca;
    }

    public int getTempoEscolhido() {
        return tempoEscolhido;
    }

    public void setTempoEscolhido(int tempoEscolhido) {
        this.tempoEscolhido = tempoEscolhido;
    }

    public boolean isPago() {
        return pago;
    }

    public void setPago(boolean pago) {
        this.pago = pago;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public void setFim(LocalDateTime fim) {
        this.fim = fim;
    }

    public boolean isPausado() {
        return pausado;
    }

    public void setPausado(boolean pausado) {
        this.pausado = pausado;
    }

    public LocalDateTime getUltimaPausa() {
        return ultimaPausa;
    }

    public void setUltimaPausa(LocalDateTime ultimaPausa) {
        this.ultimaPausa = ultimaPausa;
    }

    public int getTempoRestanteAntesPausa() {
        return tempoRestanteAntesPausa;
    }

    public void setTempoRestanteAntesPausa(int tempoRestanteAntesPausa) {
        this.tempoRestanteAntesPausa = tempoRestanteAntesPausa;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public String getEstado() {
        return estado;
    }   
    
    public void setEstado(String estado) {
        this.estado = estado;
    }

    // Calcula tempo restante dinamicamente em segundos
    public int getTempoRestante() {
        if (fim != null)
            return 0; // já terminou
        int totalSegundos = tempoEscolhido * 60;

        if (pausado && ultimaPausa != null) {
            return tempoRestanteAntesPausa; // retorna o valor congelado
        } else if (ultimaPausa != null) {
            long segundosPassados = java.time.Duration.between(ultimaPausa, LocalDateTime.now()).getSeconds();
            int restante = totalSegundos - (int) segundosPassados;
            return Math.max(restante, 0);
        } else {
            return totalSegundos;
        }
    }
}
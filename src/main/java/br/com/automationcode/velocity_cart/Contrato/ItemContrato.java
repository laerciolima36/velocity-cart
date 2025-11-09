package br.com.automationcode.velocity_cart.Contrato;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;
import br.com.automationcode.velocity_cart.Produto.Produto;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "itens_contrato")
public class ItemContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relação com o contrato principal
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id")
    @JsonIgnore
    private Contrato contrato;

    // Produto (brinquedo)
    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    // Tempo de uso do brinquedo (em minutos, por exemplo)
    @Column(nullable = false)
    private Integer tempoUso;

    // Valor acordado para esse brinquedo
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal valor;

    // Tolerância máxima de uso (em minutos, por exemplo)
    @Column(nullable = false)
    private Integer toleranciaMaxima;
}
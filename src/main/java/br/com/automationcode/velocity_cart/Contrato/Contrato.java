package br.com.automationcode.velocity_cart.Contrato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "contratos")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Número do contrato gerado automaticamente
    @Column(name = "numero_contrato", unique = true, nullable = false)
    private String numeroContrato;

    @Column(nullable = false)
    private String nomeContratante;

    @Column(nullable = false)
    private String enderecoContratante;

    @Column(nullable = false)
    private String numeroEndContratante;

    @Column(nullable = false)
    private String bairroContratante;

    @Column(nullable = false)
    private String telefoneContratante;

    private LocalDate dataInicio;

    private LocalTime horaInicio;

    private boolean finalizado;

    @Lob
    private String assinatura; // Aqui fica a string base64

    // Valor total do contrato
    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;

    // Itens do contrato
    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemContrato> itens = new ArrayList<>();

    @PrePersist
    public void aoSalvar() {
        this.numeroContrato = "C-" + System.currentTimeMillis();
        calcularValorTotal();
    }

    @PreUpdate
    public void aoAtualizar() {
        calcularValorTotal();
    }

    public void calcularValorTotal() {
        if (itens != null && !itens.isEmpty()) {
            this.valorTotal = itens.stream()
                .map(ItemContrato::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            this.valorTotal = BigDecimal.ZERO;
        }
    }
}

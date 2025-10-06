package br.com.automationcode.velocity_cart.Contrato;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
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

    // Número do contrato gerado automaticamente (pode ser UUID ou sequencial)
    @Column(name = "numero_contrato", unique = true, nullable = false)
    private String numeroContrato;

    @Column(nullable = false)
    private String nomeContratante;

    @Column(nullable = false)
    private String enderecoContratante;

    @Column(nullable = false)
    private String telefoneContratante;

    // Relacionamento com os itens do contrato
    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemContrato> itens = new ArrayList<>();

    @PrePersist
    public void gerarNumeroContrato() {
        // Exemplo simples: C-20251006-0001
        this.numeroContrato = "C-" + System.currentTimeMillis();
    }
}

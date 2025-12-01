package br.com.automationcode.velocity_cart.Venda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vendas")
@CrossOrigin(origins = "*")
public class VendaController {

    @Autowired
    VendaService vendaService;

    @Operation(summary = "Salva uma nova venda, calcula subtotal e valor total, e atualiza o estoque dos produtos.")
    @PostMapping
    public ResponseEntity<Venda> salvarVenda(@RequestBody Venda venda) {
        venda.setDataVenda(LocalDateTime.now());
        BigDecimal subtotal = venda.getItens().stream()
                .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        venda.setSubTotal(subtotal);
        venda.setValorTotal(subtotal.subtract(venda.getDesconto()));

        for (ItemVenda item : venda.getItens()) {
            item.setVenda(venda); // Definir a venda para cada item
        }
        Venda salva = vendaService.save(venda);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    @Operation(summary = "Busca vendas por período, retornando uma lista de vendas entre as datas fornecidas.")
    @GetMapping("/periodo")
    public ResponseEntity<List<Venda>> buscarPorPeriodo(
            @RequestParam("inicio") String inicio,
            @RequestParam("fim") String fim) {

        LocalDateTime dataInicio = LocalDate.parse(inicio).atStartOfDay();
        LocalDateTime dataFim = LocalDate.parse(fim).atTime(LocalTime.MAX);

        List<Venda> vendas = vendaService.buscarPorPeriodo(dataInicio, dataFim);
        return ResponseEntity.ok(vendas);
    }

    @Operation(summary = "Deleta a venda de acordo com o ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarVenda(@PathVariable Long id) {
        vendaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
    
}
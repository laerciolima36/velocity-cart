package br.com.automationcode.velocity_cart.Contrato;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;

    @PostMapping
    public ResponseEntity<Contrato> criar(@RequestBody Contrato contrato) {
        Contrato novoContrato = contratoService.salvarContrato(contrato);
        return ResponseEntity.ok(novoContrato);
    }

    @GetMapping
    public ResponseEntity<List<Contrato>> listar() {
        return ResponseEntity.ok(contratoService.listarContratos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contrato> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(contratoService.buscarPorId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        contratoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/iniciar")
    public ResponseEntity<Contrato> iniciarContrato(@PathVariable Long id) {
        Contrato contratoIniciado = contratoService.iniciarContrato(id);
        return ResponseEntity.ok(contratoIniciado);
    }
}
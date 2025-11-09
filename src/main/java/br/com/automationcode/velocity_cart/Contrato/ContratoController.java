package br.com.automationcode.velocity_cart.Contrato;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/contratos")
public class ContratoController {

    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @PostMapping("/criar")
    public ResponseEntity<Contrato> criar(@RequestBody Contrato contrato) {
        Contrato novoContrato = contratoService.salvarContrato(contrato);
        return ResponseEntity.ok(novoContrato);
    }

    @GetMapping
    public ResponseEntity<List<Contrato>> listar() {
        return ResponseEntity.ok(contratoService.listarContratos());
    }

    @GetMapping("/finalizados")
    public ResponseEntity<List<Contrato>> listarfinalizados() {
        return ResponseEntity.ok(contratoService.listarContratosFinalizados());
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
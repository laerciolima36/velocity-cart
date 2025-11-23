package br.com.automationcode.velocity_cart.Aluguel;

import java.util.List;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("api/aluguel")
public class AluguelController {

    @Autowired
    private InformacoesService informacoesService;

    private final AluguelService aluguelService;

    public AluguelController(AluguelService aluguelService) {
        this.aluguelService = aluguelService;
    }

    @PostMapping("/criar")
    public Aluguel criar(@RequestBody Aluguel aluguel) {
        return aluguelService.criarAluguel(aluguel);
    }

    @PostMapping("/{id}/pausar")
    public void pausar(@PathVariable Long id) {
        aluguelService.pausarAluguel(id);
    }

    @PostMapping("/{id}/retomar")
    public void retomar(@PathVariable Long id) {
        aluguelService.retomarAluguel(id);
    }

    @PostMapping("/{id}/finalizar")
    public void finalizar(@PathVariable Long id) {
        aluguelService.finalizarAluguel(id);
    }

    @GetMapping("/stream")
    public SseEmitter streamAlugueis() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                while (true) {
                    List<Aluguel> alugueis = aluguelService.getTodosAlugueis();
                    emitter.send(alugueis);
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @GetMapping("/informacoes")
    public List<InfoDTO> getInformacoes() {
        return informacoesService.getInfo();
    }

    /**
     * Retorna todos os aluguéis finalizados com flagView = true
     */
    @GetMapping("/finalizados")
    public ResponseEntity<List<Aluguel>> getAlugueisFinalizadosByFlag() {
        List<Aluguel> alugueis = aluguelService.getAlugueisFinalizadosByFlag();

        if (alugueis.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(alugueis);
    }

    /**
     * Atualiza o flagView de um aluguel específico para false
     */
    @PutMapping("/finalizado/{id}")
    public ResponseEntity<String> atualizarFlagView(@PathVariable("id") Long aluguelId) {
        try {
            aluguelService.setFlagView(aluguelId);
            return ResponseEntity.ok("FlagView do aluguel " + aluguelId + " atualizada com sucesso.");
        } catch (Exception e) {
            // log.error("Erro ao atualizar flagView do aluguel {}: {}", aluguelId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Erro ao atualizar FlagView do aluguel.");
        }
    }

    @PostMapping("/audiofinal/{id}")
    public ResponseEntity<String> reproduzirAudioFinal(@PathVariable("id") Long aluguelId) {
        try {
            aluguelService.reproduzirAudioFinal(aluguelId);
            return ResponseEntity.ok("Audio Reproduzido " + aluguelId + ".");
        } catch (Exception e) {
            // log.error("Erro ao atualizar flagView do aluguel {}: {}", aluguelId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Erro ao reproduzir o audio do aluguel.");
        }
    }
}
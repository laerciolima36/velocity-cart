package br.com.automationcode.velocity_cart.Aluguel;

import java.util.List;
import java.util.concurrent.Executors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("api/aluguel")
public class AluguelController {

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
}

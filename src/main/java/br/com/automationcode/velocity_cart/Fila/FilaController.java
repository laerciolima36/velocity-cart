package br.com.automationcode.velocity_cart.Fila;

import java.lang.ProcessHandle.Info;
import java.util.List;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.com.automationcode.velocity_cart.Aluguel.InformacoesService;

@RestController
@RequestMapping("/api/fila")
public class FilaController {

    @Autowired
    private FilaService filaService;

    @Autowired
    InformacoesService informacoesService;

    @GetMapping
    public List<Fila> listarFilas() {
        return filaService.getTodosFilas();
    }

    //caso o cliente deseje cancelar a reserva e sair da fila
    @DeleteMapping("/{id}")
    public void deletarFila(@PathVariable Long id) {
        filaService.deleteById(id);
    }

    @GetMapping("/stream")
    public SseEmitter streamAlugueis() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                while (true) {
                    List<Fila> filaDetails = informacoesService.getFilaDetails();
                    emitter.send(filaDetails);
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
package br.com.automationcode.velocity_cart.Fila;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fila")
public class FilaController {

    @Autowired
    private FilaService filaService;

    @GetMapping
    public List<Fila> listarFilas() {
        return filaService.getTodosFilas();
    }

    //caso o cliente deseje cancelar a reserva e sair da fila
    @DeleteMapping("/{id}")
    public void deletarFila(@PathVariable Long id) {
        filaService.deleteById(id);
    }
}
package br.com.automationcode.velocity_cart.Audio;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vozes")
public class VozController {

    @Autowired
    private VozService vozesService;

    @GetMapping
    public List<Voz> getAllVozes() {
        return vozesService.getAllVozes();
    }

    @PostMapping("/ativar/{id}")
    public Voz setVozAtiva(@PathVariable Long id) {
        return vozesService.setVozAtiva(id);
    }
}
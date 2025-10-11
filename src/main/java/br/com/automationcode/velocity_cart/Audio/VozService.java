package br.com.automationcode.velocity_cart.Audio;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class VozService {

    @Autowired
    VozRepository vozesRepository;

    public List<Voz> getAllVozes() {
        return vozesRepository.findAll();
    }

    @Transactional
    public Voz setVozAtiva(Long id) {
        List<Voz> todasVozes = vozesRepository.findAll();
        for (Voz voz : todasVozes) {
            if (voz.getId().equals(id)) {
                voz.setAtiva(true);
            } else {
                voz.setAtiva(false);
            }
            vozesRepository.save(voz);
        }
        return vozesRepository.findByAtivaTrue();
    }

    public String getVozAtiva() {
        Voz vozAtiva = vozesRepository.findByAtivaTrue();
        return vozAtiva != null ? vozAtiva.getVoz() : "pt-BR-Standard-C"; // Voz padrão se nenhuma estiver ativa
    }

}
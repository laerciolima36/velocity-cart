package br.com.automationcode.velocity_cart.Audio;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private final TextToSpeechService ttsService;

    public TtsController(TextToSpeechService ttsService) {
        this.ttsService = ttsService;
    }

    @GetMapping("/speak")
    public String speak(@RequestParam String text) {
        try {
            ttsService.speak(text);
            return "Falando: " + text;
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro: " + e.getMessage();
        }
    }
}
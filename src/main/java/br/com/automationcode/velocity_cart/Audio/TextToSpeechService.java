package br.com.automationcode.velocity_cart.Audio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.springframework.stereotype.Service;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;

@Service
public class TextToSpeechService {

    public void speak(String text) throws Exception {
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {

            // Texto a converter
            SynthesisInput input = SynthesisInput.newBuilder()
                    .setText(text)
                    .build();

            // Configuração da voz
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("pt-BR")
                    // .setSsmlGender(SsmlVoiceGender.NEUTRAL)
                    .setName("pt-BR-Standard-C") // você pode trocar por outras vozes
                    .build();

            // Configuração do áudio
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.LINEAR16) // WAV (para reprodução direta)
                    .build();

            // Requisição
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

            // Pegar bytes de áudio
            ByteString audioContents = response.getAudioContent();

            // Salvar temporariamente
            File tempFile = File.createTempFile("tts-", ".wav");
            try (OutputStream out = new FileOutputStream(tempFile)) {
                out.write(audioContents.toByteArray());
            }

            // Reproduzir no servidor
            playAudio(tempFile);

            tempFile.deleteOnExit();
        }
    }

    private void playAudio(File file) throws Exception {
        try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(file)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
            Thread.sleep(clip.getMicrosecondLength() / 1000);
            clip.close();
        }
    }
}
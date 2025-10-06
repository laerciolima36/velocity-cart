// package br.com.automationcode.velocity_cart.Audio;

// import java.io.IOException;

// import javax.sound.sampled.AudioFormat;
// import javax.sound.sampled.AudioInputStream;
// import javax.sound.sampled.AudioSystem;
// import javax.sound.sampled.DataLine;
// import javax.sound.sampled.LineUnavailableException;
// import javax.sound.sampled.SourceDataLine;

// import org.springframework.stereotype.Service;

// import marytts.MaryInterface;
// import marytts.exceptions.SynthesisException;

// @Service
// public class TextToSpeechService {

//     private final MaryInterface maryInterface;

//     // O Spring injeta o MaryInterface configurado no MaryTTSConfig
//     public TextToSpeechService(MaryInterface maryInterface) {
//         this.maryInterface = maryInterface;
//     }

//     /**
//      * Sintetiza o texto para fala e retorna um array de bytes no formato WAV.
//      * * @param text O texto a ser convertido para fala.
//      * @return Array de bytes representando o arquivo de áudio WAV.
//      * @throws IOException Se houver erro de I/O durante a conversão do áudio.
//      * @throws SynthesisException Se o motor MaryTTS falhar ao sintetizar.
//      */
//     public void synthesizeAndPlay(String text) 
//            throws SynthesisException, IOException, LineUnavailableException {
           
//         System.out.println("Service: Iniciando síntese e reprodução interna para o texto: " + text);
        
//         // 1. Geração do AudioInputStream pelo MaryTTS
//         AudioInputStream audio = maryInterface.generateAudio(text);

//         // 2. Criação dos objetos de reprodução
//         AudioFormat format = audio.getFormat();
//         DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

//         // Verifica se a linha de áudio é suportada
//         if (!AudioSystem.isLineSupported(info)) {
//             System.err.println("A linha de áudio não é suportada no seu sistema.");
//             audio.close();
//             throw new LineUnavailableException("Linha de áudio não suportada.");
//         }

//         // 3. Abrindo a linha de áudio e preparando o buffer
//         SourceDataLine line = null;
//         try {
//             line = (SourceDataLine) AudioSystem.getLine(info);
//             line.open(format);
//             line.start();

//             int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
//             byte[] buffer = new byte[bufferSize];
//             int bytesRead;

//             // 4. Lendo do AudioInputStream e escrevendo para a linha de áudio
//             while ((bytesRead = audio.read(buffer, 0, buffer.length)) != -1) {
//                 if (bytesRead > 0) {
//                     line.write(buffer, 0, bytesRead);
//                 }
//             }

//             // 5. Finalizando a reprodução e liberando recursos
//             line.drain(); // Espera a linha terminar de reproduzir tudo
//             System.out.println("Service: Reprodução interna concluída.");

//         } finally {
//             if (line != null) {
//                 line.stop();
//                 line.close();
//             }
//             if (audio != null) {
//                 audio.close();
//             }
//         }
//     }
// }
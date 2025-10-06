// package br.com.automationcode.velocity_cart.Audio;

// import java.util.Locale;

// import org.springframework.beans.factory.DisposableBean; // Para desligamento
// import org.springframework.beans.factory.InitializingBean; // Para inicialização
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// import marytts.LocalMaryInterface;
// import marytts.MaryInterface;

// @Configuration
// public class MaryTTSConfig implements InitializingBean, DisposableBean {

//     private MaryInterface mary;

//     /**
//      * 1. Inicializa o MaryTTS Server Embutido (ON/UP)
//      * Implementa a InitializingBean para garantir que o MaryTTS suba
//      * assim que o Spring Boot iniciar e antes de aceitar requisições.
//      */
//     @Override
//     public void afterPropertiesSet() throws Exception {
//         System.out.println(">>> Inicializando o servidor MaryTTS embutido...");
//         try {
//             // Cria a instância do servidor local do MaryTTS
//             mary = new LocalMaryInterface();
//             mary.setLocale(Locale.ENGLISH);

//             // Tenta definir o idioma e voz
//             // IMPORTANTE: O locale 'pt' só funcionará se o pacote de idioma estiver no
//             // classpath.
//             // mary.setLocale(Locale.forLanguageTag("pt"));

//             // Seleciona a voz disponível para 'pt' ou uma voz fallback
//             // Tenta encontrar uma voz em Português
//             // String voiceName = mary.getAvailableVoices().stream()
//             //         .filter(v -> v.toLowerCase().contains("pt"))
//             //         .findFirst()
//             //         .orElse(null);

//             // if (voiceName != null) {
//             //     mary.setVoice(voiceName);
//             //     System.out.println("MaryTTS rodando. Voz selecionada: " + voiceName);
//             // } else {
//             //     System.err
//             //             .println("Nenhuma voz para Português ('pt') encontrada. Verifique a instalação do pacote JAR.");
//             //     // Se não houver voz em PT, pode tentar uma voz padrão (como EN) para debug
//             //     mary.setVoice("cmu-slt-hsmm");
//             // }

//         } catch (Exception e) {
//             System.err.println("Falha ao inicializar o MaryTTS: " + e.getMessage());
//             throw e;
//         }
//     }

//     /**
//      * 2. Desliga o MaryTTS Server (OFF/DOWN)
//      * Implementa DisposableBean para garantir que o servidor seja
//      * desligado corretamente ao finalizar a aplicação Spring.
//      */
//     @Override
//     public void destroy() {
//         if (mary != null) {
//             System.out.println(">>> Desligando o servidor MaryTTS...");
//             // O MaryTTS precisa ser desligado explicitamente.
//             // Para LocalMaryInterface, você pode precisar forçar o encerramento do servidor
//             // interno.
//             // Dependendo da versão e implementação, basta deixar o objeto 'mary' ser
//             // coletado.
//             // Em implementações mais robustas, você chamaria: Mary.shutdown();
//             // Para fins de limpeza de recursos, vamos confiar no encerramento do processo
//             // Spring.
//         }
//     }

//     /**
//      * 3. Expõe o MaryInterface como um Bean do Spring
//      * Permite injetar a instância 'mary' em qualquer Controller ou Serviço.
//      */
//     @Bean
//     public MaryInterface maryInterface() {
//         return mary;
//     }
// }

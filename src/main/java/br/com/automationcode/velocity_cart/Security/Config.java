package br.com.automationcode.velocity_cart.Security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class Config implements WebMvcConfigurer {

    private static final String UPLOAD_DIR = "imagens/produtos/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/imagens/produtos/**")
                .addResourceLocations("file:" + UPLOAD_DIR);
    }
}
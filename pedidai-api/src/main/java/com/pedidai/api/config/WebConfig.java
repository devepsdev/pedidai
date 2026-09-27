package com.pedidai.api.config;

import com.pedidai.api.services.impl.ProductServiceImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Imatges de producte, a /api/img/productes/ perquè nginx ja envia /api cap a l'API. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get(ProductServiceImpl.IMAGE_DIR).toAbsolutePath().toUri().toString();
        registry.addResourceHandler(ProductServiceImpl.IMAGE_URL_PREFIX + "**")
                .addResourceLocations(uploadPath);
    }
}

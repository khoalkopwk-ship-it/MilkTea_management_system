package vn.edu.ute.milktea.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("uploads/reviews").toAbsolutePath();
        String uploadUri = uploadDir.toUri().toString();
        registry.addResourceHandler("/images/reviews/**")
                .addResourceLocations(uploadUri, "classpath:/static/images/reviews/");
    }
}

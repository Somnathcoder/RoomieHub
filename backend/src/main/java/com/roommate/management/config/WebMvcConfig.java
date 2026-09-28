package com.roommate.management.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Exposes uploaded files (profile photos, receipts, etc.) as static resources
 * under /uploads/**. ID proof documents are served through a dedicated,
 * authorization-checked controller endpoint instead (see MemberController),
 * not through this generic static mapping.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = new File(fileStorageProperties.getBaseDir()).getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + path + File.separator);
    }
}

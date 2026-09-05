package topic_management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Web MVC Configuration to register central AuthInterceptor.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/student/**",
                        "/lecturer/**",
                        "/department-head/**",
                        "/division-head/**",
                        "/admin/**",
                        "/api/v2/**"
                )
                .excludePathPatterns(
                        "/",
                        "/login",
                        "/logout",
                        "/auth/**",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/api-docs/**",
                        "/api/topics/**"
                );
    }
}

package mg.eni.tulear.ramassage.config;

import mg.eni.tulear.ramassage.securite.JournalAuditInterceptor;
import mg.eni.tulear.ramassage.service.JournalAuditService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Branche l'intercepteur du journal d'audit sur l'API. */
@Configuration
public class AuditWebConfig implements WebMvcConfigurer {

    private final ObjectProvider<JournalAuditService> journal;

    public AuditWebConfig(ObjectProvider<JournalAuditService> journal) {
        this.journal = journal;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JournalAuditInterceptor(journal)).addPathPatterns("/api/**");
    }
}

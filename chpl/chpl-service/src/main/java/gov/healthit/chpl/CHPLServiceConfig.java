package gov.healthit.chpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.castor.CastorMarshaller;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import gov.healthit.chpl.job.MeaningfulUseUploadJob;

@Configuration
@EnableWebMvc
@EnableTransactionManagement(proxyTargetClass = true)
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
@EnableAsync
@EnableAspectJAutoProxy
@EnableScheduling
@EnableCaching
@PropertySource("classpath:/environment.properties")
@ComponentScan(basePackages = {
        "gov.healthit.chpl.**"
})
public class CHPLServiceConfig extends WebMvcConfigurerAdapter implements EnvironmentAware {

    private static final Logger LOGGER = LogManager.getLogger(CHPLServiceConfig.class);

    @Autowired
    private Environment env;

    @Override
    public void setEnvironment(final Environment environment) {
        this.env = environment;
    }

    @Bean
    public MappingJackson2HttpMessageConverter jsonConverter() {
        MappingJackson2HttpMessageConverter bean = new MappingJackson2HttpMessageConverter();
        bean.setPrefixJson(false);
        List<MediaType> mediaTypes = new ArrayList<MediaType>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        bean.setSupportedMediaTypes(mediaTypes);
        return bean;
    }

    @Bean
    public org.springframework.orm.jpa.LocalEntityManagerFactoryBean entityManagerFactory() {
        LOGGER.info("get LocalEntityManagerFactoryBean");
        org.springframework.orm.jpa.LocalEntityManagerFactoryBean bean =
                new org.springframework.orm.jpa.LocalEntityManagerFactoryBean();
        bean.setPersistenceUnitName(env.getRequiredProperty("persistenceUnitName"));
        return bean;
    }

    @Bean
    public org.springframework.orm.jpa.JpaTransactionManager transactionManager() {
        LOGGER.info("get JpaTransactionManager");
        org.springframework.orm.jpa.JpaTransactionManager bean =
                new org.springframework.orm.jpa.JpaTransactionManager();
        bean.setEntityManagerFactory(entityManagerFactory().getObject());
        return bean;
    }

    @Bean
    public org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor persistenceAnnotationBeanPostProcessor() {
        LOGGER.info("get PersistenceAnnotationBeanPostProcessor");
        return new org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor();
    }

    @Bean(name = "multipartResolver")
    public CommonsMultipartResolver getResolver() throws IOException {
        LOGGER.info("get CommonsMultipartResolver");
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();

        // Set the maximum allowed size (in bytes) for each individual file.
        resolver.setMaxUploadSize(5242880);// 5MB

        // You may also set other available properties.

        return resolver;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        te.setCorePoolSize(10);
        te.setMaxPoolSize(100);
        return te;
    }

    @Bean
    public Marshaller marshaller() {
        LOGGER.info("get Marshaller");
        return new CastorMarshaller();
    }

    @Bean
    public CacheManager cacheManager() {
        LOGGER.info("get CacheManager");
        return new EhCacheCacheManager(ehCacheCacheManager().getObject());
    }

    @Bean
    public EhCacheManagerFactoryBean ehCacheCacheManager() {
        LOGGER.info("get EhCacheManagerFactoryBean");
        EhCacheManagerFactoryBean cmfb = new EhCacheManagerFactoryBean();
        cmfb.setConfigLocation(new ClassPathResource("ehCache.xml"));
        cmfb.setShared(true);
        return cmfb;
    }

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:/errors");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public CookieLocaleResolver localeResolver() {
        CookieLocaleResolver localeResolver = new CookieLocaleResolver();
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        localeResolver.setCookieName("my-locale-cookie");
        localeResolver.setCookieMaxAge(3600);
        return localeResolver;
    }

    @Bean
    public LocaleChangeInterceptor localeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(localeInterceptor());
    }

    @Bean
    public MeaningfulUseUploadJob meaningfulUseUploadJob() {
        return new MeaningfulUseUploadJob();
    }

    /**
     * Get a task executor.
     * @return TaskExecutor object
     */
    @Bean(name = "jobAsyncDataExecutor")
    public TaskExecutor specificTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        //executor.setCorePoolSize(Integer.parseInt(props.getProperty("corePoolSize")));
        //executor.setMaxPoolSize(Integer.parseInt(props.getProperty("maxPoolSize")));
        //executor.setQueueCapacity(11);
        executor.setThreadNamePrefix("jobDataThread");
        executor.initialize();
        return executor;
    }
}

package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.net.URL;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public FilterRegistrationBean rewriteFilterConfig() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setName("rewriteFilter");
        registration.setFilter(new UrlRewriteFilter() {
            @Override
            protected void loadUrlRewriter(FilterConfig filterConfig) throws ServletException {
                String confPath = filterConfig.getInitParameter("confPath");
                ServletContext context = filterConfig.getServletContext();
                try {
                    final URL confUrl = getClass().getClassLoader().getResource(confPath);
                    final InputStream config = getClass().getClassLoader().getResourceAsStream(confPath);
                    Conf conf = new Conf(context, config, confPath, confUrl.toString(), false);
                    checkConf(conf);
                } catch (Throwable e) {
                    throw new ServletException(e);
                }
            }
        });
        registration.addInitParameter("confPath", "urlrewrite.xml");
        return registration;
    }
}

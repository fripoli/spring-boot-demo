# spring-boot-demo
This repo was created to be used as an example for an issue with Tuckey URL Rewrite
loading the urlrewrite.xml config file.

The config is loaded correctly if the app is run using the gradle plugin:

``./gradlew bootRun``

The output is:

```
o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
o.e.j.s.h.ContextHandler.application     : org.tuckey.web.filters.urlrewrite.UrlRewriteFilter INFO: loaded (conf ok)
o.e.j.s.h.ContextHandler.application     : Initializing Spring FrameworkServlet 'dispatcherServlet'
```

However, after building spring boot executable jar with:

``./gradlew build``

And running with:

``java -jar build/libs/demo-0.0.1-SNAPSHOT.jar``

The output is:

```
o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
o.e.j.s.h.ContextHandler.application     : org.tuckey.web.filters.urlrewrite.UrlRewriteFilter ERROR: unable to find urlrewrite conf file at urlrewrite.xml
o.e.j.s.h.ContextHandler.application     : Initializing Spring FrameworkServlet 'dispatcherServlet'
```

The error occurs because, on its standalone jar, spring boot keeps all classes under:

```
BOOT-INF/classes/
```

This can be seen doing:

```
$ jar tf build/libs/demo-0.0.1-SNAPSHOT.jar | grep urlrewrite
BOOT-INF/classes/urlrewrite.xml
BOOT-INF/lib/urlrewritefilter-4.0.4.jar
```

The problem is that spring is using another class loader and when the UrlRewriteFilter
tries to [load the conf file with the code:](https://github.com/paultuckey/urlrewritefilter/blob/35697bc72817d5cef048cffa3890b06d113deeba/src/main/java/org/tuckey/web/filters/urlrewrite/UrlRewriteFilter.java#L264)

```java
private void loadUrlRewriterLocal() {
    InputStream inputStream = context.getResourceAsStream(confPath);
    // attempt to retrieve from location other than local WEB-INF
    if ( inputStream == null ) {
        inputStream = ClassLoader.getSystemResourceAsStream(confPath);
    }
    URL confUrl = null;
    try {
        confUrl = context.getResource(confPath);
    } catch (MalformedURLException e) {
        log.debug(e);
    }
}
```

That doesn't work.

To go around this issue, lots of spring boot applications are loading their urlrewrite.xml
files overriding the protected method `loadUrlRewriter` with:

```java
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
```

Which assures that the same class loader is used.

To save lots of peoples time, I suggest the method on the `urlrewrite` library to be updated to:

```java
private void loadUrlRewriterLocal() {
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(confPath);
    // attempt to retrieve from location other than local WEB-INF
    if ( inputStream == null ) {
        inputStream = ClassLoader.getSystemResourceAsStream(confPath);
    }
    URL confUrl = getClass().getClassLoader().getResource(confPath);
```

So whatever class loader is being used for the `urlrewrite` class is also used to load
the config file.
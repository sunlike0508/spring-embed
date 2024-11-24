package hello.boot;

import java.util.List;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;


public class MySpringApplication {

    public static void run(Class configClass, String[] args) {
        System.out.println("MySpringApplication.main args = " + List.of(args));

        Tomcat tomcat = new Tomcat();
        Connector connector = new Connector();
        connector.setPort(8080);
        tomcat.setConnector(connector);

        //스프링 컨테이너 생성
        AnnotationConfigWebApplicationContext appContext = new AnnotationConfigWebApplicationContext();
        appContext.register(configClass);

        //스프링 MVC 디스패처 서블릿 생성, 스프링 컨테이너 연결 : 이렇게 하면 서블릿이 스프링 컨테이너에 있는 controller를 찾아 연결시켜줌
        DispatcherServlet dispatcherServlet = new DispatcherServlet(appContext);

        //디스패처 서블릿 등록
        //위에서 만든 서블릿을 서블릿 컨테이너에 넣는다.
        Context context = tomcat.addContext("", "/");
        tomcat.addServlet("", "dispatcher", dispatcherServlet);
        context.addServletMappingDecoded("/", "dispatcher");

        try {
            tomcat.start();
        } catch(LifecycleException e) {
            throw new RuntimeException(e);
        }
    }
}

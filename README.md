# 스프링 부트와 내장 톰캣

## WAR 배포 방식의 단점

웹 애플리케이션을 개발하고 배포하려면 다음과 같은 과정을 거쳐야 한다. 

* 톰캣 같은 웹 애플리케이션 서버(WAS)를 별도로 설치해야 한다. 
* 애플리케이션 코드를 WAR로 빌드해야 한다.
* 빌드한 WAR 파일을 WAS에 배포해야 한다.

웹 애플리케이션을 구동하고 싶으면 웹 애플리케이션 서버를 별도로 설치해야 하는 구조이다.

과거에는 이렇게 웹 애플리케이션 서버와 웹 애플리케이션 빌드 파일(WAR)이 분리되어 있는것이 당연한 구조였다.

그런데 이런 방식은 다음과 같은 단점이 있다.

### **단점**

* 톰캣 같은 WAS를 별도로 설치해야 한다. 
* 개발 환경 설정이 복잡하다.
  * 단순한 자바라면 별도의 설정을 고민하지 않고, `main()` 메서드만 실행하면 된다.
  * 웹 애플리케이션은 WAS 실행하고 또 WAR와 연동하기 위한 복잡한 설정이 들어간다. 
* 배포 과정이 복잡하다. WAR를 만들고 이것을 또 WAS에 전달해서 배포해야 한다. 
* 톰캣의 버전을 변경하려면 톰캣을 다시 설치해야 한다.

### **고민**

누군가는 오래전부터 이런 방식의 불편함을 고민해왔다. 

단순히 자바의 `main()` 메서드만 실행하면 웹 서버까지 같이 실행되도록 하면 되지 않을까? 

톰캣도 자바로 만들어져 있으니 톰캣을 마치 하나의 라이브러리 처럼 포함해서 사용해도 되지 않을까? 

쉽게 이야기해서 톰캣 같은 웹서버를 라이브러리로 내장해버리는 것이다.

이런 문제를 해결하기 위해 톰캣을 라이브러리로 제공하는 내장 톰캣(embed tomcat) 기능을 제공한다.

### 외장 서버 vs 내장 서버

<img width="699" alt="Screenshot 2024-11-19 at 22 54 26" src="https://github.com/user-attachments/assets/b9be8163-424b-4109-950b-c7d524eff4a4">

왼쪽 그림은 웹 애플리케이션 서버에 WAR 파일을 배포하는 방식, WAS를 실행해서 동작한다.

오른쪽 그림은 애플리케이션 JAR 안에 다양한 라이브러리들과 WAS 라이브러리가 포함되는 방식, `main()` 메서드를 실행해서 동작한다.

## 내장 톰캣1 - 설정

```groovy
dependencies {
    implementation 'org.apache.tomcat.embed:tomcat-embed-core:10.1.5'
}

//일반 Jar 생성
task buildJar(type: Jar) {
  manifest {
    attributes 'Main-Class': 'hello.embed.EmbedTomcatSpringMain'
  }
  with jar
}

//Fat Jar 생성
task buildFatJar(type: Jar) {
  manifest {
    attributes 'Main-Class': 'hello.embed.EmbedTomcatSpringMain'
  }
  duplicatesStrategy = DuplicatesStrategy.WARN
  from { configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) } }
  with jar
}
```

`tomcat-embed-core` : 톰캣 라이브러리이다. 톰캣을 라이브러리로 포함해서 톰캣 서버를 자바 코드로 실행할 수 있다. 서블릿 관련 코드도 포함하고 있다.

`buildJar` , `buildFatJar` 관련된 부분은 뒤에서 다시 설명한다.

## 내장 톰캣2 - 서블릿

이제 본격적으로 내장 톰캣을 사용해보자. 

내장 톰캣은 쉽게 이야기해서 톰캣을 라이브러리로 포함하고 자바 코드로 직접 실행하는 것이다

```java
public class EmbedTomcatServletMain {

    public static void main(String[] args) throws LifecycleException {
        System.out.println("EmbedTomcatServletMain.main");

        Tomcat tomcat = new Tomcat();
        Connector connector = new Connector();
        connector.setPort(8080);
        tomcat.setConnector(connector);

        //서블릿 등록
        Context context = tomcat.addContext("", "/");
        tomcat.addServlet("", "helloServlet", new HelloServlet());
        context.addServletMappingDecoded("/hello-servlet", "helloServlet");
        tomcat.start();
    }
}
```


**실행**

`EmbedTomcatServletMain.main()` 메서드를 실행하자.

http://localhost:8080/hello-servlet

**참고**

내장 톰캣을 개발자가 직접 다룰일은 거의 없다. 

스프링 부트에서 내장 톰캣 관련된 부분을 거의 대부분 자동화해서 제공하기 때문에 내장 톰캣을 깊이있게 학습하는 것은 권장하지 않는다.(백엔드 개발자는 이미 공부해야 할 것이 너무 많다.)

내장 톰캣이 어떤 방식으로 동작하는지 그 원리를 대략 이해하는 정도면 충분하다.

## 내장 톰캣3 - 스프링

```java
public class EmbedTomcatSpringMain {

  public static void main(String[] args) throws LifecycleException {
    System.out.println("EmbedTomcatSpringMain.main");

    Tomcat tomcat = new Tomcat();
    Connector connector = new Connector();
    connector.setPort(8080);
    tomcat.setConnector(connector);

    //스프링 컨테이너 생성
    AnnotationConfigWebApplicationContext appContext = new AnnotationConfigWebApplicationContext();
    appContext.register(HelloConfig.class);

    //스프링 MVC 디스패처 서블릿 생성, 스프링 컨테이너 연결 : 이렇게 하면 서블릿이 스프링 컨테이너에 있는 controller를 찾아 연결시켜줌
    DispatcherServlet dispatcherServlet = new DispatcherServlet(appContext);

    //디스패처 서블릿 등록
    //위에서 만든 서블릿을 서블릿 컨테이너에 넣는다.
    Context context = tomcat.addContext("", "/");
    tomcat.addServlet("", "dispatcher", dispatcherServlet);
    context.addServletMappingDecoded("/", "dispatcher");

    tomcat.start();
  }
}
```

* `main()` 메서드를 실행하면 다음과 같이 동작한다.
* 내장 톰캣을 생성해서 `8080` 포트로 연결하도록 설정한다.
* 스프링 컨테이너를 만들고 필요한 빈을 등록한다.
* 스프링 MVC 디스패처 서블릿을 만들고 앞서 만든 스프링 컨테이너에 연결한다. 디스패처 서블릿을 내장 톰캣에 등록한다.
* 내장 톰캣을 실행한다.

**실행**

`EmbedTomcatSpringMain.main()` 메서드를 실행하자.

http://localhost:8080/hello-spring

코드를 보면 알겠지만, 서블릿 컨테이너 초기화와 거의 같은 코드이다.

다만 시작점이 개발자가 `main()` 메서드를 직접 실행하는가, 서블릿 컨테이너가 제공하는 초기화 메서드를 통해서 실행하는가의 차이가 있을 뿐이다.

## 내장 톰캣4 - 빌드와 배포1

이번에는 애플리케이션에 내장 톰캣을 라이브러리로 포함했다. 이 코드를 어떻게 빌드하고 배포하는지 알아보자.

자바의 `main()` 메서드를 실행하기 위해서는 `jar` 형식으로 빌드해야 한다.

그리고 `jar` 안에는 `META-INF/MANIFEST.MF` 파일에 실행할 `main()` 메서드의 클래스를 지정해주어야 한다.

`META-INF/MANIFEST.MF` 

```
Manifest-Version: 1.0
Main-Class: hello.embed.EmbedTomcatSpringMain
```
Gradle의 도움을 받으면 이 과정을 쉽게 진행할 수 있다. 다음 코드를 참고하자. 

`build.gradle - buildJar` 참고

```groovy
task buildJar(type: Jar) {
  manifest {
    attributes 'Main-Class': 'hello.embed.EmbedTomcatSpringMain'
  }
  with jar
}
```

해당 코드는 `embed-start` 에서 포함해두었다.

다음과 같이 실행하자. 

**jar 빌드**

```shell
./gradlew clean buildJar
```

다음 위치에 `jar` 파일이 만들어졌을 것이다. `build/libs/embed-0.0.1-SNAPSHOT.jar`

**jar 파일 실행**

`jar` 파일이 있는 폴더로 이동한 후에 다음 명령어로 `jar` 파일을 실행해보자. 

```shell
java -jar embed-0.0.1-SNAPSHOT.jar
```

**실행 결과**

```shell
... % java -jar embed-0.0.1-SNAPSHOT.jar
Error: Unable to initialize main class hello.embed.EmbedTomcatSpringMain
Caused by: java.lang.NoClassDefFoundError: org/springframework/web/context/WebApplicationContext
```
실행 결과를 보면 기대했던 내장 톰캣 서버가 실행되는 것이 아니라, 오류가 발생하는 것을 확인할 수 있다. 

오류 메시지를 잘 읽어보면 스프링 관련 클래스를 찾을 수 없다는 오류이다.

무엇이 문제일까?

문제를 확인하기 위해 `jar` 파일의 압축을 풀어보자. 

**jar 압축 풀기**

우리가 빌드한 jar 파일의 압축을 풀어서 내용물을 확인해보자. 

`build/libs` 폴더로 이동하자.

다음 명령어를 사용해서 압축을 풀자

```shell
jar -xvf embed-0.0.1-SNAPSHOT.jar
```

<img width="393" alt="Screenshot 2024-11-21 at 23 28 09" src="https://github.com/user-attachments/assets/6f79d4c1-af7a-47c7-884d-5c6250bf46a8">

JAR를 푼 결과를 보면 스프링 라이브러리나 내장 톰캣 라이브러리가 전혀 보이지 않는다. 

따라서 해당 오류가 발생한 것이다.

과거에 WAR 파일을 풀어본 기억을 떠올려보자.

**WAR를 푼 결과**
* `WEB-INF`
  * `classes` 
    * `hello/servlet/TestServlet.class`
  * `lib`
    * `jakarta.servlet-api-6.0.0.jar`
* `index.html`

WAR는 분명 내부에 라이브러리 역할을 하는 `jar` 파일을 포함하고 있었다.

**jar 파일은 jar파일을 포함할 수 없다.**

WAR와 다르게 JAR 파일은 내부에 라이브러리 역할을 하는 JAR 파일을 포함할 수 없다. 

포함한다고 해도 인식이 안된다. 이것이 JAR 파일 스펙의 한계이다. 

그렇다고 WAR를 사용할 수 도 없다. 

WAR는 웹 애플리케이션 서버(WAS) 위에서만 실행할 수 있다.

대안으로는 라이브러리 jar 파일을 모두 구해서 MANIFEST 파일에 해당 경로를 적어주면 인식이 되지만 매우 번거롭고, Jar 파일안에 Jar 파일을 포함할 수 없기 때문에 라이브러리 역할을 하는 jar 파일도 항상 함께 가지고 다녀야 한다. 

이 방법은 권장하기 않기 때문에 따로 설명하지 않는다.

## 내장 톰캣5 - 빌드와 배포2

FatJar

대안으로는 `fat jar` 또는 `uber jar` 라고 불리는 방법이다.

Jar 안에는 Jar를 포함할 수 없다. 하지만 클래스는 얼마든지 포함할 수 있다.

라이브러리에 사용되는 `jar` 를 풀면 `class` 들이 나온다. 

이 `class` 를 뽑아서 새로 만드는 `jar` 에 포함하는 것이다. 

이렇게 하면 수 많은 라이브러리에서 나오는 `class` 때문에 뚱뚱한(fat) `jar` 가 탄생한다. 

그래서 `Fat Jar` 라고 부르는 것이다.

```groovy
//Fat Jar 생성
task buildFatJar(type: Jar) {
    manifest {
        attributes 'Main-Class': 'hello.embed.EmbedTomcatSpringMain'
    }
    duplicatesStrategy = DuplicatesStrategy.WARN
    from { configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) } }
    with jar
}
```

**jar 빌드**

```shell
./gradlew clean buildFatJar
```

빌드시 `Encountered duplicate path` 경고가 나올 수 있는데 이 부분은 무시하자

다음 위치에 `jar` 파일이 만들어졌을 것이다. 

`build/libs/embed-0.0.1-SNAPSHOT.jar`

용량을 확인해보면 `10M` 이상의 상당히 큰 사이즈가 나왔다.

**jar 파일 실행**

`jar` 파일이 있는 폴더로 이동한 후에 다음 명령어로 `jar` 파일을 실행해보자. 

```shell
java -jar embed-0.0.1-SNAPSHOT.jar
```

```shell
➜  libs git:(main) ✗ java -jar spring-embed-0.0.1-SNAPSHOT.jar
EmbedTomcatSpringMain.main
Nov 23, 2024 11:12:58 PM org.apache.coyote.AbstractProtocol init
INFO: Initializing ProtocolHandler ["http-nio-8080"]
Nov 23, 2024 11:12:58 PM org.apache.catalina.core.StandardService startInternal
INFO: Starting service [Tomcat]
Nov 23, 2024 11:12:58 PM org.apache.catalina.core.StandardEngine startInternal
INFO: Starting Servlet engine: [Apache Tomcat/10.1.5]
Nov 23, 2024 11:12:58 PM org.apache.coyote.AbstractProtocol start
INFO: Starting ProtocolHandler ["http-nio-8080"]
```

http://localhost:8080/hello-spring

드디어 정상 동작하는 것을 확인할 수 있다.

Fat Jar의 정체를 확인하기 위해 `jar` 파일의 압축을 풀어보자. 

**jar 압축 풀기**

우리가 빌드한 jar 파일의 압축을 풀어서 내용물을 확인해보자. `build/libs` 폴더로 이동하자.

다음 명령어를 사용해서 압축을 풀자

```shell
jar -xvf embed-0.0.1-SNAPSHOT.jar
```

Jar를 풀어보면 우리가 만든 클래스를 포함해서, 수 많은 라이브러리에서 제공되는 클래스들이 포함되어 있는 것을 확인할 수 있다.


### Fat Jar 정리 

**Fat Jar의 장점**

Fat Jar 덕분에 하나의 jar 파일에 필요한 라이브러리들을 내장할 수 있게 되었다.

내장 톰캣 라이브러리를 jar 내부에 내장할 수 있게 되었다.

덕분에 하나의 jar 파일로 배포부터, 웹 서버 설치+실행까지 모든 것을 단순화 할 수 있다.

이전에 살펴보았던 WAR를 외부 서버에 배포하는 방식의 단점을 다시 확인해보자.

### **WAR 단점과 해결**

* 톰캣 같은 WAS를 별도로 설치해야 한다.
  * 해결: WAS를 별도로 설치하지 않아도 된다. 톰캣 같은 WAS가 라이브러리로 jar 내부에 포함되어 있다.
* 개발 환경 설정이 복잡하다.
  * 단순한 자바라면 별도의 설정을 고민하지 않고, `main()` 메서드만 실행하면 된다.
  * 웹 애플리케이션은 WAS를 연동하기 위한 복잡한 설정이 들어간다.
  * 해결: IDE에 복잡한 WAS 설정이 필요하지 않다. 단순히 `main()` 메서드만 실행하면 된다.
* 배포 과정이 복잡하다. WAR를 만들고 이것을 또 WAS에 전달해서 배포해야 한다.
  * 해결: 배포 과정이 단순하다. JAR를 만들고 이것을 원하는 위치에서 실행만 하면 된다.
* 톰캣의 버전을 업데이트 하려면 톰캣을 다시 설치해야 한다.
  * 해결: gradle에서 내장 톰캣 라이브러리 버전만 변경하고 빌드 후 실행하면 된다.

### **Fat Jar의 단점**

Fat Jar는 완벽해 보이지만 몇가지 단점을 여전히 포함하고 있다.

* 어떤 라이브러리가 포함되어 있는지 확인하기 어렵다.
  * 모두 `class` 로 풀려있으니 어떤 라이브러리가 사용되고 있는지 추적하기 어렵다.
* 파일명 중복을 해결할 수 없다.
  * 클래스나 리소스 명이 같은 경우 하나를 포기해야 한다. 이것은 심각한 문제를 발생한다. 예를 들어서 서블릿 컨테이너 초기화에서 학습한 부분을 떠올려 보자. 
  * `META-INF/services/jakarta.servlet.ServletContainerInitializer` 이 파일이 여러 라이브러리( `jar` )에 있을 수 있다.
  * `A` 라이브러리와 `B` 라이브러리 둘다 해당 파일을 사용해서 서블릿 컨테이너 초기화를 시도한다. 둘다 해당 파일을 `jar` 안에 포함한다.
  * `Fat Jar` 를 만들면 파일명이 같으므로 `A` , `B` 라이브러리가 둘다 가지고 있는 파일 중에 하나의 파일만 선택된다. 결과적으로 나머지 하나는 포함되지 않으므로 정상 동작하지 않는다.

## 편리한 부트 클래스 만들기

지금까지 진행한 내장 톰캣 실행, 스프링 컨테이너 생성, 디스패처 서블릿 등록의 모든 과정을 편리하게 처리해주는 나만의 부트 클래스를 만들어보자. 

부트는 이름 그대로 시작을 편하게 처리해주는 것을 뜻한다.













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









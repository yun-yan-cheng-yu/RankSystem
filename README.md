# RankSystem
一个 Tomcat Web 示例项目。

当前功能很简单：

- 页面上有一个按钮和一个文本框
- 点击按钮后请求服务器接口 `/hello`
- 服务器返回 `Hello world`
- 页面把返回内容显示到文本框中

## 项目结构

```text
src/main/java/com/example/ranksystem/HelloServlet.java
src/main/webapp/index.html
pom.xml
scripts/build-war.sh
```

## 用 Maven 打包

如果本机安装了 Maven：

```bash
mvn clean package
```

生成文件：

```text
target/RankSystem.war
```

把这个文件放到 Tomcat 的 `webapps/` 目录下，然后启动 Tomcat。

访问地址：

```text
http://localhost:8080/RankSystem/
```

接口地址：

```text
http://localhost:8080/RankSystem/hello
```

## 不用 Maven 打包

如果本机没有 Maven，但已经安装 Tomcat 10：

```bash
export CATALINA_HOME=/你的/apache-tomcat-10.1.x路径
./scripts/build-war.sh
```

生成文件：

```text
build/RankSystem.war
```

然后复制到 Tomcat：

```bash
cp build/RankSystem.war "$CATALINA_HOME/webapps/"
"$CATALINA_HOME/bin/startup.sh"
```

访问：

```text
http://localhost:8080/RankSystem/
```

注意：这个项目使用 `jakarta.servlet`，对应 Tomcat 10+。如果你用 Tomcat 9，需要改成 `javax.servlet`。

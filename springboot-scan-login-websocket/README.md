使用 Spring Boot + WebSocket + QrCode.js + Thymeleaf 实现扫码登录功能

**扫码登陆原理**

扫码登录功能主要由**网页端**、**服务器**和**手机端**组成。当我们访问网页端的登录页面时，会向服务器请求获取二维码登录的请求。服务器接收到请求后，会生成一个uuid，并记录下来，然后将该uuid返回网页端。网页端接收到返回结果后，会生成一张二维码图片，其中返回的uuid信息也会融入二维码中。接下来，网页端会不断向服务器发送请求询问该二维码的状态，若使用手机成功扫码，网页端将会收到登录成功和用户信息。

下面是一个简单的Spring Boot项目，用于实现扫码登录功能，使用WebSocket来处理客户端与服务器之间的通信。在此示例中，我们将使用QrCode.js在前端生成二维码，并在客户端扫码成功后，通过WebSocket发送扫码成功的消息到客户端页面，收到登陆成功消息后，显示登陆成功页面。

**创建Spring Boot项目**

首先，创建一个Spring Boot项目。你可以使用Spring Initializer（https://start.spring.io/）来初始化项目。

**添加依赖**

在`pom.xml`文件中添加WebSocket和Thymeleaf的依赖：

```xaml
<dependencies>
    <!-- Spring Boot WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <!-- Thymeleaf -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>
```

**创建一个WebSocket配置类，用于配置WebSocket端点**

```java
package com.icoderoad.example.scanlogin.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
```

**创建一个WebSocket处理类，用于处理WebSocket连接和消息**

```java
package com.icoderoad.example.scanlogin.conf;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.springframework.stereotype.Component;

@Component
@ServerEndpoint("/ws/login/{uuid}")
public class LoginWebSocket {

	 private Session session;
    // 当前Websocket存储的连接数据：uuid -> websocket数据
    private static final ConcurrentMap<String, LoginWebSocket> WEBSOCKET_MAP = new ConcurrentHashMap<>();


    @OnOpen
    public void onOpen(@PathParam("uuid") String uuid,Session session) {
        // 将新连接的Session加入到sessions中
        this.session = session;
        WEBSOCKET_MAP.put(uuid, this);
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        // 接收到前端发来的消息，生成一个uuid并发送回前端
        String uuid = generateUUID();
        session.getBasicRemote().sendText(uuid);
    }

    @OnClose
    public void onClose(@PathParam("uuid") String uuid, Session session) {
        // 当WebSocket连接关闭时，从sessions中移除对应的Session
    	 WEBSOCKET_MAP.remove(uuid);
    	 try {
			session.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private String generateUUID() {
    	UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
    
    /**
     * 发送消息给客户端
     * @param message
     * @throws IOException
     */
    private void sendMessage(String message){
       
    	try {
			this.session.getBasicRemote().sendText(message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    
    public static void sendMessage(String uuid, String message) {
    	LoginWebSocket qrcodeWebsocket = WEBSOCKET_MAP.get(uuid);
        if (null != qrcodeWebsocket) {
            qrcodeWebsocket.sendMessage(message);
        } 
    }
}
```

**创建Controller**

创建一个Controller类，处理页面请求和WebSocket消息处理。

```java
package com.icoderoad.example.scanlogin.controller;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.icoderoad.example.scanlogin.conf.LoginWebSocket;

@Controller
public class LoginController {

	@GetMapping("/")
    public String loginPage() {
        return "login"; // 返回Thymeleaf模板页面login.html
    }
	
	@GetMapping("/generateUUID")
    @ResponseBody
    public String generateUUID() {
        // 生成一个UUID并返回给前端
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
	
	@GetMapping("/confirmLogin/{uuid}")
    public String confirmLoginPage(@PathVariable("uuid") String uuid, Model model) {
		model.addAttribute("uuid",uuid);
        return "confirm_login"; // 返回确认登录页面confirm_login.html
    }

    @GetMapping("/loginSuccess/{uuid}")
    public String loginSuccessPage(@PathVariable("uuid") String uuid) {
    	LoginWebSocket.sendMessage(uuid,"确认登陆");
        return "login_success"; // 返回登录成功页面login_success.html
    }
}
```

**创建Thymeleaf模板**

在`resources/templates`目录下创建一个名为`loing.html`的Thymeleaf模板文件，用于显示登陆二维码。

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>扫码登录</title>
     <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
	<div class="container mt-5">
        <div class="row">
            <div class="col-md-6 offset-md-3">
                <div class="card">
                    <div class="card-body text-center" style="margin: 1px auto">
						    <div id="qrcode"></div>
 					</div>
                </div>
            </div>
        </div>
    </div>
    <script src="https://cdn.jsdelivr.net/gh/davidshimjs/qrcodejs/qrcode.min.js"></script>
    <script>
        var qr = new QRCode(document.getElementById("qrcode"), {
            text: "",
            width: 128,
            height: 128
        });

        // 页面加载时，发起请求获取UUID
        fetch("/generateUUID")
            .then(response => response.text())
            .then(uuid => {
                // 将获取到的UUID设置为二维码的内容
                var confirmUrl = "http://" + window.location.host + "/confirmLogin/"+ uuid;
                console.log("confirmUrl:", confirmUrl);
                qr.makeCode(confirmUrl);
                
                // 连接WebSocket
                var socket = new WebSocket("ws://" + window.location.host + "/ws/login/" + uuid);

                socket.onopen = function (event) {
                    console.log("WebSocket连接已打开");
                };

                socket.onmessage = function (event) {
                    var message = event.data;
                    if (message === "确认登陆") {
                    	window.location.href = "/loginSuccess/" + uuid;
                    }
                };

                socket.onclose = function (event) {
                    console.log("WebSocket连接已关闭");
                };
            })
            .catch(error => console.error("获取UUID时出错:", error));

       
    </script>
</body>
</html>
```

在`resources/templates`目录下创建一个名为confirm_login.html的Thymeleaf模板文件，用于显示登陆确认页面。

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>确认登录</title>
    <!-- 引入Bootstrap CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
    <div class="container mt-5">
        <div class="row">
            <div class="col-md-6 offset-md-3">
                <div class="card">
                    <div class="card-body text-center">
                        <h1>请确认登录</h1>
                        <form th:action="@{'/loginSuccess/'+${uuid}}">
                        <button id="confirmButton" class="btn btn-primary mt-3">确认登录</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>

</body>
</html>
```

在`resources/templates`目录下创建一个名为 login_success.html 的Thymeleaf模板文件，用于显示登陆成功页面。

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>登录成功</title>
    <!-- 引入Bootstrap CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
</head>
<body>
    <div class="container mt-5">
        <div class="row">
            <div class="col-md-6 offset-md-3">
                <div class="card">
                    <div class="card-body text-center">
                        <h1>登录成功</h1>
                        <a href="/" class="btn btn-primary mt-3">返回首页</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
```

**运行项目**

现在运行项目，访问首页`http://ip:8080/`（ip需使用同一网下可被其他设置访问的ip地址，而非localhost），生成用于登录二维码图片；`http://ip:8080/`来查看扫码登录页面。页面会显示二维码，用户可以扫描该二维码进行登录。扫码成功后，会显示"确认登录"消息，在手机端点击确认登陆，网页页会转到“登录成功”页。
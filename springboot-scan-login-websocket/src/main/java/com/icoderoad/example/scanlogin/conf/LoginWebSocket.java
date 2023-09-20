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
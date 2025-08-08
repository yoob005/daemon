package com.auto.daemon.wsk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class UpbitWebSocketListener extends WebSocketListener{
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private MessageHandler messageHandler = new MessageHandler();
	
    @Override
    public void onMessage(WebSocket webSocket, String text) {
    	try {
    		
			messageHandler.strMsgHandler(text);
			
		} catch (Exception e) {
			logger.error("onMessge String Error : {}", e);
		}
    }
    
    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
    	try {
			
    		messageHandler.byteStrMsgHandler(bytes);
    		
		} catch (Exception e) {
			logger.error("onMessge ByteString Error : {}", e);
		}	
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
    	logger.info("********************* WebSocket 연결 성공 *********************");
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    	logger.error("############ WebSocket 연결 실패: " + t.getMessage());
    }
}

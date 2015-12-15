package jiang.linz;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.List;

public class LinzDispatcher {

	LinzOutMetaData outMetaData;
	Object handler;
	
	public LinzDispatcher(Object handler) {
		this.handler = handler;
		Class<?> handlerClass = handler.getClass();
		this.outMetaData = new LinzOutMetaData(handlerClass);
	}
	
	public void actionPerformed(Socket socket) {
		
		InputStream inputStream;
		try {
			inputStream = socket.getInputStream();
		} catch (IOException e1) {
			System.out.println("error when getInputStream: " + e1.getMessage());
			return;
		}
		LinzRequest request = new LinzRequest(inputStream);
		invokeHandlerAction(request);
		invokeHandlerStates();
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			System.out.println("error when inform state: " + e.getMessage());
		}
		
		OutputStream output;
		try {
			output = socket.getOutputStream();
		} catch (IOException e2) {
			System.out.println("error when getInputStream: " + e2.getMessage());
			return;
		}
		LinzResponse response = new LinzResponse(output, request, outMetaData);
		Thread responseProcess = new Thread(response);
		responseProcess.start();
	}
	

	private void invokeHandlerAction(LinzRequest request) {
		String name = request.getAction();
		LinzOutAction action = outMetaData.getAction(name);
		if (action == null)
			return;
		
		Method method = action.getMethod();
		
		try {		
			if (!request.hasParameters()) {
				method.invoke(handler);
			}
			else {
				method.invoke(handler, request.getParameters());
			}
		} catch (IllegalAccessException e) {
			System.out.println("IllegalAccessException: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			System.out.println("IllegalArgumentException: " + e.getMessage());
		} catch (InvocationTargetException e) {
			System.out.println("InvocationTargetException: " + e.getMessage());
		}
	}
	
	private void invokeHandlerStates() {
		List<LinzOutState> outStates = outMetaData.getAllStates();
		for (LinzOutState state : outStates) {
			state.setState(handler);
		}
	}

}

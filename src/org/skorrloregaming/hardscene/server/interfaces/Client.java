package org.skorrloregaming.hardscene.server.interfaces;

import java.io.IOException;
import java.net.Socket;

public class Client {

	public Socket socket = null;
	public String address = "0.0.0.0";
	public Integer id = 0;
	public String name = "unspecified";
	public String token = "unspecified";
	public boolean unsupportedClient = false;

	public Client(Socket socket, Integer id, String name, String token, boolean unsupportedClient) {
		this.socket = socket;
		this.address = socket.getRemoteSocketAddress().toString().split(":")[0].replace("/", "");
		this.id = id;
		this.name = name;
		this.token = token;
		this.unsupportedClient = unsupportedClient;
	}

	public boolean sendMessage(String msg) {
		try {
			socket.getOutputStream().write(msg.getBytes());
			socket.getOutputStream().flush();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean closeTunnel() {
		try {
			socket.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

}

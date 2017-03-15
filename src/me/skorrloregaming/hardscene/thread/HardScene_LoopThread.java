package me.skorrloregaming.hardscene.thread;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.skorrloregaming.hardscene.HardScene;
import me.skorrloregaming.hardscene.event.ClientConnectEvent;
import me.skorrloregaming.hardscene.http.WebServer;
import me.skorrloregaming.hardscene.http.WebSocket;
import me.skorrloregaming.hardscene.interfaces.Client;
import me.skorrloregaming.hardscene.interfaces.Logger;

public class HardScene_LoopThread implements Runnable {

	private String unsupportedAuthentication(Socket socket) {
		try {
			Thread.sleep(350);
			String na = "na";
			boolean patternMatch = false;
			while (na.length() > 16 || na.length() < 3 || patternMatch) {
				if (socket.getInputStream().available() > 0)
					return "na";
				socket.getOutputStream().write("Display Name: ".getBytes());
				socket.getOutputStream().flush();
				byte[] nameBytes = new byte[24];
				socket.getInputStream().read(nameBytes);
				na = new String(nameBytes, StandardCharsets.UTF_8).trim();
				na = na.replace("~!", "");
				Pattern pattern = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);
				Matcher m = pattern.matcher(na.replace("_", ""));
				patternMatch = m.find();
				if (na.length() > 14 || na.length() < 4) {
					socket.getOutputStream().write(("Please specify a name with a length between 3 and 16." + '\r' + '\n').getBytes());
					socket.getOutputStream().flush();
				} else if (patternMatch) {
					socket.getOutputStream().write(("Invalid display name syntax, please try again." + '\r' + '\n').getBytes());
					socket.getOutputStream().flush();
				}
			}
			byte[] tokenBytes = new byte[24];
			long resolute = -1;
			while (resolute < 100) {
				if (socket.getInputStream().available() > 0)
					socket.getInputStream().read(tokenBytes);
				String line = "Auth Token: ";
				if (resolute > -1)
					line = '\r' + '\n' + line;
				socket.getOutputStream().write(line.getBytes());
				socket.getOutputStream().flush();
				long pastTime = System.currentTimeMillis();
				socket.getInputStream().read(tokenBytes);
				long newTime = System.currentTimeMillis();
				resolute = newTime - pastTime;
			}
			na = na + "~!" + new String(tokenBytes, StandardCharsets.UTF_8).toString();
			return na;
		} catch (Exception ex) {
			return "na";
		}
	}

	@Override
	public void run() {
		Socket socket = null;
		while (HardScene.running) {
			try {
				socket = HardScene.server.accept();
			} catch (Exception ig) {
				break;
			}
			// START: Unsupported client support
			boolean unsupportedClient = false;
			String na = "na";
			try {
				Thread.sleep(500);
				if (socket.getInputStream().available() == 0) {
					unsupportedClient = true;
					na = unsupportedAuthentication(socket);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			Random ran = new Random();
			byte[] messageBytes = new byte[2048];
			String name = na;
			boolean webClient = false;
			boolean webServer = false;
			Integer clientID = ran.nextInt(900) + 100;
			if (na.equals("na")) {
				try {
					if (socket.getInputStream().read(messageBytes) == -1) {
						Logger.info(HardScene.formatAddress(socket) + " closed its socket before it could be processed.");
					} else {
						name = new String(messageBytes, StandardCharsets.UTF_8).trim();
						String res = name.split("\\r?\\n")[0];
						if (res.contains("HTTP/1.1")) {
							String header = res.split("/")[1].replace(" HTTP", "").toLowerCase();
							if (header.equals("websocket")) {
								WebSocket ws = new WebSocket(socket, name.split("\\r?\\n"), clientID);
								ws.bind();
								Thread.sleep(500);
								if (!ws.readLogin()) {
									Logger.info(HardScene.formatAddress(socket) + " closed its socket before it could be processed.");
									webServer = true;
								} else {
									name = ws.getWebSocketClient().name + "~!" + ws.getWebSocketClient().token;
									ws.start();
									webClient = true;
								}
							} else if (header.equals("")) {
								new WebServer(socket, name.split("\\r?\\n")).bind();
								webServer = true;
							} else {
								DataOutputStream out = new DataOutputStream(socket.getOutputStream());
								Logger.info("WebServer (" + HardScene.formatAddress(socket) + "): " + name.split("\\r?\\n")[0]);
								out.writeBytes("HTTP/1.1 200 OK\r\n");
								out.writeBytes("Content-Type: text/html\r\n\r\n");
								out.writeBytes("<h1>Not Found</h1>\r\n<p>The requested URL /" + header + " was not found on this server.</p>\r\n");
								out.writeBytes("<p>Additionally, a 404 Not Found error was encountered while trying to use an ErrorDocument to handle the request.</p>");
								out.flush();
								socket.close();
								webServer = true;
							}
						}
					}
				} catch (Exception e) {
					Logger.info(HardScene.formatAddress(socket) + " closed its socket before it could be processed.");
				}
			}
			if (!webServer && name.length() < 100 && !name.equals("na")) {
				// END: Unsupported client support
				try {
					String token = "";
					try {
						token = name.trim().split("~!")[1];
					} catch (Exception ig) {
					}
					Client client = new Client(socket, clientID, name.trim().split("~!")[0], token, unsupportedClient, webClient);
					if (HardScene.bannedManager.propertyExists(client.address)) {
						Logger.info(client.address + " has been denied access to connect due to being banned.");
						client.closeTunnel();
					} else {
						if (!HardScene.config.allowSameNameClients) {
							for (Client c : HardScene.instance.clients.values()) {
								if (c.name.equals(client.name)) {
									c.sendMessage("Connection aborted, as you logged in from another location.");
									c.closeTunnel();
								}
							}
						}
						try {
							new ClientConnectEvent(client);
						} catch (IOException e) {
							e.printStackTrace();
						}
						if (!webClient) {
							Thread acceptIncomingSignals = new Thread(new HardScene_ListenThread(client));
							acceptIncomingSignals.start();
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
}
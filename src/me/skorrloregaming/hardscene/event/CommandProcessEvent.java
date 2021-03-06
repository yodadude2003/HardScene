package me.skorrloregaming.hardscene.event;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Properties;

import me.skorrloregaming.hardscene.HardScene;
import me.skorrloregaming.hardscene.config.ConfigurationManager;
import me.skorrloregaming.hardscene.interfaces.Client;
import me.skorrloregaming.hardscene.interfaces.LegacyCommandSender;

public class CommandProcessEvent {

	public CommandProcessEvent(String[] args, LegacyCommandSender logger) {
		try {
			if (args[0].equalsIgnoreCase("help")) {
				viewHelp(logger);
			} else if (args[0].equalsIgnoreCase("op")) {
				if (args.length >= 2) {
					String address = args[1];
					boolean hit = false;
					for (Client c : HardScene.instance.clients.values()) {
						if (c.name.equalsIgnoreCase(address)) {
							address = c.name;
							hit = true;
						}
					}
					if (hit) {
						boolean result = HardScene.operatorManager.addProperty(address);
						if (result) {
							logger.sendMessage("Success. That address is now considered operator.");
						} else {
							logger.sendMessage("Failed. That address is already operator.");
						}
					} else {
						logger.sendMessage("Could not find a client with that username.");
						return;
					}
				} else {
					logger.sendMessage("Failed. Syntax: '" + logger.preCommandSyntax + "ban (ip)'");
				}
			} else if (args[0].equalsIgnoreCase("deop")) {
				if (args.length >= 2) {
					String address = args[1];
					boolean result = HardScene.operatorManager.removeProperty(address);
					if (result) {
						logger.sendMessage("Success. That address is no longer operator.");
					} else {
						logger.sendMessage("Failed. Could not find a operator with that username.");
						logger.sendMessage("Just so you know, the username is case-sensitive.");
					}
				} else {
					logger.sendMessage("Failed. Syntax: '" + logger.preCommandSyntax + "pardon (ip)'");
				}
			} else if (args[0].equalsIgnoreCase("ban")) {
				if (args.length >= 2) {
					String address = args[1];
					boolean result = HardScene.bannedManager.addProperty(address);
					for (Client c : HardScene.instance.clients.values()) {
						String clientAddress = c.address.split(":")[0];
						if (clientAddress.equals(address)) {
							c.closeTunnel();
						}
					}
					if (result) {
						logger.sendMessage("Success. That address is no longer allowed to connect");
					} else {
						logger.sendMessage("Failed. That address is already not allowed to connect.");
					}
				} else {
					logger.sendMessage("Failed. Syntax: '" + logger.preCommandSyntax + "ban (ip)'");
				}
			} else if (args[0].equalsIgnoreCase("pardon")) {
				if (args.length >= 2) {
					String address = args[1];
					boolean result = HardScene.bannedManager.removeProperty(address);
					if (result) {
						logger.sendMessage("Success. That address is now allowed to connect.");
					} else {
						logger.sendMessage("Failed. That address is currently allowed to connect.");
					}
				} else {
					logger.sendMessage("Failed. Syntax: '" + logger.preCommandSyntax + "pardon (ip)'");
				}
			} else if (args[0].equalsIgnoreCase("check")) {
				if (HardScene.running) {
					logger.sendMessage("The server socket is properly running.");
				} else {
					logger.sendMessage("The server socket is not properly running.");
				}
			} else if (args[0].equalsIgnoreCase("reload")) {
				HardScene.config = new ConfigurationManager(HardScene.configFile);
				if (args.length > 1 && HardScene.configFile.exists()) {
					int port = 0;
					try {
						port = Integer.parseInt(args[1]);
					} catch (Exception ex) {
					}
					if (port > 0 && port < 65535 && port > 1) {
						Properties p = new Properties();
						try (FileReader reader = new FileReader(HardScene.configFile)) {
							p.load(reader);
							p.setProperty("port", port + "");
							if (HardScene.configFile.exists())
								HardScene.configFile.delete();
							FileOutputStream outputStream = new FileOutputStream(HardScene.configFile);
							p.store(outputStream, null);
							outputStream.close();
						}
						logger.sendMessage("Success. Server socket port changed.");
					} else {
						logger.sendMessage("Failed. Server socket port was not changed.");
					}
				}
				if (HardScene.running) {
					new CommandProcessEvent("toggle ".split(" "), logger);
					new CommandProcessEvent("toggle ".split(" "), logger);
				} else {
					new CommandProcessEvent("toggle ".split(" "), logger);
				}
				logger.sendMessage("Success. Legacy configuration file reloaded.");
			} else if (args[0].equalsIgnoreCase("toggle") || args[0].equalsIgnoreCase("stop")) {
				if (HardScene.running) {
					logger.sendMessage("Terminating server socket..");
					try {
						HardScene.server.close();
						HardScene.running = false;
						logger.sendMessage("Terminating child threads..");
						for (Client c : HardScene.instance.clients.values()) {
							c.closeTunnel();
						}
						HardScene.instance.clients.clear();
					} catch (Exception ignored) {
					}
					logger.sendMessage("Success.");
					return;
				} else {
					logger.sendMessage("Starting server socket and child threads..");
					boolean result = HardScene.instance.startServer();
					if (result) {
						logger.sendMessage("Success.");
					} else {
						logger.sendMessage("Failed. An internal error occurred, check logs for more information.");
					}
				}
			} else if (args[0].equalsIgnoreCase("kick")) {
				if (args.length >= 2) {
					if (!HardScene.instance.clients.containsKey(Integer.parseInt(args[1]))) {
						logger.sendMessage("Failed. Could not find a client with the ID:" + args[1] + ".");
						return;
					}
					Client c = HardScene.instance.clients.get(Integer.parseInt(args[1]));
					logger.sendMessage("Kicking the specified client from the server..");
					try {
						c.closeTunnel();
					} catch (Exception ignored) {
					}
					logger.sendMessage("Success.");
				} else {
					logger.sendMessage("Failed. Syntax: '" + logger.preCommandSyntax + "kick (id)'");
				}
			} else if (args[0].equalsIgnoreCase("broadcast")) {
				if (args.length >= 2) {
					String message = logger.getName() + " says..";
					for (int i = 1; i < args.length; i++) {
						message += " " + args[i];
					}
					logger.sendMessage("Broadcasting message to all connected clients..");
					try {
						HardScene.broadcast(message);
					} catch (Exception ignored) {
					}
					logger.sendMessage("Success. Broadcasted message to online clients.");
				}
			} else if (args[0].equalsIgnoreCase("tell")) {
				if (args.length >= 3) {
					if (!HardScene.instance.clients.containsKey(Integer.parseInt(args[1]))) {
						logger.sendMessage("Could not find a client with the ID:" + args[1] + ".");
						return;
					}
					Client c = HardScene.instance.clients.get(Integer.parseInt(args[1]));
					String message = logger.getName() + " says..";
					for (int i = 2; i < args.length; i++) {
						message += " " + args[i];
					}
					c.sendMessage(message);
					logger.sendMessage("Success. Sent message privately to client " + c.id + ".");
					return;
				} else {
					logger.sendMessage("Failed. Syntax: '" + logger.preCommandSyntax + "tell (id) (message)'");
				}
			} else if (args[0].equalsIgnoreCase("list")) {
				if (args.length >= 2) {
					if (args[1].equalsIgnoreCase("/a")) {
						logger.sendMessage("Listing current clients in complete mode..");
						for (Client c : HardScene.instance.clients.values()) {
							logger.sendMessage(c.address.toString() + " / " + c.id + " / " + c.name);
						}
						return;
					} else {
						logger.sendMessage("Failed. Syntax: '" + logger.preCommandSyntax + "list [/a]'");
						return;
					}
				}
				logger.sendMessage("Listing current clients in safe mode..");
				for (Client c : HardScene.instance.clients.values()) {
					logger.sendMessage("0.0.0.0 / " + c.id + " / " + c.name);
				}
			} else {
				logger.sendMessage("Failed. The specified command was not found.");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.sendMessage("Failed. An internal error was encountered.");
		}
	}

	public void viewHelp(LegacyCommandSender logger) {
		logger.sendMessage("HardScene - Commands");
		logger.sendMessage("" + logger.preCommandSyntax + "help - Display this listing.");
		logger.sendMessage("" + logger.preCommandSyntax + "op (clientName) - Op client.");
		logger.sendMessage("" + logger.preCommandSyntax + "deop (clientName) - Deop client.");
		logger.sendMessage("" + logger.preCommandSyntax + "ban (ip) - Ban client.");
		logger.sendMessage("" + logger.preCommandSyntax + "pardon (ip) - Pardon client.");
		logger.sendMessage("" + logger.preCommandSyntax + "kick (id) - Kick client.");
		logger.sendMessage("" + logger.preCommandSyntax + "check - Check state of server.");
		logger.sendMessage("" + logger.preCommandSyntax + "reload [port] - Reload server config.");
		logger.sendMessage("" + logger.preCommandSyntax + "stop - Shutdown the server.");
		logger.sendMessage("" + logger.preCommandSyntax + "list [/a] - List clients.");
		logger.sendMessage("" + logger.preCommandSyntax + "tell (id) (msg) - Message client.");
		logger.sendMessage("" + logger.preCommandSyntax + "broadcast (msg) - Broadcast message.");
	}

}

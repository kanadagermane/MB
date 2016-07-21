package producer;

import java.io.*;
import java.net.*;
import message.*;

/**
 * @author Nikolai Seip, Sebastian Mattheis, Fabian Hinz
 *
 */
public class Producer implements ProducerIF {
	private static int serverPort = 55555;
	private String name;
	private boolean registered = false;
	private InetAddress serverAddress;

	public Producer(String name, String address) throws Exception, IOException {
		this.serverAddress = InetAddress.getByName(address);
		if (!Util.testConnection(serverAddress, serverPort, 1000))
			throw new IOException("There is no server at the given address");
		String[] producers = getProducers();
		for (String n : producers) {
			if (n.equalsIgnoreCase(name))
				throw new Exception("This producer name is already taken");
		}
		this.name = name;
	}

	public boolean registerOnServer() {
		Message answer = Util.sendAndGetMessage(MessageFactory.createRegisterProducerMsg(name), serverAddress, serverPort);
		PayloadProducer answerPayload = (PayloadProducer) answer.getPayload();
		if (answerPayload.getSuccess() == true) { registered = true; return true; }
		else return false;
	}

	public boolean deregisterFromServer() {
		Message answer = Util.sendAndGetMessage(MessageFactory.createDeregisterProducerMsg(name), serverAddress, serverPort);
		PayloadProducer answerPayload = (PayloadProducer) answer.getPayload();
		if (answerPayload.getSuccess() == true) { registered = false; return true; }
		else return false;
	}

	public boolean sendMessage(String msg) {
		Message answer = Util.sendAndGetMessage(MessageFactory.createBroadcastMessage(name, msg), serverAddress, serverPort);
		PayloadMessage pm = (PayloadMessage) answer.getPayload();
		switch (pm.getMessage()) {
		case "not registered":
			return false;
		case "ok":
			return true;
		default:
			throw new RuntimeException("Test in Antwortmessage nicht interpretierbar");
		}
	}
	
	public boolean isRegistered() {
		return registered;
	}

	private String[] getProducers() {
		Message answer = Util.sendAndGetMessage(MessageFactory.createRequestProducerListMsg(), serverAddress, serverPort);
		return ((PayloadGetProducerList) answer.getPayload()).getProducers();
	}
}
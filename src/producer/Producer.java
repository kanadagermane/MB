package producer;

import java.io.*;
import java.net.*;

import message.*;

public class Producer {
	private static int serverPort = 55555;
	private String name;
	private InetAddress serverAddress;

	public Producer(String name, String address) throws IOException{
		this.serverAddress = InetAddress.getByName(address);
		if (!testConnection(serverAddress, 1000))
			throw new IOException("There is no server at the given address");
		this.name = name;
	}
	private boolean testConnection(InetAddress address, int timeout) {
		Socket server = new Socket();
		try {
			server.connect(new InetSocketAddress(address, serverPort), timeout);
			server.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	public String[] getProducers() {
		Message answer = this.sendAndGetMessage(new Message(MessageType.getProducerList, null), serverAddress);
		return ((PayloadGetProducerList) answer.getPayload()).getProducers();
	}
	private Message sendAndGetMessage(Message message, InetAddress address) {
		Socket server;
		try {
			server = new Socket(address, serverPort);

			Message answer = null;
			ObjectOutputStream out = null;
			ObjectInputStream in = null;

			out = new ObjectOutputStream(server.getOutputStream());
			in = new ObjectInputStream(server.getInputStream());
			out.writeObject(message);
			answer = (Message) in.readObject();

			in.close();
			out.close();
			server.close();
			return answer;
		} catch (Exception e) {
			return null;
		}
}

}
package consumer;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import message.*;

public class Consumer {
	private static int serverPort = 55555;
	private int consumerID;
	private InetAddress mcastadr;
	private InetAddress serverAddress;
	private HashSet<String> subscriptions;
	private HashSet<String> producerList;
	MulticastSocket udpSocket;

	/**
	 * Used to interact with a MessageServer
	 * 
	 * @param address
	 *            address of the server
	 * @throws IOException
	 *             when the address is not reachable.
	 */
	public Consumer(String address) throws IOException {
		subscriptions = new HashSet<>();
		producerList = new HashSet<>();
		this.serverAddress = InetAddress.getByName(address);
		if (!testConnection(serverAddress, 1000))
			throw new IOException("There ist no server on the specified address");
	}

	/**
	 * Checks if it is possible to establish a TCP connection using the "serverPort"
	 * 
	 * @param adress
	 *            The address of the server to be checked
	 * @param timeout
	 *            Timeout of the connection
	 * @return if the connection was successful
	 */
	private boolean testConnection(InetAddress adress, int timeout) {

		try (Socket server = new Socket();) {
			server.connect(new InetSocketAddress(adress, serverPort), timeout);
			return true;
		} catch (IOException e) {
			return false;
		}

	}

	/**
	 * Fetches a list of all producers this consumer is subscribed to
	 * 
	 * @return a list of producers this consumer is subscribed to
	 */
	public String[] getSubscriptions() {
		return subscriptions.toArray(new String[0]);
	}

	/**
	 * Subscribes this user to the given producers
	 * 
	 * @param producers
	 *            The names of the producers (can not be null)
	 * @return a list of producers, it wasn't possible to subscribe on
	 */
	public String[] subscribeToProducers(String[] producers) {
		if (producers == null)
			throw new IllegalArgumentException("'producers' may not be null");
		List<String> list = new LinkedList<>();
		for (String s : producers) {
			// existiert dieser Producer?
			if (this.producerList.contains(s)) {
				subscriptions.add(s);
			} else {
				list.add(s);
			}
		}
		return list.toArray(new String[0]);

	}

	/**
	 * Unsubscribes this user from the given producers
	 * 
	 * @param producers
	 *            The names of the producers (can not be null)
	 * @return
	 */
	public String[] unsubscribeFromProducers(String[] producers) {
		if (producers == null)
			throw new IllegalArgumentException("'producers' may not be null");
		List<String> list = new LinkedList<>();
		for (String s : producers) {
			if (!subscriptions.remove(s)) {
				list.add(s);
			}
		}
		return list.toArray(new String[0]);
	}

	/**
	 * Fetches all available producers
	 * 
	 * @return all available producers
	 */
	public String[] getProducers() {
		Message answer = this.sendAndGetMessage(new Message(MessageType.getProducerList, null), serverAddress);
		for (String s : ((PayloadGetProducerList) answer.getPayload()).getProducers()) {
			producerList.add(s);
		}
		return producerList.toArray(new String[0]);
	}

	/**
	 * Registers the user on the server
	 */
	public void registerOnServer() {
		Message answer = sendAndGetMessage(new Message(MessageType.RegisterConsumer, null), serverAddress);
		PayloadRegisterConsumer answerPayload = (PayloadRegisterConsumer) answer.getPayload();
		this.consumerID = answerPayload.getId();
		this.mcastadr = answerPayload.getMulticastAddress();
		// TODO Operation successful?
		// TODO vlt die Methode registerOnMulticastGroup hier aufrufen und nicht seperat?
	}

	/**
	 * Registers the user on the server
	 * 
	 * @return if the operation was successful
	 */
	public boolean deregisterFromServer() {

		Message answer = sendAndGetMessage(new Message(MessageType.DeregisterConsumer, new PayloadDeregisterConsumer(consumerID)), serverAddress);
		PayloadDeregisterConsumer answerPayload = (PayloadDeregisterConsumer) answer.getPayload();

		return answerPayload.getSenderID() != 0;

	}

	/**
	 * Sends a message object to the server via TCP and receives it afterwards
	 * 
	 * @param message
	 *            the message to be sent
	 * @param address
	 *            the address the message is to be sent to
	 * @return the answer from the server
	 */
	private Message sendAndGetMessage(Message message, InetAddress address) {

		try (Socket server = new Socket(address, serverPort);
				ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(server.getInputStream());) {

			out.writeObject(message);
			Message answer = (Message) in.readObject();
			return answer;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * method name says all
	 */
	public void joinMulticastGroup() {
		try {
			udpSocket = new MulticastSocket();
			udpSocket.joinGroup(mcastadr);
		} catch (IOException e) {
			System.out.println("IOFehler beim Registrieren in der Multicastgruppe");
			e.printStackTrace();

		}

		Thread t = new Thread(new WaitForMessage(udpSocket));
		t.start();
	}

	public void deregisterFromMulticastGroup() {
		try {
			udpSocket.leaveGroup(mcastadr);
		} catch (IOException e) {
			System.out.println("IOFehler beim Verlassen der Multicastgruppe");
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * The class is listening for messages from the server and prints them on the console
	 *
	 */
	private class WaitForMessage implements Runnable {
		MulticastSocket udps;

		public WaitForMessage(MulticastSocket udps) {
			this.udps = udps;
		}

		@Override
		public void run() {
			// TODO Idee mit Pipes zu arbeiten und dem User ne Möglichkeit zu geben, abzufragen, ob es neue Nachrichten gibt...
			DatagramPacket dp = null;
			while (true) {
				try {
					udps.receive(dp);
					Message m = Message.getMessageFromDatagramPacket(dp);

					switch (m.getType()) {

					case DeregisterProducer:
						if (m.getType() != MessageType.DeregisterProducer)
							throw new RuntimeException("Falscher Payload in GetMessage");
						PayloadProducer pp = (PayloadProducer) m.getPayload();
						System.out.println("Der Producer " + pp.getName()
								+ " hat den Dienst eingestellt. Sie k�nnen leider keine Push-Nachrichten mehr von ihm erhalten...");
						producerList.remove(pp.getName());
						subscriptions.remove(pp.getName());
						break;

					case Message:
						if (m.getType() != MessageType.Message)
							throw new RuntimeException("Falscher Payload in GetMessage");
						System.out.println("Sie haben eine neue Push-Mitteilung:");
						// er schreibt ja jetzt einfach raus ... vlt funktioniert dies nicht, weil im hauptthread er gerade auf ne Eingabe wartet ... vlt muss
						// man dann hier den Hauptthread einschläfern und nach der Ausgabe wieder aufwecken?!
						PayloadMessage payload = (PayloadMessage) m.getPayload();
						if (subscriptions.contains(payload.getName()))
							System.out.println(payload.getName() + " meldet: \n" + payload.getText());
						break;

					default:
						break;

					}

				} catch (IOException e) {
					System.out.println("Fehler beim Bearbeiten der erhaltenen UDP-Message");
					e.printStackTrace();
				}
			}
		}
	}

}

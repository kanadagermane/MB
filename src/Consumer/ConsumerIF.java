package Consumer;

public interface ConsumerIF {

	/**
	 * This method starts the "Userinterface"
	 */
	public void startAction();

	/**
	 * This method asks for a list of Producers and gives the user the possibilty to choose producers from which he wants to get Messages.
	 */
	public void registerOnProducers();

	/**
	 * This Method registers the Consumer on the Server
	 * 
	 * @return the ID of the registered Consumer
	 */
	public int registerOnServer();

	/**
	 * This method deregisters the consumer from the server.
	 */
	public void deregister();

	/**
	 * This method handles with the Message of the Producers and shows them to the User
	 */
	public void getMessage();

}

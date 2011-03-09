package playground.sergioo.AddressLocator;

@SuppressWarnings("serial")
public class BadAddressException extends Exception {
	
	//Methods
	/**
	 * Constructs an bad address exception
	 */
	public BadAddressException(String error) {
		super("The address is wrong: "+error+".");
	}
	
}

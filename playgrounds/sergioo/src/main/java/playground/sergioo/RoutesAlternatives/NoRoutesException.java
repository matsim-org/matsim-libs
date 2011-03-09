package playground.sergioo.RoutesAlternatives;

@SuppressWarnings("serial")
public class NoRoutesException extends Exception {
	
	//Methods
	/**
	 * Constructs an bad address exception
	 */
	public NoRoutesException() {
		super("No routes were found");
	}
	
}

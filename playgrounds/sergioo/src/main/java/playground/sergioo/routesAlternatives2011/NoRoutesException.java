package playground.sergioo.routesAlternatives2011;

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

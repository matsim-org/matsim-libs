package playground.toronto.exceptions;

/**
 * Indicates that a network's ID references don't match up, or any other problem associated with
 * reading an incorrectly-formatted Matsim network from an XML file
 * 
 * @author pkucirek
 */
public class NetworkFormattingException extends Exception{

	private static final long serialVersionUID = 1L;
	
	public NetworkFormattingException(){
		super();
	}
	
	public NetworkFormattingException(String msg){
		super(msg);
	}
	
}

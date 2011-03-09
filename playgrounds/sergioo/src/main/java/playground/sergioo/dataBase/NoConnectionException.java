package playground.sergioo.dataBase;

/**
 * @author Sergio Ordóñez
 */
public class NoConnectionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public NoConnectionException() {
		super("Error en la conexión con la base de datos");		
	}
	
}

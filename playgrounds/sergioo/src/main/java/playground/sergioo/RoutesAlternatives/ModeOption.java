package playground.sergioo.RoutesAlternatives;

public enum ModeOption {
	
	//Values
	DRIVING("d"),
	WALKING("w"),
	PUBLIC_TRANSIT("r"),
	BIKING("b");
	
	//Attributes
	/**
	 * The value of the mode in an URL
	 */
	private String param;
	
	//Methods
	/**
	 * @param param
	 */
	private ModeOption(String param) {
		this.param = param;
	}
	/**
	 * @return the param
	 */
	public String getParam() {
		return param;
	}
	
}

package playground.sergioo.RoutesAlternatives;

public class Leg {
	
	//Attributes
	/**
	 * The starting location
	 */
	private String start;
	/**
	 * The end location
	 */
	private String end;
	/**
	 * The mode
	 */
	private Mode mode;
	/**
	 * The distance
	 */
	private double distance;
	
	//Methods
	/**
	 * @param start
	 * @param end
	 * @param mode
	 */
	public Leg(String start, String end, Mode mode) {
		super();
		this.start = start;
		this.end = end;
		this.mode = mode;
	}
	/**
	 * @param start
	 * @param end
	 * @param mode
	 * @param distance
	 */
	public Leg(String start, String end, Mode mode, double distance) {
		super();
		this.start = start;
		this.end = end;
		this.mode = mode;
		this.distance = distance;
	}
	/**
	 * @return the start
	 */
	public String getStart() {
		return start;
	}
	/**
	 * @return the end
	 */
	public String getEnd() {
		return end;
	}
	/**
	 * @return the mode
	 */
	public Mode getMode() {
		return mode;
	}
	/**
	 * @return the distance
	 */
	public double getDistance() {
		return distance;
	}
	/**
	 * @return The leg as a text
	 */
	public String toString() {
		if(distance!=0)
			return "("+start+":"+end+")"+mode+"("+distance+")";
		else
			return "("+start+":"+end+")"+mode;
	}
}

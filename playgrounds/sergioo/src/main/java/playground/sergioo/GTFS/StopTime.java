package playground.sergioo.GTFS;

public class StopTime {
	
	//Attributes
	private String arrivalTime;
	private String departureTime;
	private String stopId;
	
	//Methods
	/**
	 * @param arrivalTime
	 * @param departureTime
	 * @param stopId
	 */
	public StopTime(String arrivalTime, String departureTime, String stopId) {
		super();
		this.arrivalTime = arrivalTime;
		this.departureTime = departureTime;
		this.stopId = stopId;
	}
	/**
	 * @return the arrivalTime
	 */
	public String getArrivalTime() {
		return arrivalTime;
	}
	/**
	 * @return the departureTime
	 */
	public String getDepartureTime() {
		return departureTime;
	}
	/**
	 * @return the stopId
	 */
	public String getStopId() {
		return stopId;
	}
	
}

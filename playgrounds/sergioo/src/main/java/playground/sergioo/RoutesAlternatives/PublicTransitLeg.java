package playground.sergioo.RoutesAlternatives;


public class PublicTransitLeg extends Leg {
	
	//Attributes
	/**
	 * The origin station
	 */
	private String originStationId;
	/**
	 * The destination station
	 */
	private String destinationStationId;
	/**
	 * The type
	 */
	private String vehicleRouteId;
	
	//Methods
	/**
	 * @param start
	 * @param end
	 * @param mode
	 */
	public PublicTransitLeg(String start, String end, Mode mode, String originStationId, String destinationStationId, String vehicleRouteId) {
		super(start, end, mode);
		this.originStationId = originStationId;
		this.vehicleRouteId = vehicleRouteId;
		this.destinationStationId = destinationStationId;
	}
	/**
	 * @return the stationId
	 */
	public String getStationId() {
		return originStationId;
	}
	/**
	 * @return the destinationStationId
	 */
	public String getDestinationStationId() {
		return destinationStationId;
	}
	/**
	 * @return the vehicleRouteId
	 */
	public String getVehicleRouteId() {
		return vehicleRouteId;
	}
	/**
	 * @return The leg as a text
	 */
	public String toString() {
		return super.toString()+"{"+originStationId+"-"+destinationStationId+","+vehicleRouteId+"}";
	}
}

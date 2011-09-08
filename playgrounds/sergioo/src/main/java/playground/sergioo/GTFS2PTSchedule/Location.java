package playground.sergioo.GTFS2PTSchedule;

public class Location {
	
	//Attributes
	/**
	 * The latitude located
	 */
	private double latitude;
	/**
	 * The longitude located
	 */
	private double longitude;
	
	//Methods
	/**
	 * Generates a default location
	 */
	public Location() {
		super();
	}
	/**
	 * @param longitude
	 * @param latitude
	 */
	public Location(double latitude, double longitude) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
	}
	/**
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}
	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	/**
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}
	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	/**
	 * @return the location in a text form
	 */
	public String toString() {
		return latitude+","+longitude;
	}
	
}

/**
 * 
 */
package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;

/**
 * @author schn_se
 * stores a trip that is part of a logbook
 */
public class Pwvm_LogbookTrip {
	private int tripId; // the tripId within the logbook
	private int source_type;
	private int dest_type;
	private int purpose;
	private String destGeometry;
	private double z_source;
	private double z_home;
	private double distanceEmpirical; // also nicht Luftlinie, sondern gemaess Fragebogen
	private double distanceAirline; // Luftlinie, errechnet
	private String start_time;
	private String stop_time;
	private boolean isReversed;
	
	public Pwvm_LogbookTrip (Pwvm_LogbookTrip t) {
		this.tripId = t.tripId;
		this.source_type = t.source_type;
		this.dest_type = t.dest_type;
		this.purpose = t.purpose;
		this. destGeometry = t.destGeometry;
		this.z_source = t.z_source;
		this.z_home = t.z_home;
		this.distanceEmpirical = t.distanceEmpirical;
		this.distanceAirline = t.distanceAirline;
		this.start_time = t.start_time;
		this.stop_time = t.stop_time;
		this.isReversed = t.isReversed;
	}
	
	public String getStartTime() {
		return start_time;
	}

	public String getStopTime() {
		return stop_time;
	}

	public Pwvm_LogbookTrip(int tripId, int source_type, int dest_type, int purpose, double z_source, double z_home, double distanceAirline, double distanceEmpirical, String destGeometry, String starttime, String stoptime) {
		this.tripId = tripId;
		this.source_type = source_type;
		this.dest_type = dest_type;
		this.purpose = purpose;
		this.z_source = z_source;
		this.z_home = z_home;
		this.distanceAirline = distanceAirline;
		this.distanceEmpirical = distanceEmpirical;
		this.destGeometry = destGeometry;
		this.start_time = starttime;
		this.stop_time = stoptime;
		this.isReversed = false;
	}
	
	

	public boolean isReversed() {
		return isReversed;
	}

	public void setReversed(boolean isReversed) {
		this.isReversed = isReversed;
	}

	public int getTypeOfDestination() {
		return dest_type;
	}
	
	public void setTypeOfDestination(int destType) {
		dest_type = destType;
	}

	public int getPurpose() {
		return purpose;
	}

	public double getZFromSource() {
		return z_source;
	}
	
	public double getZFromHome() {
		return z_home;
	}

	public double getEmpiricalDistance() {
		return distanceEmpirical;
	}

	public double getAirLineDistance() {
		return distanceAirline;
	}

	public void setZFromSource(double z_source) {
		this.z_source = z_source;
	}

	public void setZFromHome(double z_home) {
		this.z_home = z_home;
	}

	/**
	 * Returns the geometry of the trip's destination
	 * @return 
	 */
	public String getDestGeometry() {
		return destGeometry;
	}

	public void setDestGeometry(String destGeometry) {
		this.destGeometry = destGeometry;
	}

	public int getTripId() {
		return tripId;
	}

	public int getTypeOfSource() {	
		return source_type;
	}
	
	public void setTypeOfSource(int sourceType) {
		this.source_type = sourceType;
	}

}

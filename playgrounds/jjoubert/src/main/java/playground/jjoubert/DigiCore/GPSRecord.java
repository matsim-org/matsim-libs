package playground.jjoubert.DigiCore;

public class GPSRecord implements Comparable<GPSRecord>{
	private int vehID;
	private long time;
	private double longitude;
	private double latitide;
	private int status;
	private int speed;
	
	/**
	 * A container class to handle and sort the GPS log records obtained from the DigiCore
	 * data set.  
	 * @param vehID a unique integer value identifying each vehicle;
	 * @param time a UNIX-based time stamp indicating when the GPS log was created;
	 * @param x the longitude, expressed as decimal degrees (WGS84);
	 * @param y the latitude, expressed as decimal degrees (WGS84);
	 * @param status an integer digit indicating a predefined vehicle status. Status codes 
	 * 		  are described in <code>Statuses.xls</code>;
	 * @param speed of the vehicle when GPS log was created. This turned out to not be usable.
	 */
	
	public GPSRecord(int vehID, long time, double x, double y, int status, int speed) {
		this.vehID = vehID;
		this.time = time;
		this.longitude = x;
		this.latitide = y;
		this.status = status;
		this.speed = speed;
	}

	public int getVehID() {
		return vehID;
	}

	public long getTime() {
		return time;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getLatitude() {
		return latitide;
	}

	public int getStatus() {
		return status;
	}

	public int getSpeed() {
		return speed;
	}

	/**
	 * The <code>compareTo</code> method has been overwritten. A <code>GPSRecord</code> is
	 * considered <i>smaller</i> than another if its UNIX-based time stamp is <i>earlier</i>
	 * than the other.
	 */
	public int compareTo(GPSRecord o) {
		return (int) (this.getTime() - o.getTime() );
	}


}

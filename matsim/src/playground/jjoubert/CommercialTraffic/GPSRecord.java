package playground.jjoubert.CommercialTraffic;

public class GPSRecord implements Comparable<GPSRecord>{
	private int vehID;
	private long time;
	private double longitude;
	private double latitide;
	private int status;
	private int speed;
	
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

	public int compareTo(GPSRecord o) {
		return (int) (this.getTime() - o.getTime() );
	}


}

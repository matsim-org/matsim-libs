package playground.jjoubert.CommercialTraffic;

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.vividsolutions.jts.geom.Coordinate;

public class GPSPoint implements Comparable<GPSPoint>{
	int vehID;
	GregorianCalendar time;
	int status;
	Coordinate coordinate;
	public static final String SA_TIME_ZONE = "GMT+2";
	public static final String SA_LANGUAGE = "en";
	public static final String SA_CODE = "ZA";
	
	public GPSPoint(int vehID, long time, int status, Coordinate c){
		this.vehID = vehID;
		this.status = status;
		this.coordinate = c;
		GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone(SA_TIME_ZONE), new Locale(SA_LANGUAGE, SA_CODE) );
		gc.setTimeInMillis(time*1000);
		this.time = gc;
	}
	
	public long getVehID() {
		return vehID;
	}
	
	public GregorianCalendar getTime() {
		return time;
	}

	public int getStatus() {
		return status;
	}

	public int compareTo(GPSPoint o) {
		return this.time.compareTo(o.time);
	}
	
	public Coordinate getCoordinate(){
		return this.coordinate;
	}
	
	public void setCoordinate(Coordinate c){
		this.coordinate = c;
	}
}

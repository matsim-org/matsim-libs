package playground.jjoubert.CommercialTraffic;

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A class to capture a single GPS point from the DigiCore data set
 * to study commercial vehicle movement. 
 * 
 * ... This could maybe be changed in future to rather extend the
 *     {@code com.vividsolutions.jts.geom.Coordinate} class. For now
 *     it merely has a coordinate attribute.
 *      
 * @author johanwjoubert
 */
public class GPSPoint implements Comparable<GPSPoint>{
	private int vehID;
	private GregorianCalendar time;
	private int status;
	private Coordinate coordinate;
	public static final String SA_TIME_ZONE = "GMT+2";
	public static final String SA_LANGUAGE = "en";
	public static final String SA_CODE = "ZA";
	
	/**
	 * Constructs a {@code GPSPoint} class.
	 * 
	 * @param vehID of type {@code int}, using the first field of the DigiCore data set.
	 * @param time of type {@code long}, expressed as seconds from 1 Jan 1970 00:00 at GMT +2.
	 * @param status of type {@code int}, as per the status sheet provided by DigiCore
	 * @param c of type {@code Coordinate}, from the library {@code com.vividsolutions.jts.geom.Coordinate}
	 */
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

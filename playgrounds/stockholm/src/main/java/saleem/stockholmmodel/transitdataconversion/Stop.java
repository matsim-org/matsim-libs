package saleem.stockholmmodel.transitdataconversion;

/** 
 * A PT stop object used in converting Excel based Stop data into MatSim based transit schedule 
 * data structure, consisting of neccessary information about stops.
 * 
 * @author Mohammad Saleem
 */
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.matsim.api.core.v01.Coord;

public class Stop {
	private Coord coord;
	private String name;
	private String id;
	private String transportmode;
	private Boolean isblocking = false;
	private Boolean awaitdeparture = false;
	private String linkRefId = "11_1";
	private String departureoffset = "00:00:00";
	private String arrivaloffset = "00:00:00";
	private String departuretime = "";
	Stop(String name, Coord coord, String id){
		this.coord = coord;
		this.name = name;
		this.id = id;
	}
	Stop(Stop stop){
		this.coord = stop.coord;
		this.name = stop.name;
		this.id = stop.id;
		this.departureoffset = stop.departureoffset;
		this.isblocking = stop.isblocking;
		this.awaitdeparture = stop.awaitdeparture;
		this.transportmode = stop.transportmode;
		this.linkRefId = stop.linkRefId;
		this.arrivaloffset = stop.arrivaloffset;
		this.departuretime = stop.departuretime;
		
	}
	public Coord getCoord(){
		return coord;
	}
	public String getTransportMode(){
		return transportmode;
	}
	public String getName(){
		return name;
	}
	public String getDepartureTime(){
		return departuretime;
	}
	
	//Trimming the long departure time format in Excel files into short format usable in MatSim transit schedule. 
	public void setDepartureTime(String departuretime){
		this.departuretime=departuretime.substring(departuretime.length()-8, departuretime.length());
	}
	public String getId(){
		return id;
	}
	public Boolean getIsBlocking(){
		return isblocking;
	}
	public Boolean getAwaitDeparture(){
		return awaitdeparture;
	}
	public String getLinkRefId(){
		return linkRefId;
	}
	public String getDepartureOffset(){
		return departureoffset;
	}
	public void setDepartureOffset(String departureoffset){
		this.departureoffset = departureoffset;
	}
	
	//Should be changed into a dictionary object; converts transportmode into MatSim form
	public void setTransportMode(String transportmode){
		if(transportmode.equals("FERRYBER")){
			this.transportmode = "FERRY";
		}
		else if(transportmode.equals("METROSTN")){
			this.transportmode = "TRAIN";
		}
		else if(transportmode.equals("TRAMSTN")){
			this.transportmode = "TRAM";
		}
		else if(transportmode.equals("RAILWSTN")){
			this.transportmode = "PENDELTAG";
		}
		else if(transportmode.equals("SHIPBER")){
			this.transportmode = "FERRY";
		}
		else if(transportmode.equals("BUSTERM")){
			this.transportmode = "BUS";
		}
	}
	public String getArrivalOffset(){
		return arrivaloffset;
	}
	/**Calculate departure offset based on departing time from previous station and arriving at current. 
	   Both times are in "yyyy-MM-dd kk:mm:ss" format.*/
	
	public String calculateDepartureOffset(String time1, String time2){
		SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
		try{
			Date date = sdf.parse(time1);
			long timeInMillis1 = date.getTime();
			date = sdf.parse(time2);
			long timeInMillis2 = date.getTime(); 
			long timeInSeconds = (timeInMillis2-timeInMillis1)/1000;
			int hours = (int) timeInSeconds / 3600;
		    int remainder = (int) timeInSeconds - hours * 3600;
		    int mins = remainder / 60;
		    remainder = remainder - mins * 60;
		    int secs = remainder;
		    String departureoffset = (hours<10)?"0"+hours+":":""+hours+":";//Hours, Mins and Secs in format "00:00:00"
		    departureoffset = (mins<10)?departureoffset+"0"+mins+":":departureoffset+mins+":";
		    departureoffset = (secs<10)?departureoffset+"0"+secs:departureoffset+secs;

			return departureoffset;
		}catch(ParseException p){
			p.printStackTrace();
			return null;
		}
	}
}

package saleem.stockholmscenario.stockholmptdata;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.matsim.api.core.v01.Coord;

public class Stop {
	Coord coord;
	String name;
	String id;
	String transportmode;
	Boolean isblocking = false;
	Boolean awaitdeparture = false;
	String linkRefId = "11_1";
	String departureoffset = "00:00:00";
	String arrivaloffset = "00:00:00";
	String departuretime = "";
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
	public void setDepartureTime(String departuretime){
		if(departuretime.startsWith("2015-05-09")){
			departuretime=departuretime.replaceFirst("2015-05-09", "");
			departuretime=departuretime.trim();
			int hours=Integer.parseInt(departuretime.substring(0, 2))+24;
			departuretime=hours+departuretime.substring(2, departuretime.length());
		}else if(departuretime.startsWith("2015-05-08")){
			departuretime=departuretime.replaceFirst("2015-05-08", "");
			departuretime=departuretime.trim();
		}else if(departuretime.startsWith("2015-06-03")){
			departuretime=departuretime.replaceFirst("2015-06-03", "");
			departuretime=departuretime.trim();
		}else if(departuretime.startsWith("2015-06-04")){
			departuretime=departuretime.replaceFirst("2015-06-04", "");
			departuretime=departuretime.trim();
		}
		this.departuretime=departuretime;
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

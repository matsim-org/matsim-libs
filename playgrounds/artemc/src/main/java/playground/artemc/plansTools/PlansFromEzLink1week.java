package playground.artemc.plansTools;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

public class PlansFromEzLink1week extends MatsimXmlWriter{

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws NoConnectionException 
	 * @throws ParseException 
	 */

	  
	
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/Ezlink_1week_DataBase.properties"));
			
		String StartTime="";
		Float Ride_Time=0f;
		String EndTime="";
		String newTime="";
		Double StartLat=0.0;
		Double StartLon=0.0;
		Double EndLat=0.0;
		Double EndLon=0.0;
//		Integer PlanID=0;
		Long CARD_ID=0L;
		String EndTimeLastLeg="";
		Double previousEndLat=0.0;
		Double previousEndLon=0.0;
		
		
		PlansFromEzLink1week plansFileFromEzLink = new PlansFromEzLink1week();
		plansFileFromEzLink.writeHeader();
		
		//MATSim object for coordinate transfomration
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N"); 
		
		//Read cardIDs for one day
		ResultSet agents = dba.executeQuery("SELECT DISTINCT CARD_ID FROM v2_card_ids12042011");
		
		int k=0,i=0;
		//Loop through obtained cardIDs
		while(agents.next()){
			i=i+1;
			CARD_ID = agents.getLong(1);
//			PlanID = i;
//			PlanID = agents.getInt(2);
			Boolean newPerson=true;
			if(i==1000){
			 k=k+1000;
			 System.out.println("Agents: "+k);
			 i=0;
			}
			
			//Read all trips of one cardID
			ResultSet rs = dba.executeQuery("SELECT CARD_ID,Ride_Start_Time, Ride_Time, start_lat,start_lon, end_lat,end_lon FROM v2_trips12042011 WHERE CARD_ID="+Long.toString(CARD_ID)+" ORDER BY Ride_Start_Time");
			plansFileFromEzLink.writeNewPerson(Long.toString(CARD_ID));
			while(rs.next()) {
				CARD_ID = rs.getLong(1);
				StartTime = rs.getString(2);
				Ride_Time = rs.getFloat(3);
				StartLat = rs.getDouble(4);
				StartLon = rs.getDouble(5);
				EndLat = rs.getDouble(6);
				EndLon = rs.getDouble(7);
//				TripID = rs.getInt(9);		
				
				String rideDuration = plansFileFromEzLink.transformMinutesToTime(Ride_Time);
				EndTime = plansFileFromEzLink.calculateEndTime(StartTime,rideDuration);
						
				//If endtime after midnight, add 24 hours 
				if(EndTime.substring(0,2).equals("00") && !StartTime.substring(0,2).equals("00") ){
			    	 newTime="24"+EndTime.substring(2,8);
			    	 EndTime=newTime;
			     }
				
				
				//Transfortm coordinates to UTM48N
				Coord coordStart = new Coord(StartLon, StartLat);
				Coord coordEnd = new Coord(EndLon, EndLat);
				Coord UTMStart = ct.transform(coordStart);
				Coord UTMEnd = ct.transform(coordEnd);
				StartLon=UTMStart.getX();
				StartLat=UTMStart.getY();
				EndLon=UTMEnd.getX();
				EndLat=UTMEnd.getY();		
				plansFileFromEzLink .writeTrip(Long.toString(CARD_ID),StartTime,rideDuration,EndTimeLastLeg,Double.toString(StartLat),Double.toString(StartLon),Double.toString(EndLat),Double.toString(EndLon),newPerson,Double.toString(previousEndLat),Double.toString(previousEndLon));
				newPerson=false;
				EndTimeLastLeg=EndTime;
				previousEndLat=EndLat;
				previousEndLon=EndLon;
			}
			plansFileFromEzLink.writeClosePerson(EndTime, Double.toString(EndLat),Double.toString(EndLon));
			rs.close();
		}
	    dba.close();
	    plansFileFromEzLink .writeEnd();
	    System.out.println("Done!");
	    System.out.println("Persons: " + plansFileFromEzLink.getPersons());
	    System.out.println("Trips: " + plansFileFromEzLink .getTrips());	    
	}

	private int persons=0;
	private int trips=0;
	private SimpleDateFormat sdf;
	
	public int getPersons() {
		return persons;
	}
	
	public int getTrips() {
		return trips;
	}
	

	public void writeNewPerson(String PlanID) throws IOException{
	//	this.appendFile("./data/ezLinkDataSimulation/plans1000.xml");
		
		List<Tuple<String, String>> listEmpty = new ArrayList<Tuple<String,String>>();
		List<Tuple<String, String>> listPerson = new ArrayList<Tuple<String,String>>();
		listPerson.add(new Tuple<String, String>("id", PlanID));
		this.writer.write(NL);
		this.writeStartTag("person", listPerson);
		this.writeStartTag("plan", listEmpty);
		persons++;
	}
	
	public void writeClosePerson(String EndTime, String EndLat,String EndLon) throws IOException  {

		List<Tuple<String, String>> listAct = new ArrayList<Tuple<String,String>>();
		listAct.add(new Tuple<String, String>("type", "dummy"));
		
//		String FinalTime = "23:59:59";
//		if(EndTime.substring(0,2).equals("24")){
//			FinalTime=EndTime;
//		}
		
		listAct.add(new Tuple<String, String>("x", EndLon));
		listAct.add(new Tuple<String, String>("y", EndLat));
		listAct.add(new Tuple<String, String>("start_time", EndTime));

		this.writeStartTag("act", listAct, true);
		this.writeEndTag("plan");
		this.writeEndTag("person");
	//	this.close();
	}
	
	public void writeTrip(String CARD_ID,String StartTime,String Duration,String EndTimeLastLeg,String StartLat,String StartLon, String EndLat,String EndLon,Boolean newPerson, String previousEndLat, String previousEndLon) throws IOException, ParseException {
		
		sdf = new SimpleDateFormat("HH:mm:ss");
		
		List<Tuple<String, String>> listActStart = new ArrayList<Tuple<String,String>>();	
		List<Tuple<String, String>> listActEnd = new ArrayList<Tuple<String,String>>();	
		List<Tuple<String, String>> listLeg = new ArrayList<Tuple<String,String>>();
		List<Tuple<String, String>> listLegUndefined = new ArrayList<Tuple<String,String>>();
		
		if (newPerson==true){
			listActStart.add(new Tuple<String, String>("type", "dummy"));
			listActStart.add(new Tuple<String, String>("x", StartLon));
			listActStart.add(new Tuple<String, String>("y", StartLat));		
			listActStart.add(new Tuple<String, String>("end_time", StartTime));
			this.writeStartTag("act", listActStart, true);					
		}
		else{	
			Date time = sdf.parse(StartTime);
			Date oneSecond = sdf.parse("00:00:01");
			Date twoSeconds = sdf.parse("00:00:02");
			long diffOne = time.getTime() - oneSecond.getTime() - 27000000;
			long diffTwo = time.getTime() - twoSeconds.getTime() - 27000000;
			
			String newEndTime = sdf.format(diffTwo);	
			String newStartTimeFakeLeg = sdf.format(diffOne);	
//			System.out.println(newEndTime);
//			System.out.println(newStartTimeFakeLeg);
//			System.out.println(StartTime);
//			System.out.println("-----------");
			
			listActEnd.add(new Tuple<String, String>("type", "dummy"));
			listActEnd.add(new Tuple<String, String>("x", previousEndLon));
			listActEnd.add(new Tuple<String, String>("y", previousEndLat));
			listActEnd.add(new Tuple<String, String>("start_time", EndTimeLastLeg));
			listActEnd.add(new Tuple<String, String>("end_time", newEndTime));
			this.writeStartTag("act", listActEnd, true);	
			
			listLegUndefined.add(new Tuple<String, String>("mode", "undefined"));
			this.writeStartTag("leg", listLegUndefined, true);
			
			listActStart.add(new Tuple<String, String>("type", "dummy"));
			listActStart.add(new Tuple<String, String>("x", StartLon));
			listActStart.add(new Tuple<String, String>("y", StartLat));
			listActStart.add(new Tuple<String, String>("start_time", newStartTimeFakeLeg));
			listActStart.add(new Tuple<String, String>("end_time", StartTime));			
			this.writeStartTag("act", listActStart, true);	
		}	
		
		listLeg.add(new Tuple<String, String>("mode", "pt"));
		this.writeStartTag("leg", listLeg, true);	
		
		trips++;
	}		
	
 public void writeHeader() throws IOException {
	 	this.useCompression(true);
	 	List<Tuple<String, String>> listEmpty = new ArrayList<Tuple<String,String>>();
	 	this.openFile("./data/ezLinkPlans/plans_timetest.xml");
		this.writeXmlHead();
		this.writeDoctype("plans", "http://www.matsim.org/files/dtd/plans_v4.dtd");
		this.writeStartTag("plans", listEmpty);
	//	this.close();		
 }	
 
 public void writeEnd() throws IOException{
//	 	this.appendFile("./data/ezLinkDataSimulation/plansComplete1.xml");
	 	this.writeEndTag("plans");
	 	this.close();
 }
 
 public String transformMinutesToTime(float minutes) throws ParseException{
		
	    Integer durationInSeconds = Math.round(minutes * 60);
		Integer durationHours = durationInSeconds / 3600;
		Integer remainder = durationInSeconds % 3600;
		Integer durationMinutes = remainder / 60;
		Integer durationSeconds = remainder % 60;
		String durationFull = durationHours+":"+durationMinutes+":"+durationSeconds;
	
		sdf = new SimpleDateFormat("HH:mm:ss");
		Date durationFull_df = sdf.parse(durationFull);
		durationFull = sdf.format(durationFull_df);
		
		return durationFull; 
	}
 
 public String calculateEndTime (String startTime, String duration) throws ParseException{
	 
		Integer startHours = Integer.parseInt(startTime.substring(0,2));
		Integer startMinutes = Integer.parseInt(startTime.substring(3,5));
		Integer startSeconds = Integer.parseInt(startTime.substring(6,8));
		Integer startInSeconds = startHours*3600+startMinutes*60+startSeconds;
		
		Integer durationHours = Integer.parseInt(duration.substring(0,2));
		Integer durationMinutes = Integer.parseInt(duration.substring(3,5));
		Integer durationSeconds = Integer.parseInt(duration.substring(6,8));
		Integer durationInSeconds = durationHours*3600+durationMinutes*60+durationSeconds;
		
		Integer endTimeInSeconds = startInSeconds+durationInSeconds;
		Integer endTimeHours = endTimeInSeconds / 3600;
		Integer remainder = endTimeInSeconds % 3600;
		Integer endTimeMinutes = remainder / 60;
		Integer endTimeSeconds = remainder % 60;
		String endTime = endTimeHours+":"+endTimeMinutes+":"+endTimeSeconds;
	
		sdf = new SimpleDateFormat("HH:mm:ss");
		Date endTime_df= sdf.parse(endTime);
		endTime=sdf.format(endTime_df);
		
		return endTime;
	 }
}

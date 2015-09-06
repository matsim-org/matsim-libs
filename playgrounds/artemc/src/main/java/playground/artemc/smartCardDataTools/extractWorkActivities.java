package playground.artemc.smartCardDataTools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

public class extractWorkActivities {
	
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/PTjourneysDataBase.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
//		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\HomeLocations1.csv"));
		BufferedWriter outWork = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\WorkLocations_ver2.csv"));
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		
		Long CARD_ID=0L;
		String jStartTime = "";
		String jEndTime = "";
		String passenger=""; 
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		Double firstLat=0.0;
		Double firstLon=0.0;
		Integer twoworks=0;
		Integer consistent=0;
		Integer longact=0;
		Integer adult=0;
		
		ResultSet cardIDs = dba.executeQuery("SELECT * FROM CARD_IDs WHERE NumberOfJourneys > 1");
		while(cardIDs.next()){
			CARD_ID = cardIDs.getLong(1);
			ResultSet agentTrips = dba.executeQuery("SELECT CARD_ID,Journey_Start_Time,Journey_End_Time, start_lat,start_lon, end_lat,end_lon,PassengerType FROM trips_ver1 WHERE CARD_ID="+Long.toString(CARD_ID)+" AND PassengerType='Adult' ORDER BY Journey_Start_Date, Journey_Start_Time");					
			int count=0;
			int firstwork=0;
			while(agentTrips.next()){
			 if(count!=0){
				  startLat = agentTrips.getDouble(4);
				  startLon = agentTrips.getDouble(5);
				 Coord coordStart = new Coord(startLon, startLat);
				  Coord UTMStart = ct.transform(coordStart);
				  startLon=UTMStart.getX();
				  startLat=UTMStart.getY();
				  double distance = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
				  long distanceRounded = Math.round(distance);
				  
				  jStartTime = agentTrips.getString(2);	  
				  Date startTime = sdf.parse(jStartTime);
				  Date endTime = sdf.parse(jEndTime);
				  long difference = startTime.getTime() - endTime.getTime();
				  
				  passenger = agentTrips.getString(8);

//				  System.out.println(Long.toString(difference)+"\n");
//				  out.write(Long.toString(difference)+"\n");			
				  if(difference >= 28800000 && difference <= 43200000){
					  longact=longact+1;
					  if (distanceRounded<=1000 & passenger.equals("Adult")){
						  adult=adult+1;
						  firstwork=firstwork+1;
						  consistent=consistent+1;
						  outWork.write(Long.toString(CARD_ID)+","+Double.toString(startLat)+","+Double.toString(startLon)+"\n");  
						  outWork.flush();
					  }
					  if(firstwork==2)
						  twoworks=twoworks+1;
					  
//					  System.out.println(twoworks+","+Long.toString(CARD_ID));
//					  System.out.println(Long.toString(CARD_ID)+","+Double.toString(firstLat)+","+Double.toString(firstLon)+","+Double.toString(startLat)+","+Double.toString(startLon)+"\n");
//					  out.write(Long.toString(CARD_ID)+","+Double.toString(firstLat)+","+Double.toString(firstLon)+"\n");

//					  out.flush();
					 
				  }
				  
			  }
			  else{
				  firstLat = agentTrips.getDouble(4);
				  firstLon = agentTrips.getDouble(5);
				 Coord coordStart = new Coord(firstLon, firstLat);
				  Coord UTMStart = ct.transform(coordStart);
				  firstLon=UTMStart.getX();
				  firstLat=UTMStart.getY();				  
			  }
			  	  
			  jEndTime = agentTrips.getString(3);
			  
			  endLat = agentTrips.getDouble(6);
			  endLon = agentTrips.getDouble(7);
				Coord coordEnd = new Coord(endLon, endLat);
			  Coord UTMEnd = ct.transform(coordEnd);
			  endLon=UTMEnd.getX();
			  endLat=UTMEnd.getY();
			  
			  count=count+1;		  		 
			}
			agentTrips.close(); 	
		}
//		out.close();
		outWork.close();
	
		  System.out.println("Total activities 8-12h: "+longact);
		  System.out.println("Consistent: "+consistent);
		  System.out.println("More then 1 work: "+twoworks);
		  System.out.println("Adults: "+adult);
	}

	
}

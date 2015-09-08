package playground.artemc.smartCardDataTools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

public class activityDurations {
	
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/Ezlink_1week_DataBase.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\ActivityDurations_12042011.csv"));
//		BufferedWriter aggregationErrors = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\aggregationErrors.csv"));
//		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		
		Long CARD_ID=0L;
		String jStartTime = "";
		String jEndTime = "";
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		Double firstLat=0.0;
		Double firstLon=0.0;

		
		ResultSet cardIDs = dba.executeQuery("SELECT * FROM CARD_IDs12042011");
		while(cardIDs.next()){
			CARD_ID = cardIDs.getLong(1);
			ResultSet agentTrips = dba.executeQuery("SELECT CARD_ID,Ride_Start_Time,Ride_Time, start_lat,start_lon, end_lat,end_lon,PassengerType FROM v2_trips12042011 WHERE CARD_ID="+Long.toString(CARD_ID)+" ORDER BY Ride_Start_Date, Ride_Start_Time");			
// AND PassengerType!='Child/Student' 			
			int count=0;
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
				  if(distanceRounded<=1000){
					  jStartTime = agentTrips.getString(2);	  
					  out.write(jEndTime+","+jStartTime+"\n");
					  out.flush();
//					  System.out.println(jEndTime+","+jStartTime+"\n");
				  }
//				  Date startTime = sdf.parse(jStartTime);
//				  Date endTime = sdf.parse(jEndTime);
//				  long difference = startTime.getTime() - endTime.getTime();
//				  System.out.println(Long.toString(difference)+"\n");

//				  out.write(Long.toString(difference)+"\n");			
//				  if(difference >= 28800000 && difference <= 43200000){
//					  System.out.println(CARD_ID);
//					  aggregationErrors.write(Long.toString(CARD_ID)+"\n");
//				  }
			  }
			 else{
				  firstLat = agentTrips.getDouble(4);
				  firstLon = agentTrips.getDouble(5);
				 Coord coordStart = new Coord(firstLon, firstLat);
				  Coord UTMStart = ct.transform(coordStart);
				  firstLon=UTMStart.getX();
				  firstLat=UTMStart.getY();				  
			  }
			  
			  
			  endLat = agentTrips.getDouble(6);
			  endLon = agentTrips.getDouble(7);
				Coord coordEnd = new Coord(endLon, endLat);
			  Coord UTMEnd = ct.transform(coordEnd);
			  endLon=UTMEnd.getX();
			  endLat=UTMEnd.getY();
			  
			  endTimeCalculator c = new endTimeCalculator();
			  jEndTime = c.calculateEndTime(agentTrips.getString(2),agentTrips.getDouble(3));
			  count=count+1;
			  		 
			}
			agentTrips.close(); 	
		}
		out.close();
//		aggregationErrors.close();
	}
	
}

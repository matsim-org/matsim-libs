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
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

public class distancesBetweenTrips_1week {
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/Ezlink_1week_DataBase.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\DistancesBetweenTrips12042011.csv"));
		BufferedWriter outFirstLast = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\DistancesBetweenFirstLastActivity12042011.csv"));
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		
		Long CARD_ID=0L;
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		Double firstLat=0.0;
		Double firstLon=0.0;
		
		String rStartTime = "";
		String rEndTime = "";
		
		
//		ArrayList<Double> allDistances = new ArrayList<Double>();
		
		ResultSet cardIDs = dba.executeQuery("SELECT * FROM CARD_IDs12042011");
		while(cardIDs.next()){
			CARD_ID = cardIDs.getLong(1);
			ResultSet agentTrips = dba.executeQuery("SELECT Ride_Start_Time,Ride_Time, start_lat,start_lon, end_lat,end_lon FROM v1_trips12042011 WHERE CARD_ID="+Long.toString(CARD_ID)+" ORDER BY Ride_Start_Date,Ride_Start_Time");
			
			
			int trips=0;
			while(agentTrips.next()){
			  if(trips!=0){
				  startLat = agentTrips.getDouble(3);
				  startLon = agentTrips.getDouble(4);
				  rStartTime=agentTrips.getString(1);
				  Coord coordStart =new CoordImpl(startLon, startLat);
				  Coord UTMStart = ct.transform(coordStart);
				  startLon=UTMStart.getX();
				  startLat=UTMStart.getY();
				  
				  Date startTime = sdf.parse(rStartTime);
				  Date endTime = sdf.parse(rEndTime);
				  
				  long difference = startTime.getTime() - endTime.getTime();
				  if(difference >= 1800000 && endLat!=0){
					  double distance = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
					  long distanceRounded = Math.round(distance);
//				  System.out.println(Math.round(distance));
					  out.write(Long.toString(distanceRounded) +"\n");
				  }
//				  Double oDistance = new Double(distance);
//				  allDistances.add(oDistance);
			  }
			  else{
				  firstLat = agentTrips.getDouble(3);
				  firstLon = agentTrips.getDouble(4);
				  Coord coordStart =new CoordImpl(firstLon, firstLat);
				  Coord UTMStart = ct.transform(coordStart);
				  firstLon=UTMStart.getX();
				  firstLat=UTMStart.getY();				  
			  }
			  
			  
			  endTimeCalculator c = new endTimeCalculator();
			  rEndTime = c.calculateEndTime(agentTrips.getString(1),agentTrips.getDouble(2));
			  
			  endLat = agentTrips.getDouble(5);
			  endLon = agentTrips.getDouble(6);			  
			  Coord coordEnd =new CoordImpl(endLon, endLat);
			  Coord UTMEnd = ct.transform(coordEnd);
			  endLon=UTMEnd.getX();
			  endLat=UTMEnd.getY();
			  
			  

			  trips=trips+1;
			}
			double distanceFirstLast = Math.sqrt((firstLat - endLat)*(firstLat - endLat) + (firstLon - endLon)*(firstLon - endLon));
			long distanceFirstLastRounded = Math.round(distanceFirstLast);
			outFirstLast.write(Long.toString(distanceFirstLastRounded) +"\n");
//			System.out.println(Math.round(distanceFirstLast)+","+CARD_ID);
			agentTrips.close(); 
		}
//		System.out.println(allDistances.size());
		out.close();
		outFirstLast.close();		
		
	}
	
	
	

}

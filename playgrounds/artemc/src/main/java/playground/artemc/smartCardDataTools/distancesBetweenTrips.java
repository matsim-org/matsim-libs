package playground.artemc.smartCardDataTools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

public class distancesBetweenTrips {
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/PTjourneysDataBase.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
//		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\DistancesBetweenTrips1.csv"));
		BufferedWriter outFirstLast = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\DistancesBetweenFirstLastActivity1.csv"));
		
		Long CARD_ID=0L;
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		Double firstLat=0.0;
		Double firstLon=0.0;
		
		
//		ArrayList<Double> allDistances = new ArrayList<Double>();
		
		ResultSet cardIDs = dba.executeQuery("SELECT * FROM CARD_IDs");
		while(cardIDs.next()){
			CARD_ID = cardIDs.getLong(1);
			ResultSet agentTrips = dba.executeQuery("SELECT CARD_ID,Journey_Start_Time, start_lat,start_lon, end_lat,end_lon FROM trips_ver1 WHERE CARD_ID="+Long.toString(CARD_ID)+" ORDER BY Journey_Start_Date,Journey_Start_Time");
			
			int trips=0;
			while(agentTrips.next()){
			  if(trips!=0){
				  startLat = agentTrips.getDouble(3);
				  startLon = agentTrips.getDouble(4);
				  Coord coordStart = new Coord(startLon, startLat);
				  Coord UTMStart = ct.transform(coordStart);
				  startLon=UTMStart.getX();
				  startLat=UTMStart.getY();
				  double distance = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
				  long distanceRounded = Math.round(distance);
//				  System.out.println(Math.round(distance));
//				  out.write(Long.toString(distanceRounded) +"\n");
					
//				  Double oDistance = new Double(distance);
//				  allDistances.add(oDistance);
			  }
			  else{
				  firstLat = agentTrips.getDouble(3);
				  firstLon = agentTrips.getDouble(4);
				  Coord coordStart = new Coord(firstLon, firstLat);
				  Coord UTMStart = ct.transform(coordStart);
				  firstLon=UTMStart.getX();
				  firstLat=UTMStart.getY();				  
			  }
			  
			  
			  endLat = agentTrips.getDouble(5);
			  endLon = agentTrips.getDouble(6);
				Coord coordEnd = new Coord(endLon, endLat);
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
//		out.close();
		outFirstLast.close();		
		
	}
	
	
	

}

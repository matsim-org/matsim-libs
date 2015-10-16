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


public class oneWayJourneys {
	
	
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/ArtemDataBase.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\OneWayTrips.csv"));
		
		Long CARD_ID=0L;
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		int trips=1;

		

		
		ResultSet cardIDs = dba.executeQuery("SELECT * FROM CARD_IDs WHERE NumberOfJourneys = 1 LIMIT 1000");
		
		while(cardIDs.next()){
			CARD_ID = cardIDs.getLong(1);
			ResultSet agentTrips = dba.executeQuery("SELECT CARD_ID,Journey_Start_Time, start_lat,start_lon, end_lat,end_lon FROM trips_ver1 WHERE CARD_ID="+Long.toString(CARD_ID)+" ORDER BY Journey_Start_Time");
			
			while(agentTrips.next()){
			
				  startLat = agentTrips.getDouble(3);
				  startLon = agentTrips.getDouble(4);
				Coord coordStart = new Coord(startLon, startLat);
				  Coord UTMStart = ct.transform(coordStart);
				  startLon=UTMStart.getX();
				  startLat=UTMStart.getY();
				
				  endLat = agentTrips.getDouble(5);
				  endLon = agentTrips.getDouble(6);
				Coord coordEnd = new Coord(endLon, endLat);
				  Coord UTMEnd = ct.transform(coordEnd);
				  endLon=UTMEnd.getX();
				  endLat=UTMEnd.getY();
				  
				  String jStartTime = agentTrips.getString(2);
				  
				  double distance = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
				  long distanceRounded = Math.round(distance);

				  out.write(Long.toString(distanceRounded)+","+jStartTime+"\n");
//				  System.out.println(Long.toString(distanceRounded)+","+jStartTime+"\n");
				  trips = trips +1;
			  
			}

		}
		
		out.close();

		ResultSet oneWayJourneys = dba.executeQuery("SELECT COUNT(*) FROM CARD_IDs WHERE NumberOfJourneys = 1");
		while(oneWayJourneys.next()){
			Long oneWayJourneysNumber = oneWayJourneys.getLong(1);
			System.out.println(Long.toString(oneWayJourneysNumber));
		}
		
		
		
	}
	
	

}

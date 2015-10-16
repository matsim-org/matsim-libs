package playground.artemc.hits;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class unreportedTrips {
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/LocalDataBase.properties"));
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_UTM48N");
		BufferedWriter outToFile = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\HITSDistancesBetweenTrips.csv"));
//		BufferedWriter outModes = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\HITSModes.csv"));
		
		String household="";
		String pax="";
		String mode="";
		Double startLat=0.0;
		Double startLon=0.0;
		Double endLat=0.0;
		Double endLon=0.0;
		Double firstLat=0.0;
		Double firstLon=0.0;
		int bus=0;
		int walk=0;
		int mrtlrt=0;
		int compBus=0;
		int taxi=0;
		int pt=0;
		int other=0;
		int pt_trips=0;
		int ptwalk_trips=0;
		int allmode_trips=0;
		int ptothers_trips=0;
		int nopt_trips=0;
		int numberOfStages=0;
		int totalTrips=0;
		int firstTripNoPt=0;
		int lastTripNoPt=0;
		int numberOfPersons=0;

		ResultSet persons = dba.executeQuery("SELECT * FROM hits_persons_using_pt LIMIT 7000");
	
		while(persons.next()){
			
			household = persons.getString(1);
			pax = persons.getString(2);
			ResultSet agentTrips = dba.executeQuery("SELECT h1_hhid,pax_id,trip_id,stage_id,t10_mode,start_lat,start_lon, end_lat,end_lon FROM hits_journeys_with_pt WHERE h1_hhid='"+household+"' AND pax_id='"+pax+"' ORDER BY trip_id,stage_id");
			int trips=1;
			numberOfPersons++;
			System.out.println(numberOfPersons);
			
			while(agentTrips.next()){
				
				int tripID = agentTrips.getInt(3);			
				
				if(trips!=tripID){		
					startLat = agentTrips.getDouble(6);
					startLon = agentTrips.getDouble(7);
					Coord coordStart = new Coord(startLon, startLat);
					Coord UTMStart = ct.transform(coordStart);
					startLon=UTMStart.getX();
					startLat=UTMStart.getY();
					double distanceTrips = Math.sqrt((startLat - endLat)*(startLat - endLat) + (startLon - endLon)*(startLon - endLon));
					long distanceTripsRounded = Math.round(distanceTrips);
					if(distanceTrips >500)
						System.out.println(distanceTripsRounded+","+household+","+pax+","+tripID);
				//		agent_unknown=agent_unknown+1;
						
						
					
					
//					Remove this people from hits_persons_using_pt
//					outToFile.write(Long.toString(distanceRounded)+","+agentTrips.getString(1)+","+agentTrips.getString(2)+"\n");
					
					trips=tripID;	
					totalTrips=totalTrips+1;	  
				}
				else{
				}
			  
				endLat = agentTrips.getDouble(8);
				endLon = agentTrips.getDouble(9);
				Coord coordEnd = new Coord(endLon, endLat);
				Coord UTMEnd = ct.transform(coordEnd);
				endLon=UTMEnd.getX();
				endLat=UTMEnd.getY();
				mode=agentTrips.getString(5);
//			  	System.out.println(mode);

			
				numberOfStages=numberOfStages + 1;
			}
		    
			totalTrips=totalTrips+1;
	
		    
//			double distanceFirstLast = Math.sqrt((firstLat - endLat)*(firstLat - endLat) + (firstLon - endLon)*(firstLon - endLon));
//			long distanceFirstLastRounded = Math.round(distanceFirstLast);
//			outFirstLast.write(Long.toString(distanceFirstLastRounded) +"\n");
//			System.out.println(Math.round(distanceFirstLast));
			agentTrips.close(); 
		}

		System.out.println("===============================");
		System.out.println("Total Trips: "+totalTrips);
		System.out.println();
		System.out.println("First Trip is without PT: "+firstTripNoPt);
		System.out.println("Last Trip is without PT: "+lastTripNoPt);
		
		
		
//		System.out.println(allDistances.size());
		outToFile.close();
//		outFirstLast.close();		
		
	}
	
	
	

}

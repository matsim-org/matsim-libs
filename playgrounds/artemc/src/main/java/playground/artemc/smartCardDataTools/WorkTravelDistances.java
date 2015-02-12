package playground.artemc.smartCardDataTools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

public class WorkTravelDistances {

	/**
	 * @param args
	 * @throws java.sql.SQLException
	 * @throws java.io.IOException
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoConnectionException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		DataBaseAdmin dba_ezlink = new DataBaseAdmin(new File("./data/dataBases/Ezlink_1week_DataBase_local.properties"));
		DataBaseAdmin dba_workfacilites = new DataBaseAdmin(new File("./data/dataBases/work_facilities_aux_DataBase.properties"));		
		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\DCM_DistancesToWorkTrips.csv"));
		
		ResultSet workJourneys = dba_workfacilites.executeQuery("SELECT journeyID, workLat, workLon, card_id FROM DCM_work_activities ORDER BY RAND()");
		
		Double workLat = 0.0;
		Double workLon = 0.0;
		Double homeLat = 0.0; 
		Double homeLon = 0.0;
		String jID = "";
		String journeyID = "";
		Integer transfer=0;
		String originStop = "Unknown";
		
		if (workJourneys.last()) {
			System.out.println(workJourneys.getRow());
			workJourneys.beforeFirst(); 
		}
		
		
		while(workJourneys.next()){
			
			workLat = workJourneys.getDouble(2);
			workLon = workJourneys.getDouble(3);
			jID = workJourneys.getString(1);
			
	//		System.out.println("WorkCoordinates: "+workLat+","+workLon);
		
			ResultSet journeys = dba_ezlink.executeQuery("SELECT JOURNEY_ID, Transfer_Number, BOARDING_STOP_STN FROM v1_trips12042011 WHERE CARD_ID="+workJourneys.getString(4)+" ORDER BY Ride_Start_Time");
		//	ResultSet person = dba_ezlink.executeQuery("SELECT JOURNEY_ID, Transfer_Number, BOARDING_STOP_STN FROM v1_trips12042011 WHERE JOURNEY_ID="+jID+" ORDER BY Ride_Start_Time");
		
			
			originStop = "Unknown";
			while(journeys.next()){
				journeyID = journeys.getString(1);
				transfer = journeys.getInt(2);
			//	System.out.println(journeyID+","+transfer);
				if(journeyID.equals(jID)){
					originStop = journeys.getString(3);					
				}
			}
			if(!originStop.equals("Unknown")){
				ResultSet originCoordinates = dba_workfacilites.executeQuery("SELECT lat, lon FROM stops WHERE stop_id='"+originStop+"'");
		//		System.out.println(journeyID+","+originStop);
				homeLat=0.0;
				while(originCoordinates.next()){
					homeLat = originCoordinates.getDouble(1);
					homeLon = originCoordinates.getDouble(2);
					Double beelineDistance = Math.sqrt((homeLat - workLat)*(homeLat - workLat) + (homeLon - workLon)*(homeLon - workLon));
				//	System.out.println(beelineDistance);
					out.write(beelineDistance+"\n");
				}
				if(homeLat==0.0){
					System.out.println("No stop in the databse: "+originStop);
				}
			}
			else{
				System.out.println(workJourneys.getString(4)+","+jID+","+originStop+","+transfer);
			}			
		}		
		out.close();

	}
}

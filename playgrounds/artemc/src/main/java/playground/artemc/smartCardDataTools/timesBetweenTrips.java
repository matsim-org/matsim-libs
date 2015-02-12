package playground.artemc.smartCardDataTools;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class timesBetweenTrips {
	
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/LocalDataBase.properties"));
		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\TimesBetweenTrips_test.csv"));
//		BufferedWriter aggregationErrors = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\aggregationErrors.csv"));
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		
		Long CARD_ID=0L;
		String jStartTime = "";
		String jEndTime = "";

		
		ResultSet cardIDs = dba.executeQuery("SELECT * FROM CARD_IDs WHERE NumberOfJourneys > 1 LIMIT 20");
		while(cardIDs.next()){
			CARD_ID = cardIDs.getLong(1);
			ResultSet agentTrips = dba.executeQuery("SELECT CARD_ID,Journey_Start_Time,Journey_End_Time, start_lat,start_lon, end_lat,end_lon FROM trips_ver1 WHERE CARD_ID="+Long.toString(CARD_ID)+" ORDER BY Journey_Start_Time");			
// AND PassengerType!='Child/Student' 			
			int count=0;
			while(agentTrips.next()){
			 if(count!=0){
				  jStartTime = agentTrips.getString(2);	  
				  out.write(jEndTime+","+jStartTime+"\n");
				  out.flush();
				  System.out.println(jEndTime+","+jStartTime+"\n");
				  
				  Date startTime = sdf.parse(jStartTime);
				  Date endTime = sdf.parse(jEndTime);
				  long difference = startTime.getTime() - endTime.getTime();
				  System.out.println(Long.toString(difference)+"\n");
//				  out.write(Long.toString(difference)+"\n");			
				  if(difference >= 28800000 && difference <= 43200000){
					  System.out.println(CARD_ID);
//					  aggregationErrors.write(Long.toString(CARD_ID)+"\n");
				  }
			  }
			  	  
			  jEndTime = agentTrips.getString(3);
			  count=count+1;
			  		 
			}
			agentTrips.close(); 	
		}
		out.close();
//		aggregationErrors.close();
	}
	
}

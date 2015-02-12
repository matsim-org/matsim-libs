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


public class BusBunching95 {
	
	public static void main(String args[]) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/LocalDataBase.properties"));
		BufferedWriter out = new BufferedWriter( new FileWriter("C:\\Work\\Workspace\\MatsimProjects\\data\\ezLinkAnalysis\\Bus197at18071_22022011a.csv"));

		String jStartTime = "";

		ResultSet agentTrips = dba.executeQuery("SELECT CARD_ID,Ride_Start_Time FROM trips22022011 WHERE ALIGHTING_STOP_STN ='18071' AND Srvc_Number=197 ORDER BY Ride_Start_Time");			
			while(agentTrips.next()){

				  jStartTime = agentTrips.getString(2);	  
				  out.write(jStartTime+"\n");
				  out.flush();
//				  System.out.println(jEndTime+","+jStartTime+"\n");		  		 
			}
			agentTrips.close(); 	
		out.close();
	}

}

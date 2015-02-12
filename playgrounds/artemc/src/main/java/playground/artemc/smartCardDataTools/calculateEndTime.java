package playground.artemc.smartCardDataTools;

import playground.artemc.utils.DataBaseAdmin;
import playground.artemc.utils.NoConnectionException;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class calculateEndTime {
	
	private static SimpleDateFormat sdf;

	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException, ParseException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("./data/dataBases/Ezlink_1week_DataBase.properties"));
			
		String StartTime="";
		Float duration=0f;
		String durationFull = "";
		String Endtime = "";


		Long CARD_ID=9000147172420800L;

		ResultSet rs = dba.executeQuery("SELECT CARD_ID,Ride_Start_Time, Ride_Time FROM v1_trips12042011 WHERE CARD_ID="+Long.toString(CARD_ID));
		while(rs.next()) {
			CARD_ID = rs.getLong(1);
			StartTime = rs.getString(2);
			duration = rs.getFloat(3);
			
			Integer startHours = Integer.parseInt(StartTime.substring(0,2));
			Integer startMinutes = Integer.parseInt(StartTime.substring(3,5));
			Integer startSeconds = Integer.parseInt(StartTime.substring(6,8));
			Integer startInSeconds = startHours*3600+startMinutes*60+startSeconds;
			
			Integer durationInSeconds = Math.round(duration * 60);
			Integer durationHours = durationInSeconds / 3600;
			Integer remainder = durationInSeconds % 3600;
			Integer durationMinutes = remainder / 60;
			Integer durationSeconds = remainder % 60;
			
			
			Integer EndtimeInSeconds = startInSeconds+durationInSeconds;
			Integer EndtimeHours = EndtimeInSeconds / 3600;
			remainder = EndtimeInSeconds % 3600;
			Integer EndtimeMinutes = remainder / 60;
			Integer EndtimeSeconds = remainder % 60;
			durationFull = durationHours+":"+durationMinutes+":"+durationSeconds;
			Endtime = EndtimeHours+":"+EndtimeMinutes+":"+EndtimeSeconds;
			
			
			sdf = new SimpleDateFormat("HH:mm:ss");
			Date durationFull_df = sdf.parse(durationFull);
			Date Endtime_df= sdf.parse(Endtime);
			durationFull = sdf.format(durationFull_df);
			Endtime=sdf.format(Endtime_df);
			
			 
		    System.out.println(StartTime+", "+duration+", "+durationFull+", "+Endtime);
		}
	    dba.close();
	    
	}

}

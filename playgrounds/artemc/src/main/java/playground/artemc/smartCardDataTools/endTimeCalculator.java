package playground.artemc.smartCardDataTools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class endTimeCalculator {
	
	private static SimpleDateFormat sdf;
	
	public String calculateEndTime(String startTime, double duration) throws ParseException
	{
		Integer startHours = Integer.parseInt(startTime.substring(0,2));
		Integer startMinutes = Integer.parseInt(startTime.substring(3,5));
		Integer startSeconds = Integer.parseInt(startTime.substring(6,8));
		Integer startInSeconds = startHours*3600+startMinutes*60+startSeconds;
		
		Integer durationInSeconds = (int) Math.round(duration * 60);
		Integer durationHours = durationInSeconds / 3600;
		Integer remainder = durationInSeconds % 3600;
		Integer durationMinutes = remainder / 60;
		Integer durationSeconds = remainder % 60;
		
		
		Integer endtimeInSeconds = startInSeconds+durationInSeconds;
		Integer endtimeHours = endtimeInSeconds / 3600;
		remainder = endtimeInSeconds % 3600;
		Integer endtimeMinutes = remainder / 60;
		Integer endtimeSeconds = remainder % 60;
		String durationFull = durationHours+":"+durationMinutes+":"+durationSeconds;
		String endtime = endtimeHours+":"+endtimeMinutes+":"+endtimeSeconds;
		
		sdf = new SimpleDateFormat("HH:mm:ss");
		Date durationFull_df = sdf.parse(durationFull);
		Date endtime_df= sdf.parse(endtime);
		durationFull = sdf.format(durationFull_df);
		endtime=sdf.format(endtime_df);
		
		return endtime;		
	}

}

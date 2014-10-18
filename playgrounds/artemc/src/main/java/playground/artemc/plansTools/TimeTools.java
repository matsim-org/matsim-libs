package playground.artemc.plansTools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeTools{

	/**
	 * @param args
	 * @throws ParseException 
	 * 
	 */
	
	public static void main(String[] args) throws ParseException{
		System.out.println(secondsTotimeString(3721.00));
		System.out.println(timeStringToSeconds("01:01:59"));
		
	}
	
	public static Double timeStringToSeconds(String time){
		
		Integer timeHours = Integer.parseInt(time.substring(0,2));
		Integer timeMinutes = Integer.parseInt(time.substring(3,5));
		Integer timeSeconds = Integer.parseInt(time.substring(6,8));
		Double timeInSeconds = (double) (timeHours*3600+timeMinutes*60+timeSeconds);
		
		return timeInSeconds;

	}

	public static String secondsTotimeString(Double time) throws ParseException{
		
		Integer timeHours = (int) (time / 3600);
		Integer remainder = (int) (time % 3600);
		Integer timeMinutes = remainder / 60;
		Integer timeSeconds = remainder % 60;
		String timeString = timeHours+":"+timeMinutes+":"+timeSeconds;
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		Date time_df= sdf.parse(timeString);
		timeString=sdf.format(time_df);
		
		return timeString;

	}
}

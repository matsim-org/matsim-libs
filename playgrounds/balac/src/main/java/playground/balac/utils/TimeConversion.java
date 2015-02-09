package playground.balac.utils;

public class TimeConversion {	
	
	public static String convertDoubleToTime(Double timeInMinutes) {
		
		int hours = (int) (timeInMinutes / 60);
		int min = (int) (timeInMinutes - hours * 60);
		String s;
		if (hours < 10)
			s = "0" + hours;
		else 
			s = Integer.toString(hours);
		if (min < 10)				
			s +=  ":0" + String.valueOf(min);
		else
			s += ":" + String.valueOf(min);
		
		return s;
	}
	
	public static double convertTimeToDouble(String time) {
		
		String[] arr = time.split(":");
		
		double h = Double.parseDouble(arr[0]);
		
		double m = Double.parseDouble(arr[1]);
		
		
		return h * 60 + m;
		
	}

}

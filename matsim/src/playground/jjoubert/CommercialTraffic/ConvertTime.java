package playground.jjoubert.CommercialTraffic;


public class ConvertTime {
	final static int DAY = 86400;
	final static int HOUR = 3600;
	final static int MIN = 60;

	public static void main(String args[]) {
		
		long dateStart = 0;
		long dateEnd = 71;
		long duration = dateEnd - dateStart;
		
		int DHMS[] = convertSecToDHMS(duration);
		printDHMS(DHMS);
	}

	private static int[] convertSecToDHMS(long durationS) {
		float daysF = durationS/DAY;
		int days = (int) daysF;
		float hoursF = (durationS - (days*DAY) )/HOUR;
		int hours = (int) hoursF;
		float minutesF = (durationS - (days*DAY) - (hours*HOUR) )/MIN;
		int minutes = (int) minutesF;
		int seconds = (int) (durationS  - (days*DAY) - (hours*HOUR)  - (minutes*MIN));
		int newDuration[] = {days, hours, minutes, seconds};
		return newDuration;
	}
	
	public static void printDHMS(int[] time){
		String days, hours, minutes, seconds;
		if(time[0] != 0){
			if(time[0] == 1 ){
				days = time[0] + " day; ";
			} else{
				days = time[0] + " days; ";
			}
		} else{
			days = "";
		}
		if( (time[1] != 0) || (days.length() > 0) ){
			if( time[1] == 1 ){
				hours = time[1] + " hour; ";
			} else{
				hours = time[1] + " hours; ";
			}
		} else{
			hours = "";
		}
		if( (time[2] != 0) || (hours.length() > 0) ){
			if(time[2] == 1){
				minutes = time[2] + " minute; ";
			} else{
				minutes = time[2] + " minutes; ";
			}
		} else{
			minutes = "";
		}
		if( time[3] == 1 ){
			seconds = time[3] + " second.";
		} else{
			seconds = time[3] + " seconds.";
		}
		
		System.out.println(days + hours + minutes + seconds);
	}
}

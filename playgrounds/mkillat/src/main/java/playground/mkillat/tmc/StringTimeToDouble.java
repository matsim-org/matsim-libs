package playground.mkillat.tmc;

public class StringTimeToDouble {
	
	public double transformer (String time){
		double output=0;
		String[] timeArray = time.split(":");
		double hours = Double.parseDouble(timeArray[0]);
		double minutes = Double.parseDouble(timeArray[1]);
		double seconds = Double.parseDouble(timeArray[2]);
		output = (3600 * hours) + (60 * minutes) + seconds;
		
		return output;
	}

}
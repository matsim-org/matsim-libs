package playground.toronto.transitnetworkutils;

import java.io.IOException;
import java.util.ArrayList;

import org.matsim.core.utils.collections.Tuple;

/**
 * A simple data structure to represent generalized scheduled transit stops. The ArrayList 'times'
 * stores arrival/departure Tuples. The times are stored as strings which can later be parsed into
 * actual hours/minutes.
 * 
 * @author pkucirek
 *
 */

public class ScheduledStop {
	
	private String id;
	private ArrayList<Tuple<Double, Double>> times;
	
	public boolean isVIA;

	public ScheduledStop(String id){
		this.id = id;
		this.times = new ArrayList<Tuple<Double,Double>>();
		this.isVIA = false;
	}
	
	public String getId(){
		return this.id;
	}
	
	public double getAvgDepTime() throws IOException{
		double arrSum = 0;
		
		for (Tuple<Double, Double> T : this.times)
			arrSum += T.getFirst();
		
		
		return arrSum / this.times.size();
	}
	
	public double getAvgArrTime() throws IOException{
		double depSum = 0;
		
		for (Tuple<Double, Double> T : this.times)
			depSum += T.getSecond();
		
		return depSum / this.times.size();
	}
	
	public ArrayList<Tuple<Double, Double>> getTimes(){
		return this.times;
	}
	
	public void setTime(int index, Tuple<Double,Double> override){
		this.times.set(index, override);
	}
	
	/**
	 * Parses and adds the corresponding arrival/departure pair to the list of stop-times.
	 * 
	 * @param arrivalDepartureTime
	 * @throws IOException
	 */
	public void AddTime(Tuple<String,String> arrivalDepartureTime) throws IOException{
		Double first;
		Double second;
		if (arrivalDepartureTime.getFirst().equals("") || arrivalDepartureTime.getFirst() == null){
			second = parseTime(arrivalDepartureTime.getSecond());
			first = second;
		}else if (arrivalDepartureTime.getSecond().equals("") || arrivalDepartureTime.getSecond() == null){
			first = parseTime(arrivalDepartureTime.getFirst());
			second = first;
		}else{
			first = parseTime(arrivalDepartureTime.getFirst());
			second = parseTime(arrivalDepartureTime.getSecond());
		}
		
		Tuple<Double,Double> parsedTimes = new Tuple<Double, Double>(first, second);
		this.times.add(parsedTimes);
	}
	
	/**
	 * Parses the 4-character time representation hhmm, padding with leading zeroes
	 * 
	 * @param s - the time string to parse
	 * @return
	 * @throws IOException
	 */
	public static double parseTime(String s) throws NumberFormatException{
		if (s.length() > 4) throw new NumberFormatException("Could not parse time: \"" + s + "\"!");
		
		double result = 0;
		
		char[] c = s.toCharArray();
		char[] x = new char[]{'0','0','0','0'};
		for (int i = 0; i < c.length; i++) x[i + 4 - c.length] = c[i];
		
		try {
			int hours = Integer.parseInt("" + x[0] + x[1]);
			int minutes = Integer.parseInt("" + x[2] + x[3]);
			result = hours * 3600 + minutes * 60;
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Could not parse time: \"" + s + "\"!");
		}
		
		//Carry time over to next day for any time earlier tahn 0300h
		if (result < (3*3600)) result += (24*3600);
		
		return result;
	}
}

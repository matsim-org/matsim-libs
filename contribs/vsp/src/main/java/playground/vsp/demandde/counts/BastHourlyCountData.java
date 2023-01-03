/**
 * 
 */
package playground.vsp.demandde.counts;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tilmann Schlenther
 */
public class BastHourlyCountData{
	
	private String id;
	private Day representedDay;
	
	private Map<Integer,Integer> R1Divisors;
	private Map<Integer,Integer> R2Divisors;
	private HashMap<Integer,Double> R1VolumesPerHour;
	private HashMap<Integer,Double> R2VolumesPerHour;

	
	public BastHourlyCountData(String id, Day representedDay){
		this.id = id;
		this.representedDay = representedDay;
		this.R1VolumesPerHour = new HashMap<Integer, Double>();
		this.R2VolumesPerHour = new HashMap<Integer, Double>();
		this.R1Divisors = new HashMap<Integer,Integer>();
		this.R2Divisors = new HashMap<Integer,Integer>();
	}
	
	public Map<Integer,Double> getR1Values(){
		return this.R1VolumesPerHour;
	}
	
	public Map<Integer,Double> getR2Values(){
		return this.R2VolumesPerHour;
	}
	

	public String getId() {
		return this.id;
	}

	public Day getRepresentedDay() {
		return representedDay;
	}
	
	public void computeAndSetVolume(boolean direction1, int hour, double value){
		if(direction1){
			Integer divisor = this.R1Divisors.get(hour); 
			if(divisor != null){
				Double volume = this.R1VolumesPerHour.get(hour); 
				volume = ( ((volume * divisor) + value)  / (divisor+1) );
				divisor ++;
				this.R1Divisors.put(hour, divisor);
				this.R1VolumesPerHour.put(hour,volume);
			}
			else{
				this.R1Divisors.put(hour, 1);
				this.R1VolumesPerHour.put(hour, value);
			}
		}
		else{
			Integer divisor = this.R2Divisors.get(hour); 
			if(divisor != null){
				Double volume = this.R2VolumesPerHour.get(hour); 
				volume = ( ((volume*divisor) + value)  / (divisor+1) );
				divisor ++;
				this.R2Divisors.put(hour, divisor);
				this.R2VolumesPerHour.put(hour,volume);
			}
			else{
				this.R2Divisors.put(hour, 1);
				this.R2VolumesPerHour.put(hour, value);
			}
		}
	}
	
	
	protected enum Day{
		WEEKDAY, WEEKEND
	}
	
	@Override
	public String toString(){
		String str = "Zählstelle:\t" + this.id + "\n - RICHTUNG 1 - : \n";
		for(Integer hour : this.R1VolumesPerHour.keySet()){
			str += "Std " + hour + "\t" + this.R1VolumesPerHour.get(hour) + "\n";
		}
		str += "\n -RICHTUNG 2 : \n";
		for(Integer hour : this.R2VolumesPerHour.keySet()){
			str += "Std " + hour + "\t" + this.R2VolumesPerHour.get(hour) + "\n";
		}
		return str;
	}
	
	
}

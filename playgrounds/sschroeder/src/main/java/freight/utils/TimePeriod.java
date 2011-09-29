package freight.utils;


public class TimePeriod {
	
	public double start;
	public double end;
	
	public TimePeriod(double start, double end) {
		super();
		this.start = start;
		this.end = end;
	}
	
	public boolean isWithin(double time){
		if(time >= start && time < end){
			return true;
		}
		return false;
	}
	
}
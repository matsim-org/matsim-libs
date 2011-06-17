package vrp.basics;

/**
 * 
 * @author stefan schroeder
 *
 */

public class TimeWindow {
	private double start;
	private double end;
	public TimeWindow(double start, double end) {
		super();
		this.start = start;
		this.end = end;
	}
	public double getStart() {
		return start;
	}
	
	public void setStart(double start) {
		this.start = start;
	}
	
	public void setEnd(double end) {
		this.end = end;
	}
	
	public double getEnd() {
		return end;
	}
	
	public boolean conflict(){
		if(start > end){
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "[start="+start+"][end="+end+"]";
	}
	
}
package freight.utils;

import java.util.ArrayList;
import java.util.List;


public class TimePeriods{

	public List<TimePeriod> getPeriods() {
		return periods;
	}

	private List<TimePeriod> periods = new ArrayList<TimePeriod>();
	
	public void addTimePeriod(TimePeriod timePeriod){
		periods.add(timePeriod);
	}
	
	public TimePeriod getPeriod(double time) {
		for(TimePeriod period : periods){
			if(period.start <= time && period.end > time){
				return period;
			}
		}
		return null;
	}

	public TimePeriod getPeriod(double start, double end) {
		for(TimePeriod period : periods){
			if(period.start <= start && period.end > start && period.start <= end && period.end > end){
				return period;
			}
		}
		return null;
	}
}
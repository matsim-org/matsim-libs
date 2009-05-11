package playground.gregor.sims.evacbase;

import org.matsim.core.api.population.Activity;

public class StaticEvacuationStartTimeCalculator implements EvacuationStartTimeCalculator {

	private final double endTime;
	
	public StaticEvacuationStartTimeCalculator(double endTime) {
		this.endTime = endTime; 
	}

	public double getEvacuationStartTime(Activity act) {
		return this.endTime;
	}


}

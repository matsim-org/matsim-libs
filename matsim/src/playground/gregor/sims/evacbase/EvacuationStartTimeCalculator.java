package playground.gregor.sims.evacbase;

import org.matsim.core.api.population.Activity;

public interface EvacuationStartTimeCalculator {

	public double getEvacuationStartTime(Activity act);
}

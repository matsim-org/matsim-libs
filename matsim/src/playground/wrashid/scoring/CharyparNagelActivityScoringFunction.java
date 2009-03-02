package playground.wrashid.scoring;

import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Plan;

import playground.wrashid.scoring.interfaces.ActivityScoringFunction;
import playground.wrashid.scoring.interfaces.BasicScoringFunction;

public class CharyparNagelActivityScoringFunction implements ActivityScoringFunction, BasicScoringFunction {

	private Plan plan;

	public void endActivity(double time) {
		// TODO Auto-generated method stub
		
	}

	public void startActivity(double time, Act act) {
		// TODO Auto-generated method stub
		
	}

	public void finish() {
		// TODO Auto-generated method stub
		
	}

	public double getScore() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void reset() {
		// TODO Auto-generated method stub
		
	}

	public CharyparNagelActivityScoringFunction(Plan plan) {
		this.plan=plan;
	}

}

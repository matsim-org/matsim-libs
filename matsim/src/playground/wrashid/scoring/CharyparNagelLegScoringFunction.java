package playground.wrashid.scoring;

import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Plan;

import playground.wrashid.scoring.interfaces.BasicScoringFunction;
import playground.wrashid.scoring.interfaces.LegScoringFunction;

public class CharyparNagelLegScoringFunction implements LegScoringFunction, BasicScoringFunction {

	private Plan plan;

	public void endLeg(double time) {
		// TODO Auto-generated method stub
		
	}

	public void startLeg(double time, Leg leg) {
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
	
	public CharyparNagelLegScoringFunction(Plan plan) {
		this.plan=plan;
	}

}

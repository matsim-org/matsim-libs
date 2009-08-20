package playground.ciarif.retailers;

import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.scoring.ActivityScoringFunction;
import playground.meisterk.kti.scoring.LegScoringFunction;


public class RetailersScoringFunctionFactory extends org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory {
	
	private final TreeMap<Id, FacilityPenalty> facilityPenalties;
	
	private ActivityScoringFunction activities = null;
	
	public RetailersScoringFunctionFactory(
			CharyparNagelScoringConfigGroup config, 
			final TreeMap<Id, FacilityPenalty> facilityPenalties) {
		super(config);
		this.facilityPenalties = facilityPenalties;
	}
	
	public ScoringFunction getNewScoringFunction(PlanImpl plan) {
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		this.activities = new ActivityScoringFunction(plan, super.getParams(), this.facilityPenalties);
		scoringFunctionAccumulator.addScoringFunction(this.activities);
		//scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(plan, super.getParams(), this.ktiConfigGroup));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.MoneyScoringFunction(super.getParams()));
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction(super.getParams()));
		
		return scoringFunctionAccumulator;
		
	}

	public ActivityScoringFunction getActivities() {
		return activities;
	}

}


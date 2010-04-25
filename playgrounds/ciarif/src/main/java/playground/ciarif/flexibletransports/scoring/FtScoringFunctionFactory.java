package playground.ciarif.flexibletransports.scoring;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.locationchoice.facilityload.FacilityPenalty;

import playground.ciarif.flexibletransports.config.FtConfigGroup;
import playground.meisterk.kti.scoring.ActivityScoringFunction;


public class FtScoringFunctionFactory extends CharyparNagelScoringFunctionFactory {
		
	private final Config config;
		private final FtConfigGroup ftConfigGroup;
		private final TreeMap<Id, FacilityPenalty> facilityPenalties;
		private final ActivityFacilities facilities;
		
		public FtScoringFunctionFactory(
				final Config config, 
				final FtConfigGroup ftConfigGroup,
				final TreeMap<Id, FacilityPenalty> facilityPenalties,
				final ActivityFacilities facilities) {
			super(config.charyparNagelScoring());
			this.config = config;
			this.ftConfigGroup = ftConfigGroup;
			this.facilityPenalties = facilityPenalties;
			this.facilities = facilities;
		}
	
		@Override
		public ScoringFunction createNewScoringFunction(Plan plan) {
			
			ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
			
			scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(
					plan, 
					super.getParams(), 
					this.facilityPenalties,
					this.facilities));
			scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(
					(PlanImpl) plan, 
					super.getParams(),
					config,
					this.ftConfigGroup));
			scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.MoneyScoringFunction(super.getParams()));
			scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction(super.getParams()));
			
			return scoringFunctionAccumulator;
			
		}
}

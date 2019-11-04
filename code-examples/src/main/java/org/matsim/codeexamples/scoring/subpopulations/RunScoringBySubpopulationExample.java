package org.matsim.codeexamples.scoring.subpopulations;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

class RunScoringBySubpopulationExample{

	public static void main( String[] args ){

		// this is just a syntax test!

		Config config = ConfigUtils.createConfig() ;
		{
			PlanCalcScoreConfigGroup.ScoringParameterSet params = config.planCalcScore().getOrCreateScoringParameters( "employed" );
			{
				PlanCalcScoreConfigGroup.ActivityParams actParams = new PlanCalcScoreConfigGroup.ActivityParams();
				actParams.setActivityType( "home" );
				// ...
				params.addActivityParams( actParams );
			}
			{
				PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams( TransportMode.car ) ;
				modeParams.setMarginalUtilityOfTraveling( -6. ) ;
				// ...
				params.addModeParams( modeParams );
			}
		}

	}
}

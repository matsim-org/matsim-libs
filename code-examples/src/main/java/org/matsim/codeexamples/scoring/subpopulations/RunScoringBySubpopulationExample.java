package org.matsim.codeexamples.scoring.subpopulations;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;

class RunScoringBySubpopulationExample{

	public static void main( String[] args ){

		// this is just a syntax test!

		Config config = ConfigUtils.createConfig() ;
		{
			ScoringConfigGroup.ScoringParameterSet params = config.scoring().getOrCreateScoringParameters( "employed" );
			{
				ScoringConfigGroup.ActivityParams actParams = new ScoringConfigGroup.ActivityParams();
				actParams.setActivityType( "home" );
				// ...
				params.addActivityParams( actParams );
			}
			{
				ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams( TransportMode.car ) ;
				modeParams.setMarginalUtilityOfTraveling( -6. ) ;
				// ...
				params.addModeParams( modeParams );
			}
		}

	}
}

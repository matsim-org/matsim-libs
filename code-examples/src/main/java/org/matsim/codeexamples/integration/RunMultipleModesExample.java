package org.matsim.codeexamples.integration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.matsim.core.config.groups.PlanCalcScoreConfigGroup.*;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode;

class RunMultipleModesExample{

	public static void main( String [] args ) {

		Config config = ConfigUtils.loadConfig( IOUtils.newUrl( ExamplesUtils.getTestScenarioURL( "equil" ) , "config.xml" ) ) ;

		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 1 );
		{
			StrategySettings stratSets = new StrategySettings(  ) ;
			stratSets.setStrategyName( ChangeSingleTripMode );
			stratSets.setWeight( 1. );
			config.strategy().addStrategySettings( stratSets );

			config.changeMode().setModes( new String [] { TransportMode.car, TransportMode.bike} );

			config.plansCalcRoute().setNetworkModes( Arrays.asList( TransportMode.car, TransportMode.bike ) );
			// yyyy the above line (by itself) leads to failure.  kai, jan'18
		}
		{ // bike:


			// scoring:
			ModeParams params = new ModeParams( TransportMode.bike ) ;
			config.planCalcScore().addModeParams( params );
		}

		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		for( Link link : scenario.getNetwork().getLinks().values() ){

			Set<String> modes = new HashSet<>(  Arrays.asList( TransportMode.car, TransportMode.bike ) ) ;
			link.setAllowedModes( modes );
		}

		// ---

		Controler controler = new Controler( scenario ) ;

		// ---

		controler.run() ;
	}

}

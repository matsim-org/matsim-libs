package org.matsim.codeexamples.extensions;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.FixedCostsEstimator;

import java.util.List;

class RunInformedModeChoiceExample{

	public static void main( String[] args ){

		Config config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" );

		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controller().setLastIteration( 100 );

		{
			InformedModeChoiceConfigGroup informedModeChoiceConfig = ConfigUtils.addOrGetModule( config, InformedModeChoiceConfigGroup.class );
			informedModeChoiceConfig.setModes( List.of( "car", "bike", "walk" ) );
			informedModeChoiceConfig.setTopK( 5 );
		}

		config.replanning().addStrategySettings( new ReplanningConfigGroup.StrategySettings().setStrategyName(
				InformedModeChoiceModule.SELECT_BEST_K_PLAN_MODES_STRATEGY ).setWeight( 0.1 ) );

		config.scoring().addModeParams( new ScoringConfigGroup.ModeParams( "bike" ) );
		config.scoring().addModeParams( new ScoringConfigGroup.ModeParams( "car" ).setDailyUtilityConstant( -50 ) );
		config.scoring().addModeParams( new ScoringConfigGroup.ModeParams( "walk" ) );

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario );

		InformedModeChoiceModule module = new InformedModeChoiceModule.Builder()
								  .withFixedCosts( FixedCostsEstimator.DailyConstant.class, "car" )
								  .withLegEstimator( DefaultLegScoreEstimator.class, ModeOptions.ConsiderIfCarAvailable.class, "car" )
								  .withLegEstimator( DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "bike", "walk" )
								  .build();
		controler.addOverridingModule( module );

		controler.run();

	}

}

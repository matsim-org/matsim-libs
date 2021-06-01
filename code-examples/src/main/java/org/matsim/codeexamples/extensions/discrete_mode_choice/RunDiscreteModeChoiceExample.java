package org.matsim.codeexamples.extensions.discrete_mode_choice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.*;
import org.matsim.contribs.discrete_mode_choice.modules.ModelModule.ModelType;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public final class RunDiscreteModeChoiceExample{
	// yyyyyy The code below fails with an OptionalTime exception.  :-(  kai, jun'21

	public static void main ( String [] args ) {

		args = new String [] {"scenarios/equil/config.xml"};

		Config config = ConfigUtils.loadConfig( args );

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		config.controler().setLastIteration( 2 );

		String modeB = "modeB";
		final String[] modes = {TransportMode.car, modeB};

		{
			PlansCalcRouteConfigGroup.ModeRoutingParams pars = new PlansCalcRouteConfigGroup.ModeRoutingParams( modeB );
			pars.setTeleportedModeSpeed( 10. );
			config.plansCalcRoute().addModeRoutingParams( pars );
		}
		{
			PlansCalcRouteConfigGroup.ModeRoutingParams pars = new PlansCalcRouteConfigGroup.ModeRoutingParams( TransportMode.walk );
			pars.setTeleportedModeSpeed( 4./3.6 );
			config.plansCalcRoute().addModeRoutingParams( pars );
		}
		{
			PlanCalcScoreConfigGroup.ModeParams params = new PlanCalcScoreConfigGroup.ModeParams( modeB );
			params.setConstant( 13. );
			config.planCalcScore().addModeParams( params );
		}

		// the following is first inlined and then adapted from DiscreteModeChoiceConfigurator.configureAsSubtourModeChoiceReplacement( config );
		{
			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
			stratSets.setStrategyName( DiscreteModeChoiceModule.STRATEGY_NAME );
			stratSets.setWeight( 0.1 );
			config.strategy().addStrategySettings( stratSets );
		}
		{
			DiscreteModeChoiceConfigGroup dmcConfig = ConfigUtils.addOrGetModule( config, DiscreteModeChoiceConfigGroup.class );

			dmcConfig.setCachedModes( CollectionUtils.stringArrayToSet( modes ) );
			// (I think that this is only a hint to the computation, influencing performance, but not behavior.  kai, jun'21)

			dmcConfig.getSubtourConstraintConfig().setConstrainedModes( CollectionUtils.stringArrayToSet( modes ) );
			// (These are, I think, the chain-based modes. kai, jun'21)

			dmcConfig.getVehicleTourConstraintConfig().setRestrictedModes( CollectionUtils.stringArrayToSet( modes ) );
			// (yy I don't know the difference to the subtour constraint.  kai, jun'21)

			dmcConfig.setModelType( ModelType.Tour );
			// (yy I assume that this means that the algorithm is working at the tour level.  As opposed to trip level.  kai, jun'21)

			dmcConfig.setSelector( SelectorModule.RANDOM );
			dmcConfig.setTourConstraints( CollectionUtils.stringArrayToSet( new String[]{ConstraintModule.VEHICLE_CONTINUITY, ConstraintModule.SUBTOUR_MODE} ) );
			dmcConfig.setTourEstimator( EstimatorModule.UNIFORM );
			dmcConfig.setTourFinder( TourFinderModule.PLAN_BASED );

			dmcConfig.setModeAvailability( ModeAvailabilityModule.DEFAULT );
			// (do not consider car availability.  If one looks into the code, this is in fact cleverly programmed as a replacable guice binding.)

			dmcConfig.getDefaultModeAvailabilityConfig().setAvailableModes( CollectionUtils.stringArrayToSet( modes ) );
			// (to configure the "detault" mode availability.  Don't know what it does. Presumably configures the modes between which can be switched.  kai, jun'21)
		}

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new DiscreteModeChoiceModule() );

		controler.run() ;

	}

}








































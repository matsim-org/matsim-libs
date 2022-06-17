package org.matsim.codeexamples.extensions.discrete_mode_choice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.*;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;

import static org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

public final class RunDiscreteModeChoiceExample{
	// The code below works with v14 since approx jun'21.  kai

	public static void main ( String [] args ) {

		args = new String [] {"scenarios/equil/config.xml"};

		Config config = ConfigUtils.loadConfig( args );

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		config.controler().setLastIteration( 2 );

		String modeB = "modeB";
		final String[] modes = {TransportMode.car, modeB};

		// we have to do this ugly cast because TeleportedModeParams extends ModeParams and the setter methods are part
		// of ModeParams and the return signature is ModeParams :-/ . This has to be changed in the Config Group itself
		var paramB = (PlansCalcRouteConfigGroup.TeleportedModeParams) new PlansCalcRouteConfigGroup.TeleportedModeParams(modeB).setTeleportedModeSpeed(10.);
		var paramWalk = (PlansCalcRouteConfigGroup.TeleportedModeParams) new PlansCalcRouteConfigGroup.TeleportedModeParams(TransportMode.walk)
				.setTeleportedModeSpeed(4./3.6);

		config.plansCalcRoute().addTeleportedModeParams(paramB);
		config.plansCalcRoute().addTeleportedModeParams(paramWalk);
		config.planCalcScore().addModeParams( new PlanCalcScoreConfigGroup.ModeParams( modeB ).setConstant( 13. ) );

		// the following is first inlined and then adapted from DiscreteModeChoiceConfigurator.configureAsSubtourModeChoiceReplacement( config );
		config.strategy().addStrategySettings( new StrategySettings().setStrategyName( DiscreteModeChoiceModule.STRATEGY_NAME ).setWeight( 0.1 ) );
		{
			DiscreteModeChoiceConfigGroup dmcConfig = ConfigUtils.addOrGetModule( config, DiscreteModeChoiceConfigGroup.class );

			dmcConfig.setCachedModes( CollectionUtils.stringArrayToSet( modes ) );
			// (I think that this is only a hint to the computation, influencing performance, but not behavior.  kai, jun'21)

			dmcConfig.getSubtourConstraintConfig().setConstrainedModes( CollectionUtils.stringArrayToSet( modes ) );
			// (These are, I think, the chain-based modes. kai, jun'21)

			dmcConfig.getVehicleTourConstraintConfig().setRestrictedModes( CollectionUtils.stringArrayToSet( modes ) );
			// (yy I don't know the difference to the subtour constraint.  kai, jun'21)

			dmcConfig.setModelType( ModelModule.ModelType.Tour );
			// (yy I assume that this means that the algorithm is working at the tour level.  As opposed to trip level.  kai, jun'21)

			dmcConfig.setSelector( SelectorModule.RANDOM );
			// (yyyy I don't know what this does.  kai, jun'21)

			dmcConfig.setTourConstraints( CollectionUtils.stringArrayToSet( new String[]{ConstraintModule.VEHICLE_CONTINUITY, ConstraintModule.SUBTOUR_MODE} ) );
			// (yyyy I don't know what this does.  kai, jun'21)

			dmcConfig.setTourEstimator( EstimatorModule.MATSIM_DAY_SCORING );
			// (yyyy I don't know what this does.  kai, jun'21)

			dmcConfig.setTourFinder( TourFinderModule.PLAN_BASED );
			// (yyyy I don't know what this does.  kai, jun'21)

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








































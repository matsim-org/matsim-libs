package org.matsim.codeexamples.extensions.discrete_mode_choice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.DefaultModeAvailability;
import org.matsim.contribs.discrete_mode_choice.modules.*;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;

import static org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

public final class RunDiscreteModeChoiceExample{
	// The code below works with v14 since approx jun'21.  kai

	public static void main ( String [] args ) {
		if ( args==null || args.length==0 ){
			args = new String[]{"scenarios/equil/config.xml"};
		}

		Config config = ConfigUtils.loadConfig( args );

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		config.controler().setLastIteration( 2 );

		String modeB = "modeB";
		String modeC = "modeC";
		final String[] modes = {TransportMode.car, modeB, modeC};

		config.plansCalcRoute().addTeleportedModeParams( new PlansCalcRouteConfigGroup.TeleportedModeParams(modeB).setTeleportedModeSpeed(10. ) );
		config.plansCalcRoute().addTeleportedModeParams( new PlansCalcRouteConfigGroup.TeleportedModeParams(modeC).setTeleportedModeSpeed(10. ) );
		config.plansCalcRoute().addTeleportedModeParams( new PlansCalcRouteConfigGroup.TeleportedModeParams(TransportMode.walk).setTeleportedModeSpeed(4./3.6 ) );

		config.planCalcScore().addModeParams( new PlanCalcScoreConfigGroup.ModeParams( modeB ).setConstant( 13. ) );
		config.planCalcScore().addModeParams( new PlanCalcScoreConfigGroup.ModeParams( modeC ).setConstant( 12. ) );

		// the following is first inlined and then adapted from DiscreteModeChoiceConfigurator.configureAsSubtourModeChoiceReplacement( config );
		config.strategy().addStrategySettings( new StrategySettings().setStrategyName( DiscreteModeChoiceModule.STRATEGY_NAME ).setWeight( 1. ) );
		{
			DiscreteModeChoiceConfigGroup dmcConfig = ConfigUtils.addOrGetModule( config, DiscreteModeChoiceConfigGroup.class );

			dmcConfig.setCachedModes( CollectionUtils.stringArrayToSet( modes ) );
			// (I think that this is only a hint to the computation, influencing performance, but not behavior.  kai, jun'21)

			dmcConfig.setModelType( ModelModule.ModelType.Tour );
			{ // material that is only relevant if ModelType=Tour:
				// (Means that the algo makes choices for each tour, configured below.  As opposed to making choices for each trip.)
				dmcConfig.setTourFinder( TourFinderModule.PLAN_BASED );
				// (A tour is something that returns to its starting point, but this still leaves several options.  "PLAN_BASED" means the
				// whole plan is considered as the tour ((what happens if it does not return to its starting location?)); there is also
				// "HOME_BASED" and "ACTIVITY_BASED" (trips starting from a certain activity type).)
//				dmcConfig.setTourConstraints( CollectionUtils.stringArrayToSet( new String[]{ConstraintModule.VEHICLE_CONTINUITY, ConstraintModule.SUBTOUR_MODE} ) );
				dmcConfig.setTourConstraints( CollectionUtils.stringArrayToSet( new String[]{ConstraintModule.VEHICLE_CONTINUITY} ) );
				// (This sets the constraints explicitly.  Can be configured with the next two options. Since backwards compatibility
				// with the old subtour mode choice behavior is not important for us, we remove the related constraint.)
				{
					dmcConfig.getVehicleTourConstraintConfig().setRestrictedModes( CollectionUtils.stringArrayToSet( new String[]{TransportMode.car } ) );
					// (config group for VehicleTourConstraint.  Vehicles in these modes need to be conserved.)

//					dmcConfig.getSubtourConstraintConfig().setConstrainedModes( CollectionUtils.stringArrayToSet( modes ) );
					// (config group for SubtourModeConstraint, which needs to be activated above for this here to have effect.
					// For modes listed here, the whole subtour has to use the same mode.  This is mostly for backwards
					// compatibility, since this is the behavior of the old subtour mode choice.)
				}
			}
			dmcConfig.setSelector( SelectorModule.MULTINOMIAL_LOGIT );
			// (This selects between the alternatives. Only "MULTINOMIAL_LOGIT" will actually use a logic model; "RANDOM" selects randomly
			// between the candidates, and "MAXIMUM" returns the best alternative.  "RANDOM" is what subtour mode choice does.)

			dmcConfig.setTourEstimator( EstimatorModule.MATSIM_DAY_SCORING );
			// (The estimator that is used to come up with utilities for the mode choice.)
			{
				dmcConfig.setModeAvailability( ModeAvailabilityModule.DEFAULT );
				// (do not consider car availability.  If one looks into the code, this is in fact cleverly programmed as a user-replaceable guice binding.)

				dmcConfig.getDefaultModeAvailabilityConfig().setAvailableModes( CollectionUtils.stringArrayToSet( new String[]{TransportMode.car, modeB, modeC } ) );
				// (_If_ default mode availability is configured above, this configures which modes this will include.  Other modes will not
				// be considered.  So if one removes modeC above, it will no longer be considered.)
			}
		}

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new DiscreteModeChoiceModule() );

//		controler.addOverridingModule( new AbstractModule(){
//			@Override public void install(){
//				bindModeAvailability(DEFAULT).to( DefaultModeAvailability.class );
//;
//			}
//		} )

//		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.run() ;

	}

}








































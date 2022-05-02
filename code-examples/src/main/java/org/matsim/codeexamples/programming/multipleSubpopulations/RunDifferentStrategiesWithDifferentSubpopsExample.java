package org.matsim.codeexamples.programming.multipleSubpopulations;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.algorithms.PermissibleModesCalculatorImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;

import javax.inject.Inject;

public final class RunDifferentStrategiesWithDifferentSubpopsExample{
	// I haven't tested this.  kai, feb'19

	public static void main( String[] args ){

		Config config = ConfigUtils.createConfig() ;
		config.controler().setLastIteration(2);

		// use the default subtour mode choice for subpop1:
		{
			StrategySettings settings = new StrategySettings(  ) ;
			settings.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice ) ;
			settings.setSubpopulation( "subpop1" );
			settings.setWeight( 0.2 );
			config.strategy().addStrategySettings( settings );

			config.subtourModeChoice().setModes( new String[]{ TransportMode.car, TransportMode.pt, TransportMode.bike} );
		}

		// use subtour mode choice strategy defined below for subpop2:
		{
			StrategySettings settings = new StrategySettings(  ) ;
			settings.setStrategyName( "subtourModeChoice2" );
			settings.setSubpopulation( "subpop2" );
			settings.setWeight( 0.2 );
			config.strategy().addStrategySettings( settings );
		}

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule( new AbstractModule(){
			@Override
			public void install(){
				// define second subtour mode choice strategy:
				this.addPlanStrategyBinding( "subtourModeChoice2" ).toProvider(new Provider<>() {
					@Inject
					private Provider<TripRouter> tripRouterProvider;
					@Inject
					private GlobalConfigGroup globalConfigGroup;
					@Inject
					private ActivityFacilities facilities;
					@Inject
					private TimeInterpretation timeInterpretation;

					@Override
					public PlanStrategy get() {
						PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
						SubtourModeChoiceConfigGroup modeChoiceConfig = new SubtourModeChoiceConfigGroup();
						modeChoiceConfig.setModes(new String[]{TransportMode.car, TransportMode.bike});
						builder.addStrategyModule(new SubtourModeChoice(globalConfigGroup, modeChoiceConfig, new PermissibleModesCalculatorImpl(config)));
						builder.addStrategyModule(new ReRoute(facilities, tripRouterProvider, globalConfigGroup, timeInterpretation) );
						return builder.build();
					}
				} ) ;
			}
		} ) ;


		controler.run() ;
	}

}

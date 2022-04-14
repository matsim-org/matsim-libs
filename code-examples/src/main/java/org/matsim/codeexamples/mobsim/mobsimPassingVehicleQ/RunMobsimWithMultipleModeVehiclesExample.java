package org.matsim.codeexamples.mobsim.mobsimPassingVehicleQ;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.HashSet;

import static org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import static org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import static org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import static org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;

/**
 * Example to show how the standard queue can be replaced by something else.  Search for PassingVehicleQ in the code below.
 * <p></p>
 * I have a version of this that was running about a year ago in my playground.  This "tutorial" version has never been tested (but please
 * feel free to test, report back, and fix).
 * 
 * @author nagel
 *
 */
final class RunMobsimWithMultipleModeVehiclesExample {

	public static void main( String[] args ) {
		Config config ;
		if ( args != null && args.length>=1 ) {
			config = ConfigUtils.loadConfig( args ) ;
		} else {
			config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" ) ;
			config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setLastIteration( 2 );
		}

		config.qsim().setLinkDynamics( LinkDynamics.PassingQ );
		// (The above is the important line!)

		config.qsim().setVehiclesSource( VehiclesSource.modeVehicleTypesFromVehiclesData );

		final HashSet<String> networkModesAsSet = new HashSet<>( ) ;
		{
			String mode = "car" ;
			// routing, qsim:
			networkModesAsSet.add(mode) ;
			// scoring:
			ModeParams params = new ModeParams( mode ) ;
			config.planCalcScore().addModeParams( params );
		}
		{
			String mode = "bicycle" ;
			// routing, qsim:
			networkModesAsSet.add(mode) ;
			// scoring:
			ModeParams params = new ModeParams( mode ) ;
			config.planCalcScore().addModeParams( params );
		}
		{
			String mode = "bike" ;
			// routing, qsim:
			config.plansCalcRoute().removeTeleportedModeParams( mode );
			networkModesAsSet.add(mode) ;
			// scoring:
			ModeParams params = new ModeParams( mode ) ;
			config.planCalcScore().addModeParams( params );
		}
		{
			String mode = "walk" ;
			// routing, qsim:
			config.plansCalcRoute().removeTeleportedModeParams( mode );
			networkModesAsSet.add(mode) ;
			// scoring:
			ModeParams params = new ModeParams( mode ) ;
			config.planCalcScore().addModeParams( params );
		}

		{
			StrategySettings stratSets = new StrategySettings(  ) ;
			stratSets.setWeight( 1.0 );
			stratSets.setStrategyName( DefaultStrategy.ChangeSingleTripMode );
			config.strategy().addStrategySettings( stratSets );

			config.changeMode().setModes( networkModesAsSet.toArray( new String[0] ) );
		}

		config.plansCalcRoute().setNetworkModes( networkModesAsSet ) ;

		config.qsim().setMainModes( networkModesAsSet );

		// prepare the scenario
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// make all links useable by all modes:
		for( Link link : scenario.getNetwork().getLinks().values() ){
			link.setAllowedModes( networkModesAsSet );
		}

		VehicleType car = VehicleUtils.getFactory().createVehicleType( Id.create("car", VehicleType.class ) );
		car.setMaximumVelocity(60.0/3.6);
		car.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(car);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike", VehicleType.class));
		bike.setMaximumVelocity(60.0/3.6);
		bike.setPcuEquivalents(0.25);
		scenario.getVehicles().addVehicleType(bike);

		VehicleType bicycles = VehicleUtils.getFactory().createVehicleType(Id.create("bicycle", VehicleType.class));
		bicycles.setMaximumVelocity(15.0/3.6);
		bicycles.setPcuEquivalents(0.05);
		scenario.getVehicles().addVehicleType(bicycles);

		VehicleType walks = VehicleUtils.getFactory().createVehicleType(Id.create("walk", VehicleType.class));
		walks.setMaximumVelocity(1.5);
		walks.setPcuEquivalents(0.10);  			// assumed pcu for walks is 0.1
		scenario.getVehicles().addVehicleType(walks);

		// prepare the control(l)er:
		Controler controler = new Controler( scenario ) ;

		// run everything:
		controler.run();
	}

}

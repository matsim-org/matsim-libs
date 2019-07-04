package org.matsim.codeexamples.pt;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.etaxi.optimizer.assignment.ETaxiToPlugAssignmentCostProvider;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.matsim.core.config.groups.PlansCalcRouteConfigGroup.*;

public class PtAlongALine2Test{

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	enum DrtMode { none, teleportBeeline, teleportBasedOnNetworkRoute, full }
	private DrtMode drtMode = DrtMode.teleportBasedOnNetworkRoute ;
	private boolean drt2 = true ;

	@Test
	public void testPtAlongALineWithRaptorAndDrtServiceArea() {
		// Towards some understanding of what is going on here:
		// * In many situations, a good solution is that drt drives to some transit stop, and from there directly to the destination.  The swiss rail
		// raptor will return a cost "infinity" of such a solution, in which case the calling method falls back onto transit_walk.
		// * If "walk" is defined as intermodal access, then swiss rail raptor will call the correct RoutingModule, but afterwards change the mode of all
		// legs to non_network_mode.

		Config config = PtAlongALineTest.createConfig( utils.getOutputDirectory() );

		// === GBL: ===

		config.controler().setLastIteration( 0 );

		// === ROUTER: ===

		config.plansCalcRoute().setInsertingAccessEgressWalk( true );

		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );
		// (as of today, will also influence router. kai, jun'19)

		if(  drtMode == DrtMode.teleportBeeline ){// (configure teleportation router)
			{
				ModeRoutingParams mrp = new ModeRoutingParams();
				mrp.setMode( TransportMode.drt );
				mrp.setTeleportedModeSpeed( 100. / 3.6 );
				config.plansCalcRoute().addModeRoutingParams( mrp );
			}
			if( drt2 ){
				ModeRoutingParams mrp = new ModeRoutingParams();
				mrp.setMode( "drt2" );
				mrp.setTeleportedModeSpeed( 100. / 3.6 );
				config.plansCalcRoute().addModeRoutingParams( mrp );
			}
			// teleportation router for walk or bike is automatically defined.
		} else if( drtMode == DrtMode.teleportBasedOnNetworkRoute ){// (route as network route)
			//			config.plansCalcRoute().removeModeRoutingParams( TransportMode.walk );
			Set<String> networkModes = new HashSet<>( config.plansCalcRoute().getNetworkModes() );
			networkModes.add( TransportMode.drt );
			//			networkModes.add( TransportMode.walk );
			if( drt2 ){
				networkModes.add( "drt2" );
			}
			config.plansCalcRoute().setNetworkModes( networkModes );
		}

		{
			ModeRoutingParams wlk = new ModeRoutingParams(  ) ;
			wlk.setMode( "walk2" ) ;
			wlk.setTeleportedModeSpeed( 5./3.6 ) ;
			config.plansCalcRoute().addModeRoutingParams( wlk );
		}

		// === RAPTOR: ===
		{
			SwissRailRaptorConfigGroup configRaptor = ConfigUtils.addOrGetModule( config, SwissRailRaptorConfigGroup.class ) ;

			if ( drtMode!=DrtMode.none){
				configRaptor.setUseIntermodalAccessEgress(true);
				{
					// Xxx
					IntermodalAccessEgressParameterSet paramSetXxx = new IntermodalAccessEgressParameterSet();
//					paramSetXxx.setMode( TransportMode.walk ); // this does not work because sbb raptor treats it in a special way
					paramSetXxx.setMode( "walk2" );
					paramSetXxx.setRadius( 1000000 );
					configRaptor.addIntermodalAccessEgress( paramSetXxx );
					// (in principle, walk as alternative to drt will not work, since drt is always faster.  Need to give the ASC to the router!  However, with
					// the reduced drt network we should be able to see differentiation.)
				}
				{
					// drt
					IntermodalAccessEgressParameterSet paramSetDrt = new IntermodalAccessEgressParameterSet();
					paramSetDrt.setMode( TransportMode.drt );
					paramSetDrt.setRadius( 1000000 );
					configRaptor.addIntermodalAccessEgress( paramSetDrt );
				}
				if ( drt2 ){
					IntermodalAccessEgressParameterSet paramSetDrt2 = new IntermodalAccessEgressParameterSet();
					paramSetDrt2.setMode( "drt2" );
					paramSetDrt2.setRadius( 1000000 );
					//				paramSetDrt2.setPersonFilterAttribute( null );
					//				paramSetDrt2.setStopFilterAttribute( null );
					configRaptor.addIntermodalAccessEgress( paramSetDrt2 );
				}
			}

		}

		// === SCORING: ===

		double margUtlTravPt = config.planCalcScore().getModes().get( TransportMode.pt ).getMarginalUtilityOfTraveling();;
		if ( drtMode!=DrtMode.none ) {
			//			{
//				PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams("access_walk");
//				config.planCalcScore().addModeParams(modeParams);
//			}
//			{
//				PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams("egress_walk");
//				config.planCalcScore().addModeParams(modeParams);
//			}
			// (scoring parameters for drt modes)
			{
				PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.drt);
				modeParams.setMarginalUtilityOfTraveling( margUtlTravPt );
				config.planCalcScore().addModeParams(modeParams);
			}
			if ( drt2 ) {
				PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams("drt2");
				modeParams.setMarginalUtilityOfTraveling( margUtlTravPt );
				config.planCalcScore().addModeParams(modeParams);
			}
		}
		{
			PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams("walk2");
			modeParams.setMarginalUtilityOfTraveling( margUtlTravPt );
			config.planCalcScore().addModeParams(modeParams);
		}

		config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime );
		// yy why?  kai, jun'19

		// === DRT: ===

		if ( drtMode==DrtMode.full ){
			// (configure full drt if applicable)

			String drtVehiclesFile = "drt_vehicles.xml";
			String drt2VehiclesFile = "drt2_vehicles.xml";

			DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );
			// TODO: How can we set the network mode of drt2?
			// TODO: Right now uncommenting the following line gives guice injection errors
			//		dvrpConfig.setNetworkMode(TransportMode.drt);

			MultiModeDrtConfigGroup mm = ConfigUtils.addOrGetModule( config, MultiModeDrtConfigGroup.class );
			{
				DrtConfigGroup drtConfig = new DrtConfigGroup();
				drtConfig.setMaxTravelTimeAlpha( 1.3 );
				drtConfig.setVehiclesFile( drtVehiclesFile );
				drtConfig.setMaxTravelTimeBeta( 5. * 60. );
				drtConfig.setStopDuration( 60. );
				drtConfig.setMaxWaitTime( Double.MAX_VALUE );
				drtConfig.setRequestRejection( false );
				drtConfig.setMode( TransportMode.drt );
				mm.addParameterSet( drtConfig );
			}
			if ( drt2 ) {
				DrtConfigGroup drtConfig = new DrtConfigGroup();
				drtConfig.setMaxTravelTimeAlpha( 1.3 );
				drtConfig.setVehiclesFile( drt2VehiclesFile );
				drtConfig.setMaxTravelTimeBeta( 5. * 60. );
				drtConfig.setStopDuration( 60. );
				drtConfig.setMaxWaitTime( Double.MAX_VALUE );
				drtConfig.setMode( "drt2" );
				mm.addParameterSet( drtConfig );
			}

			for( DrtConfigGroup drtConfigGroup : mm.getModalElements() ){
				DrtConfigs.adjustDrtConfig( drtConfigGroup, config.planCalcScore() );
			}

			// TODO: avoid really writing out these files. However so far it is unclear how
			// to configure DRT and load the vehicles otherwise
			PtAlongALineTest.createDrtVehiclesFile(drtVehiclesFile, "DRT-", 10, Id.createLinkId("0-1" ) );
			if ( drt2 ){
				PtAlongALineTest.createDrtVehiclesFile( drt2VehiclesFile, "DRT2-", 1, Id.createLinkId( "1000-999" ) );
			}

		}

		// === VSP: ===

		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn );

		// ### SCENARIO: ###

		Scenario scenario = PtAlongALineTest.createScenario(config , 100 );

		if ( drtMode==DrtMode.full ) {
			scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory( DrtRoute.class, new DrtRouteFactory() );
		}

		//		// TODO: reference somehow network creation, to ensure that these link ids exist
		//		// add drt modes to the car links' allowed modes in their respective service area
		PtAlongALineTest.addModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 0, 400, TransportMode.drt );
		if ( drt2 ){
			PtAlongALineTest.addModeToAllLinksBtwnGivenNodes( scenario.getNetwork(), 600, 1000, "drt2" );
		}


		// The following is for the _router_, not the qsim!  kai, jun'19
		VehiclesFactory vf = scenario.getVehicles().getFactory();
		if ( drt2 ) {
			VehicleType vehType = vf.createVehicleType( Id.create( "drt2", VehicleType.class ) );
			vehType.setMaximumVelocity( 25./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}{
			VehicleType vehType = vf.createVehicleType( Id.create( TransportMode.drt, VehicleType.class ) );
			vehType.setMaximumVelocity( 25./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}{
			VehicleType vehType = vf.createVehicleType( Id.create( TransportMode.car, VehicleType.class ) );
			vehType.setMaximumVelocity( 25./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}

		scenario.getPopulation().getPersons().values().removeIf( person -> !person.getId().toString().equals( "3" ) );

		// ### CONTROLER: ###

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new SwissRailRaptorModule() ) ;

		if ( drtMode==DrtMode.full ){
			controler.addOverridingModule( new DvrpModule() );
			controler.addOverridingModule( new MultiModeDrtModule() );
			if ( drt2 ){
				controler.configureQSimComponents( DvrpQSimComponents.activateModes( TransportMode.drt, "drt2" ) );
			} else{
				controler.configureQSimComponents( DvrpQSimComponents.activateModes( TransportMode.drt ) );
			}
		}

		// This will start otfvis.  Comment out if not needed.
		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.run();
	}

	@Test
	public void networkWalkDoesNotWorkWithRaptor() {

		Config config = PtAlongALineTest.createConfig( utils.getOutputDirectory() );

		// === GBL: ===

		config.controler().setLastIteration( 0 );

		// === ROUTER: ===

		config.plansCalcRoute().setInsertingAccessEgressWalk( true );

		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );
		// (as of today, will also influence router. kai, jun'19)

		// remove teleportation walk router:
		config.plansCalcRoute().removeModeRoutingParams( TransportMode.walk );

		// add network walk router:
		Set<String> networkModes = new HashSet<>( config.plansCalcRoute().getNetworkModes() );
		networkModes.add( TransportMode.walk );
		config.plansCalcRoute().setNetworkModes( networkModes );

		// === RAPTOR: ===
		{
			SwissRailRaptorConfigGroup configRaptor = ConfigUtils.addOrGetModule( config, SwissRailRaptorConfigGroup.class ) ;

			configRaptor.setUseIntermodalAccessEgress(true);
			{
				// Xxx
				IntermodalAccessEgressParameterSet paramSetXxx = new IntermodalAccessEgressParameterSet();
				paramSetXxx.setMode( TransportMode.walk );
				paramSetXxx.setRadius( 1000000 );
				configRaptor.addIntermodalAccessEgress( paramSetXxx );
			}

		}

		// ### SCENARIO: ###

		Scenario scenario = PtAlongALineTest.createScenario(config , 100 );

		PtAlongALineTest.addModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 0, 1000, TransportMode.walk );


		// The following is in particular for the _router_, not the qsim!  kai, jun'19
		VehiclesFactory vf = scenario.getVehicles().getFactory();
		{
			VehicleType vehType = vf.createVehicleType( Id.create( TransportMode.walk, VehicleType.class ) );
			vehType.setMaximumVelocity( 4./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}

		// ### CONTROLER: ###

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new SwissRailRaptorModule() ) ;

		// This will start otfvis.  Comment out if not needed.
		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.run();
	}



}

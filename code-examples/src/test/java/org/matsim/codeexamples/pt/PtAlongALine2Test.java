package org.matsim.codeexamples.pt;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.jts.awt.PointShapeFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.etaxi.optimizer.assignment.ETaxiToPlugAssignmentCostProvider;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.*;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilter;

import java.util.*;

import static org.matsim.core.config.groups.PlansCalcRouteConfigGroup.*;

public class PtAlongALine2Test{
	private static final Logger log = Logger.getLogger( PtAlongALine2Test.class );

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	enum DrtMode { none, teleportBeeline, teleportBasedOnNetworkRoute, full }
	private DrtMode drtMode = DrtMode.full ;
	private boolean drt2 = true ;
	private boolean drt3 = true ;

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
				config.plansCalcRoute().addModeRoutingParams( new ModeRoutingParams().setMode( TransportMode.drt ).setTeleportedModeSpeed( 100. / 3.6 ) );
			if( drt2 ){
				config.plansCalcRoute().addModeRoutingParams( new ModeRoutingParams().setMode( "drt2" ).setTeleportedModeSpeed( 100. / 3.6 ) );
			}
			if( drt3 ){
				config.plansCalcRoute().addModeRoutingParams( new ModeRoutingParams().setMode( "drt3" ).setTeleportedModeSpeed( 100. / 3.6 ) );
			}
			// teleportation router for walk or bike is automatically defined.
		} else if( drtMode == DrtMode.teleportBasedOnNetworkRoute ){// (route as network route)
			Set<String> networkModes = new HashSet<>( );
			networkModes.add( TransportMode.drt );
			if( drt2 ){
				networkModes.add( "drt2" );
			}
			if( drt3 ){
				networkModes.add( "drt3" );
			}
			config.plansCalcRoute().setNetworkModes( networkModes );
		}

		// set up walk2 so we don't need walk in raptor:
		config.plansCalcRoute().addModeRoutingParams( new ModeRoutingParams(  ).setMode( "walk2" ).setTeleportedModeSpeed( 5./3.6 ) );

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
					paramSetXxx.setMaxRadius( 1000000 );
					configRaptor.addIntermodalAccessEgress( paramSetXxx );
					// (in principle, walk as alternative to drt will not work, since drt is always faster.  Need to give the ASC to the router!  However, with
					// the reduced drt network we should be able to see differentiation.)
				}
				{
					// drt
					IntermodalAccessEgressParameterSet paramSetDrt = new IntermodalAccessEgressParameterSet();
					paramSetDrt.setMode( TransportMode.drt );
					paramSetDrt.setMaxRadius( 1000000 );
					configRaptor.addIntermodalAccessEgress( paramSetDrt );
				}
				if ( drt2 ){
					IntermodalAccessEgressParameterSet paramSetDrt2 = new IntermodalAccessEgressParameterSet();
					paramSetDrt2.setMode( "drt2" );
					paramSetDrt2.setMaxRadius( 1000000 );
					//				paramSetDrt2.setPersonFilterAttribute( null );
					//				paramSetDrt2.setStopFilterAttribute( null );
					configRaptor.addIntermodalAccessEgress( paramSetDrt2 );
				}
				if ( drt3 ){
					IntermodalAccessEgressParameterSet paramSetDrt2 = new IntermodalAccessEgressParameterSet();
					paramSetDrt2.setMode( "drt3" );
					paramSetDrt2.setMaxRadius( 1000000 );
					//				paramSetDrt2.setPersonFilterAttribute( null );
					//				paramSetDrt2.setStopFilterAttribute( null );
					configRaptor.addIntermodalAccessEgress( paramSetDrt2 );
				}
			}

		}

		// === SCORING: ===

		double margUtlTravPt = config.planCalcScore().getModes().get( TransportMode.pt ).getMarginalUtilityOfTraveling();;
		if ( drtMode!=DrtMode.none ) {
			// (scoring parameters for drt modes)
			config.planCalcScore().addModeParams( new ModeParams(TransportMode.drt).setMarginalUtilityOfTraveling( margUtlTravPt ) );
			if ( drt2 ) {
				config.planCalcScore().addModeParams( new ModeParams("drt2").setMarginalUtilityOfTraveling( margUtlTravPt ) );
			}
			if ( drt3 ) {
				config.planCalcScore().addModeParams( new ModeParams("drt3").setMarginalUtilityOfTraveling( margUtlTravPt ) );
			}
		}
		config.planCalcScore().addModeParams( new ModeParams("walk2").setMarginalUtilityOfTraveling( margUtlTravPt ) );

		// === QSIM: ===

		config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime );
		// yy why?  kai, jun'19

		// === DRT: ===

		if ( drtMode==DrtMode.full ){
			// (configure full drt if applicable)

			String drtVehiclesFile = "drt_vehicles.xml";
			String drt2VehiclesFile = "drt2_vehicles.xml";
			String drt3VehiclesFile = "drt3_vehicles.xml";

			DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );
			dvrpConfig.setNetworkModes( ImmutableSet.copyOf( Arrays.asList( TransportMode.drt, "drt2", "drt3" ) ) ) ;

			MultiModeDrtConfigGroup mm = ConfigUtils.addOrGetModule( config, MultiModeDrtConfigGroup.class );
			{
				DrtConfigGroup drtConfig = new DrtConfigGroup();
				drtConfig.setMaxTravelTimeAlpha( 1.3 );
				drtConfig.setVehiclesFile( drtVehiclesFile );
				drtConfig.setMaxTravelTimeBeta( 5. * 60. );
				drtConfig.setStopDuration( 60. );
				drtConfig.setMaxWaitTime( Double.MAX_VALUE );
				drtConfig.setRejectRequestIfMaxWaitOrTravelTimeViolated( false );
				drtConfig.setMode( TransportMode.drt );
				drtConfig.setUseModeFilteredSubnetwork( true );
				mm.addParameterSet( drtConfig );
			}
			if ( drt2 ) {
				DrtConfigGroup drtConfig = new DrtConfigGroup();
				drtConfig.setMaxTravelTimeAlpha( 1.3 );
				drtConfig.setVehiclesFile( drt2VehiclesFile );
				drtConfig.setMaxTravelTimeBeta( 5. * 60. );
				drtConfig.setStopDuration( 60. );
				drtConfig.setMaxWaitTime( Double.MAX_VALUE );
				drtConfig.setRejectRequestIfMaxWaitOrTravelTimeViolated( false );
				drtConfig.setMode( "drt2" );
				drtConfig.setUseModeFilteredSubnetwork( true );
				mm.addParameterSet( drtConfig );
			}
			if ( drt2 ) {
				DrtConfigGroup drtConfig = new DrtConfigGroup();
				drtConfig.setMaxTravelTimeAlpha( 1.3 );
				drtConfig.setVehiclesFile( drt3VehiclesFile );
				drtConfig.setMaxTravelTimeBeta( 5. * 60. );
				drtConfig.setStopDuration( 60. );
				drtConfig.setMaxWaitTime( Double.MAX_VALUE );
				drtConfig.setRejectRequestIfMaxWaitOrTravelTimeViolated( false );
				drtConfig.setMode( "drt3" );
				drtConfig.setUseModeFilteredSubnetwork( true );
				mm.addParameterSet( drtConfig );
			}

			for( DrtConfigGroup drtConfigGroup : mm.getModalElements() ){
				DrtConfigs.adjustDrtConfig( drtConfigGroup, config.planCalcScore() );
			}

			// TODO: avoid really writing out these files. However so far it is unclear how
			// to configure DRT and load the vehicles otherwise
			PtAlongALineTest.createDrtVehiclesFile(drtVehiclesFile, "DRT-", 10, Id.createLinkId("0-1" ) );
			if ( drt2 ){
				PtAlongALineTest.createDrtVehiclesFile( drt2VehiclesFile, "DRT2-", 10, Id.createLinkId( "999-1000" ) );
			}
			if ( drt3 ){
				PtAlongALineTest.createDrtVehiclesFile( drt3VehiclesFile, "DRT3-", 10, Id.createLinkId( "500-501" ) );
			}

		}

		// === VSP: ===

		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn );

		// ### SCENARIO: ###

		Scenario scenario = PtAlongALineTest.createScenario(config , 30 );

		if ( drtMode==DrtMode.full ) {
			scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory( DrtRoute.class, new DrtRouteFactory() );
		}

		// add drt modes to the car links' allowed modes in their respective service area
		PtAlongALineTest.addModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 0, 400, TransportMode.drt );
		if ( drt2 ){
			PtAlongALineTest.addModeToAllLinksBtwnGivenNodes( scenario.getNetwork(), 700, 1000, "drt2" );
		}
		if ( drt3 ){
			PtAlongALineTest.addModeToAllLinksBtwnGivenNodes( scenario.getNetwork(), 500, 600, "drt3" );
		}
		// TODO: reference somehow network creation, to ensure that these link ids exist


		// The following is also for the router! kai, jun'19
		VehiclesFactory vf = scenario.getVehicles().getFactory();
		if ( drt2 ) {
			VehicleType vehType = vf.createVehicleType( Id.create( "drt2", VehicleType.class ) );
			vehType.setMaximumVelocity( 25./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		} if ( drt3 ) {
			VehicleType vehType = vf.createVehicleType( Id.create( "drt3", VehicleType.class ) );
			vehType.setMaximumVelocity( 25./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}{
			VehicleType vehType = vf.createVehicleType( Id.create( TransportMode.drt, VehicleType.class ) );
			vehType.setMaximumVelocity( 25./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}
		{
			// (does not work without; I don't really know why. kai)
			VehicleType vehType = vf.createVehicleType( Id.create( TransportMode.car, VehicleType.class ) );
			vehType.setMaximumVelocity( 25./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}

//		scenario.getPopulation().getPersons().values().removeIf( person -> !person.getId().toString().equals( "3" ) );

		// ### CONTROLER: ###

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new SwissRailRaptorModule() ) ;

		if ( drtMode==DrtMode.full ){
			controler.addOverridingModule( new DvrpModule() );
			controler.addOverridingModule( new MultiModeDrtModule() );
			if ( drt2 && drt3 ){
				controler.configureQSimComponents( DvrpQSimComponents.activateModes( TransportMode.drt, "drt2", "drt3" ) );
			} else if (drt2) {
				controler.configureQSimComponents( DvrpQSimComponents.activateModes( TransportMode.drt, "drt2" ) );
			} else if (drt3) {
				controler.configureQSimComponents( DvrpQSimComponents.activateModes( TransportMode.drt, "drt3" ) );
			} else {
				controler.configureQSimComponents( DvrpQSimComponents.activateModes( TransportMode.drt ) );
			}
		}

		// This will start otfvis.  Comment out if not needed.
		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.run();
		
		/*
		 * TODO: Asserts:
		 * All agents go from some randomly chosen link to the transit stop at the far right.
		 * 
		 * Nobody should use DRT2, because it only connects to that transit stop at the right.
		 * 
		 * People on the left should use DRT to go to the left stop or towards the middle stop (and walk the distance
		 * between the end of the DRT service area and the middle stop).
		 * 
		 * People between the middle stop and the right stop should use DRT3 or even walk into the DRT3 service area
		 * (=walk in the wrong direction to access the fast drt mode to access a fast pt trip instead of slowly walking
		 * the whole distance to the destination). At some point walking towards the DRT3 area becomes less attractive
		 * thanh walking directly to the right transit stop, so agents start to walk directly (as the pt router found no
		 * route at all including a pt leg, it returned instead a direct walk).
		 * 
		 */
		List<PlanElement> pes = controler.getScenario().getPopulation().getPersons().get(Id.createPersonId("Case2.3Agent")).getSelectedPlan().getPlanElements();
		Leg accessLeg = (Leg) pes.get(1);
	}

	@Test
	public void intermodalAccessEgressPicksWrongVariant() {
		// outdated comment:
		// this test fails because it picks a
		//    drt-nonNetworkWalk-nonNetworkWalk-drt
		//  trip over a faster
		//    drt-nonNetworkWalk-pt-nonNetworkWalk-drt
		// trip.  Commenting out the method
		//    handleTransfers(...)
		// in SwissRailRaptorCore
		//         if (hasIntermodalAccess) {
		//            // allow transfering from the initial stop to another one if we have intermodal access,
		//            // as not all stops might be intermodal
		//            handleTransfers(true, parameters);
		//        }
		// makes it pass.  I have no idea why, or if this would be a good direction to go for a fix.  kai, jul'19
		
		// does now work with these lines of code in SwissRailRaptorCore (which solve problems in other tests) gleich, aug'19

		Config config = PtAlongALineTest.createConfig( utils.getOutputDirectory() );

		// === GBL: ===

		config.controler().setLastIteration( 0 );

		// === ROUTER: ===

		config.plansCalcRoute().setInsertingAccessEgressWalk( true );

		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );
		// (as of today, will also influence router. kai, jun'19)
		{
			Set<String> networkModes = new HashSet<>( );
			networkModes.add( TransportMode.drt );
			networkModes.add( "drt2" );
			config.plansCalcRoute().setNetworkModes( networkModes );
		}

		// set up walk2 so we don't use faulty walk in raptor:
		{
			ModeRoutingParams wlk = new ModeRoutingParams(  ) ;
			wlk.setMode( "walk2" ) ;
			wlk.setTeleportedModeSpeed( 5./3.6 ) ;
			config.plansCalcRoute().addModeRoutingParams( wlk );
		}

		// === RAPTOR: ===
		{
			SwissRailRaptorConfigGroup configRaptor = ConfigUtils.addOrGetModule( config, SwissRailRaptorConfigGroup.class ) ;

			{
				configRaptor.setUseIntermodalAccessEgress(true);
				{
					// Xxx
					IntermodalAccessEgressParameterSet paramSetXxx = new IntermodalAccessEgressParameterSet();
					//					paramSetXxx.setMode( TransportMode.walk ); // this does not work because sbb raptor treats it in a special way
					paramSetXxx.setMode( "walk2" );
					paramSetXxx.setMaxRadius( 1000000 );
					configRaptor.addIntermodalAccessEgress( paramSetXxx );
					// (in principle, walk as alternative to drt will not work, since drt is always faster.  Need to give the ASC to the router!  However, with
					// the reduced drt network we should be able to see differentiation.)
				}
				{
					// drt
					IntermodalAccessEgressParameterSet paramSetDrt = new IntermodalAccessEgressParameterSet();
					paramSetDrt.setMode( TransportMode.drt );
					paramSetDrt.setMaxRadius( 1000000 );
					configRaptor.addIntermodalAccessEgress( paramSetDrt );
				}
				if ( drt2 ){
					IntermodalAccessEgressParameterSet paramSetDrt2 = new IntermodalAccessEgressParameterSet();
					paramSetDrt2.setMode( "drt2" );
					paramSetDrt2.setMaxRadius( 1000000 );
					//				paramSetDrt2.setPersonFilterAttribute( null );
					//				paramSetDrt2.setStopFilterAttribute( null );
					configRaptor.addIntermodalAccessEgress( paramSetDrt2 );
				}
			}

		}

		// === SCORING: ===

		double margUtlTravPt = config.planCalcScore().getModes().get( TransportMode.pt ).getMarginalUtilityOfTraveling();;
		{
			ModeParams modeParams = new ModeParams(TransportMode.drt);
			modeParams.setMarginalUtilityOfTraveling( margUtlTravPt );
			config.planCalcScore().addModeParams(modeParams);
		}
		{
				ModeParams modeParams = new ModeParams("drt2");
				modeParams.setMarginalUtilityOfTraveling( margUtlTravPt );
				config.planCalcScore().addModeParams(modeParams);
		}
		{
			ModeParams modeParams = new ModeParams("walk2");
			modeParams.setMarginalUtilityOfTraveling( margUtlTravPt );
			config.planCalcScore().addModeParams(modeParams);
		}

		// === QSIM: ===

		config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime );
		// yy why?  kai, jun'19

		config.qsim().setMainModes( Arrays.asList( TransportMode.car, TransportMode.drt, "drt2") );
		// yyyy buses use the car network and so that needs to be defined as network mode.   !!!! :-( :-(

		// === VSP: ===

		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn );

		// ### SCENARIO: ###

		Scenario scenario = PtAlongALineTest.createScenario(config , 100 );

		PtAlongALineTest.addModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 0, 400, TransportMode.drt );
		PtAlongALineTest.addModeToAllLinksBtwnGivenNodes( scenario.getNetwork(), 600, 1000, "drt2" );


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
		}
		{
			// (does not work without; I don't really know why. kai)
			VehicleType vehType = vf.createVehicleType( Id.create( TransportMode.car, VehicleType.class ) );
			vehType.setMaximumVelocity( 100./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}

		scenario.getPopulation().getPersons().values().removeIf( person -> !person.getId().toString().equals( "3" ) );

		// ### CONTROLER: ###

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new SwissRailRaptorModule() ) ;

		// This will start otfvis.  Comment in if desired.
//		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.addControlerListenerBinding().toInstance( new IterationEndsListener(){
					@Inject private Population population ;
					@Override public void notifyIterationEnds( IterationEndsEvent event ){
						for( Person person : population.getPersons().values() ){

							// output to help with debugging:
							log.warn("") ;
							log.warn("selected plan for personId=" + person.getId() ) ;
							for( PlanElement planElement : person.getSelectedPlan().getPlanElements() ){
								log.warn( planElement ) ;
							}
							log.warn("");

							// the trip should contain a true pt leg but does not:
							List<Leg> legs = TripStructureUtils.getLegs( person.getSelectedPlan() );
							boolean problem = true ;
							for( Leg leg : legs ){
								if ( TransportMode.pt.equals( leg.getMode() ) ) {
									problem = false ;
									break ;
								}
							}
							Assert.assertFalse( problem );

						}
					}
				} ) ;
			}
		} ) ;

		controler.run();
	}

	@Test
	@Ignore // this test is failing because raptor treats "walk" in a special way.  kai, jul'19
	public void networkWalkDoesNotWorkWithRaptor() {
		// test fails with null pointer exception

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
				paramSetXxx.setMaxRadius( 1000000 );
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
//		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.run();
	}



}

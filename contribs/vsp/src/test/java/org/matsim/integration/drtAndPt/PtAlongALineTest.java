package org.matsim.integration.drtAndPt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class PtAlongALineTest{

	private static final Id<Link> TR_LINK_m1_0_ID = Id.createLinkId( "trLinkm1-0" );
	private static final Id<Link> TR_LINK_0_1_ID = Id.createLinkId( "trLink0-1" );
	private static final Id<Link> TR_LONG_LINK_LEFT_ID = Id.createLinkId( "trLinkLongLeft" );
	private static final Id<Link> TR_LINK_MIDDLE_ID = Id.createLinkId( "trLinkMiddle" );
	private static final Id<Link> TR_LONG_LINK_RIGHT_ID = Id.createLinkId( "trLinkLongRight" );
	private static final Id<Link> TR_LINK_LASTM1_LAST_ID = Id.createLinkId( "trLinkLastm1-Last" );
	private static final Id<Link> TR_LINK_LAST_LASTp1_ID = Id.createLinkId( "trLinkLast-Lastp1" ) ;

	private static final Id<TransitStopFacility> tr_stop_fac_0_ID = Id.create( "StopFac0", TransitStopFacility.class );
	private static final Id<TransitStopFacility> tr_stop_fac_10000_ID = Id.create( "StopFac10000", TransitStopFacility.class );
	private static final Id<TransitStopFacility> tr_stop_fac_5000_ID = Id.create( "StopFac5000", TransitStopFacility.class );

	private static final Id<VehicleType> busTypeID = Id.create( "bus", VehicleType.class );


	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Ignore
	@Test
	public void testPtAlongALine() {

		Config config = createConfig( utils.getOutputDirectory() );

		Scenario scenario = createScenario( config, 1000 );

		Controler controler = new Controler( scenario ) ;

		controler.run() ;
	}

	/**
	 * Test of Intermodal Access & Egress to pt using bike.There are three transit stops, and
	 * only the middle stop is accessible by bike.
	 */
	@Ignore
	@Test
	public void testPtAlongALineWithRaptorAndBike() {

		Config config = createConfig( utils.getOutputDirectory() );

		SwissRailRaptorConfigGroup configRaptor = createRaptorConfigGroup(1000000, 1000000);// (radius walk, radius bike)
		config.addModule(configRaptor);

		Scenario scenario = createScenario( config, 1000 );

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule(new SwissRailRaptorModule()) ;

		controler.run() ;
	}

	/**
	 * Test of Drt. 200 drt Vehicles are generated on Link 499-500, and all Agents rely on these
	 * drts to get to their destination
	 */
	@Ignore
	@Test
	public void testDrtAlongALine() {


		Config config = ConfigUtils.createConfig();

		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);

		config.transit().setUseTransit(true) ;


		DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);

		MultiModeDrtConfigGroup multiModeDrtCfg = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class );
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMode("drt_A");
			drtConfig.setStopDuration(60.);
			drtConfig.setMaxWaitTime(900.);
			drtConfig.setMaxTravelTimeAlpha(1.3);
			drtConfig.setMaxTravelTimeBeta(10. * 60.);
			drtConfig.setRejectRequestIfMaxWaitOrTravelTimeViolated( false );
			drtConfig.setChangeStartLinkToLastLinkInSchedule(true);
			multiModeDrtCfg.addParameterSet(drtConfig);
		}

		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getModalElements()) {
			DrtConfigs.adjustDrtConfig(drtCfg, config.planCalcScore(), config.plansCalcRoute() );
		}

		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		{
			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
			stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice);
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
			//
			config.subtourModeChoice().setModes(new String[]{TransportMode.car, "drt_A"});
		}
		{
			StrategyConfigGroup.StrategySettings stratSets = new StrategyConfigGroup.StrategySettings();
			stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
			stratSets.setWeight(1.);
			config.strategy().addStrategySettings(stratSets);
		}

		{
			ModeParams modeParams = new ModeParams("drt_A");
			config.planCalcScore().addModeParams(modeParams);
		}

		{
			ModeParams modeParams = new ModeParams("drt_A_walk");
			config.planCalcScore().addModeParams(modeParams);
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// ---

		final int lastNodeIdx = 1000;
		final double deltaX = 100.;

		createAndAddCarNetwork( scenario, lastNodeIdx, deltaX );

		createAndAddPopulation( scenario , "drt_A", 1000);

		final double deltaY = 1000.;

		createAndAddTransitNetwork( scenario, lastNodeIdx, deltaX, deltaY );

		createAndAddTransitStopFacilities( scenario, lastNodeIdx, deltaX, deltaY );

		createAndAddTransitVehicleType( scenario );

		createAndAddTransitLine( scenario );

		TransitScheduleValidator.printResult( TransitScheduleValidator.validateAll( scenario.getTransitSchedule(), scenario.getNetwork() ) );
		// ---

		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());


		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule() );

		controler.configureQSimComponents(DvrpQSimComponents.activateModes("drt_A"));

		controler.addOverridingModule(
				PtAlongALineTest.createGeneratedFleetSpecificationModule("drt_A", "drtA-", 200,
						Id.createLinkId("499-500"), 4));


		controler.run();
	}

	/**
	 * Test of Intermodal Access & Egress to pt using drt. Only the middle pt station is accessible by
	 * drt, which is set by a StopFilterAttribute
	 */

	@Ignore
	@Test
	public void testPtAlongALineWithRaptorAndDrtStopFilterAttribute() {
		Config config = PtAlongALineTest.createConfig( utils.getOutputDirectory() );

		config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime );
		// yy why?  kai, jun'19


		config.plansCalcRoute().setInsertingAccessEgressWalk( true );
		ModeParams accessWalk = new ModeParams( TransportMode.non_network_walk );
		accessWalk.setMarginalUtilityOfTraveling(0);
		config.planCalcScore().addModeParams(accessWalk);

			// (scoring parameters for drt modes)
			{
				ModeParams modeParams = new ModeParams(TransportMode.drt);
				config.planCalcScore().addModeParams(modeParams);
			}

		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );
		// (as of today, will also influence router. kai, jun'19)

		config.controler().setLastIteration( 0 );

		{
			// (raptor config)

			SwissRailRaptorConfigGroup configRaptor = ConfigUtils.addOrGetModule( config, SwissRailRaptorConfigGroup.class ) ;
			configRaptor.setUseIntermodalAccessEgress(true);

			// drt
			IntermodalAccessEgressParameterSet paramSetDrt = new IntermodalAccessEgressParameterSet();
			paramSetDrt.setMode( TransportMode.drt );
			paramSetDrt.setMaxRadius( 1000000000 );
//			paramSetDrt.setStopFilterAttribute( "drtAccessible" );
//			paramSetDrt.setStopFilterValue( "true" );
			configRaptor.addIntermodalAccessEgress( paramSetDrt );


		}



			DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );
			MultiModeDrtConfigGroup mm = ConfigUtils.addOrGetModule( config, MultiModeDrtConfigGroup.class );
		{
			DrtConfigGroup drtConfig = new DrtConfigGroup();
			drtConfig.setMaxTravelTimeAlpha( 1.3 );
			drtConfig.setMaxTravelTimeBeta( 5. * 60. );
			drtConfig.setStopDuration( 60. );
			drtConfig.setMaxWaitTime( Double.MAX_VALUE );
			drtConfig.setRejectRequestIfMaxWaitOrTravelTimeViolated( false );
			drtConfig.setMode( TransportMode.drt );
			mm.addParameterSet( drtConfig );
		}

		for( DrtConfigGroup drtConfigGroup : mm.getModalElements() ){
			DrtConfigs.adjustDrtConfig( drtConfigGroup, config.planCalcScore(), config.plansCalcRoute() );
		}

		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn );


		Scenario scenario = createScenario(config , 100 );

		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory( DrtRoute.class, new DrtRouteFactory() );

		addModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 0, 1000, TransportMode.drt );


		// The following is for the _router_, not the qsim!  kai, jun'19
		VehiclesFactory vf = scenario.getVehicles().getFactory();
		{
			VehicleType vehType = vf.createVehicleType( Id.create( TransportMode.drt, VehicleType.class ) );
			vehType.setMaximumVelocity( 50./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}{
			VehicleType vehType = vf.createVehicleType( Id.create( TransportMode.car, VehicleType.class ) );
			vehType.setMaximumVelocity( 50./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}

		// ===

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new SwissRailRaptorModule() ) ;

		controler.addOverridingModule( new DvrpModule() );
		controler.addOverridingModule( new MultiModeDrtModule() );
		controler.configureQSimComponents( DvrpQSimComponents.activateModes( TransportMode.drt ) );

		controler.addOverridingModule(
				PtAlongALineTest.createGeneratedFleetSpecificationModule(TransportMode.drt, "DRT-", 10,
						Id.createLinkId("0-1"), 4));


		// This will start otfvis.  Comment out if not needed.
//		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.run();
	}



	static Config createConfig( String outputDir ){
		Config config = ConfigUtils.createConfig() ;

		config.global().setNumberOfThreads( 1 );

		config.controler().setOutputDirectory( outputDir ) ;
		config.controler().setLastIteration( 0 );

		config.plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).setTeleportedModeSpeed( 3. );
		config.plansCalcRoute().getModeRoutingParams().get( TransportMode.bike ).setTeleportedModeSpeed( 10. );

		config.qsim().setEndTime( 24.*3600. );

		config.transit().setUseTransit(true) ;

		// This configures otfvis:
		OTFVisConfigGroup visConfig = ConfigUtils.addOrGetModule( config, OTFVisConfigGroup.class );
		visConfig.setDrawTransitFacilities( false );
		visConfig.setColoringScheme( OTFVisConfigGroup.ColoringScheme.bvg ) ;
		visConfig.setDrawTime(true);
		visConfig.setDrawNonMovingItems(true);
		visConfig.setAgentSize(125);
		visConfig.setLinkWidth(30);
		visConfig.setShowTeleportedAgents( true );
		visConfig.setDrawTransitFacilities( true );
//		{
//			BufferedImage image = null ;
//			Rectangle2D zoomstore = new Rectangle2D.Double( 0., 0., +100.*1000., +10.*1000. ) ;
//			ZoomEntry zoomEntry = new ZoomEntry( image, zoomstore, "*Initial*" ) ;
//			visConfig.addZoom( zoomEntry );
//		}

		config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.kinematicWaves );
		config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.kinematicWaves );

		configureScoring(config);
		return config;
	}

	static Scenario createScenario( Config config, long numberOfPersons ){
		Scenario scenario = ScenarioUtils.createScenario( config );
		// don't load anything

		final int lastNodeIdx = 1000;
		final double deltaX = 100.;

		createAndAddCarNetwork( scenario, lastNodeIdx, deltaX );

		createAndAddPopulation( scenario , "pt", numberOfPersons );

		final double deltaY = 1000.;

		createAndAddTransitNetwork( scenario, lastNodeIdx, deltaX, deltaY );

		createAndAddTransitStopFacilities( scenario, lastNodeIdx, deltaX, deltaY );

		createAndAddTransitVehicleType( scenario );

		createAndAddTransitLine( scenario );

		TransitScheduleValidator.printResult( TransitScheduleValidator.validateAll( scenario.getTransitSchedule(), scenario.getNetwork() ) );
		return scenario;
	}

	private static void configureScoring(Config config) {
		ModeParams accessWalk = new ModeParams( TransportMode.non_network_walk );
		accessWalk.setMarginalUtilityOfTraveling(0);
		config.planCalcScore().addModeParams(accessWalk);

		ModeParams transitWalk = new ModeParams("transit_walk");
		transitWalk.setMarginalUtilityOfTraveling(0);
		config.planCalcScore().addModeParams(transitWalk);

		ModeParams bike = new ModeParams("bike");
		bike.setMarginalUtilityOfTraveling(0);
		config.planCalcScore().addModeParams(bike);

		ModeParams drt = new ModeParams("drt");
		drt.setMarginalUtilityOfTraveling(0);
		config.planCalcScore().addModeParams(drt);
	}

	static SwissRailRaptorConfigGroup createRaptorConfigGroup(int radiusWalk, int radiusBike) {
		SwissRailRaptorConfigGroup configRaptor = new SwissRailRaptorConfigGroup();
		configRaptor.setUseIntermodalAccessEgress(true);

		// Walk
		IntermodalAccessEgressParameterSet paramSetWalk = new IntermodalAccessEgressParameterSet();
		paramSetWalk.setMode(TransportMode.walk);
		paramSetWalk.setMaxRadius(radiusWalk);
		paramSetWalk.setPersonFilterAttribute(null);
		paramSetWalk.setStopFilterAttribute(null);
		configRaptor.addIntermodalAccessEgress(paramSetWalk );

		// Bike
		IntermodalAccessEgressParameterSet paramSetBike = new IntermodalAccessEgressParameterSet();
		paramSetBike.setMode(TransportMode.bike);
		paramSetBike.setMaxRadius(radiusBike);
		paramSetBike.setPersonFilterAttribute(null);
		//		paramSetBike.setStopFilterAttribute("bikeAccessible");
		//		paramSetBike.setStopFilterValue("true");
		configRaptor.addIntermodalAccessEgress(paramSetBike );

		return configRaptor;
	}

	private static void createAndAddTransitLine( Scenario scenario ){
		PopulationFactory pf = scenario.getPopulation().getFactory();
		;
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory tsf = schedule.getFactory();
		VehiclesFactory tvf = scenario.getTransitVehicles().getFactory();

		List<Id<Link>> linkIds = new ArrayList<>() ;
		linkIds.add( TR_LINK_0_1_ID ) ;
		linkIds.add( TR_LONG_LINK_LEFT_ID ) ;
		linkIds.add( TR_LINK_MIDDLE_ID ) ;
		linkIds.add( TR_LONG_LINK_RIGHT_ID ) ;
		linkIds.add( TR_LINK_LASTM1_LAST_ID ) ;
		NetworkRoute route = createNetworkRoute( TR_LINK_m1_0_ID, linkIds, TR_LINK_LAST_LASTp1_ID, pf );

		List<TransitRouteStop> stops = new ArrayList<>() ;
		{
			stops.add( tsf.createTransitRouteStop( schedule.getFacilities().get( tr_stop_fac_0_ID ), 0., 0. ) );
			stops.add( tsf.createTransitRouteStop( schedule.getFacilities().get( tr_stop_fac_5000_ID ), 1., 1. ) );
			stops.add( tsf.createTransitRouteStop( schedule.getFacilities().get( tr_stop_fac_10000_ID ), 1., 1. ) );
		}
		{
			TransitRoute transitRoute = tsf.createTransitRoute( Id.create( "route1", TransitRoute.class ), route, stops, "bus" );
			for ( int ii=0 ; ii<100 ; ii++ ){
				String str = "tr_" + ii ;

				scenario.getTransitVehicles().addVehicle( tvf.createVehicle( Id.createVehicleId( str ), scenario.getTransitVehicles().getVehicleTypes().get( busTypeID) ) );

				Departure departure = tsf.createDeparture( Id.create( str, Departure.class ), 7. * 3600. + ii*300 ) ;
				departure.setVehicleId( Id.createVehicleId( str ) );
				transitRoute.addDeparture( departure );
			}
			TransitLine line = tsf.createTransitLine( Id.create( "line1", TransitLine.class ) );
			line.addRoute( transitRoute );

			schedule.addTransitLine( line );
		}
	}

	private static void createAndAddTransitVehicleType( Scenario scenario ){
		VehiclesFactory tvf = scenario.getTransitVehicles().getFactory();
		VehicleType busType = tvf.createVehicleType( busTypeID );
		{
			VehicleCapacity capacity = busType.getCapacity() ;
			capacity.setSeats( 100 );
		}
		{
			busType.setMaximumVelocity( 100. / 3.6 );
		}
		scenario.getTransitVehicles().addVehicleType( busType );
	}

	private static void createAndAddTransitStopFacilities( Scenario scenario, int lastNodeIdx, double deltaX, double deltaY ){
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory tsf = schedule.getFactory();

		TransitStopFacility stopFacility0 = tsf.createTransitStopFacility( tr_stop_fac_0_ID, new Coord( deltaX, deltaY ), false );
		stopFacility0.setLinkId( TR_LINK_0_1_ID );
		schedule.addStopFacility( stopFacility0 );

		TransitStopFacility stopFacility5000 = tsf.createTransitStopFacility( tr_stop_fac_5000_ID, new Coord( 0.5 * (lastNodeIdx - 1) * deltaX, deltaY ), false );
		stopFacility5000.setLinkId( TR_LINK_MIDDLE_ID );
		stopFacility5000.getAttributes().putAttribute( "drtAccessible", "true" );
		stopFacility5000.getAttributes().putAttribute( "bikeAccessible", "true");

		schedule.addStopFacility( stopFacility5000 );

		TransitStopFacility stopFacility10000 = tsf.createTransitStopFacility( tr_stop_fac_10000_ID, new Coord( (lastNodeIdx - 1) * deltaX, deltaY ), false );
		stopFacility10000.setLinkId( TR_LINK_LASTM1_LAST_ID );
		schedule.addStopFacility( stopFacility10000 );
	}

	private static void createAndAddTransitNetwork( Scenario scenario, int lastNodeIdx, double deltaX, double deltaY ){
		NetworkFactory nf = scenario.getNetwork().getFactory();
		;

		Node nodem1 = nf.createNode( Id.createNodeId("trNodeM1" ), new Coord(-100, deltaY ) );
		scenario.getNetwork().addNode(nodem1);
		// ---
		Node node0 = nf.createNode( Id.createNodeId("trNode0" ), new Coord(0, deltaY ) );
		scenario.getNetwork().addNode(node0);
		createAndAddTransitLink( scenario, nodem1, node0, TR_LINK_m1_0_ID );
		// ---
		Node node1 = nf.createNode( Id.createNodeId("trNode1"), new Coord(deltaX, deltaY ) ) ;
		scenario.getNetwork().addNode( node1 ) ;
		createAndAddTransitLink( scenario, node0, node1, TR_LINK_0_1_ID );
		// ---
		Node nodeMiddleLeft = nf.createNode( Id.createNodeId("trNodeMiddleLeft") , new Coord( 0.5*(lastNodeIdx-1)*deltaX , deltaY ) ) ;
		scenario.getNetwork().addNode( nodeMiddleLeft) ;
		{
			createAndAddTransitLink( scenario, node1, nodeMiddleLeft, TR_LONG_LINK_LEFT_ID );
		}
		// ---
		Node nodeMiddleRight = nf.createNode( Id.createNodeId("trNodeMiddleRight") , new Coord( 0.5*(lastNodeIdx+1)*deltaX , deltaY ) ) ;
		scenario.getNetwork().addNode( nodeMiddleRight ) ;
		createAndAddTransitLink( scenario, nodeMiddleLeft, nodeMiddleRight, TR_LINK_MIDDLE_ID );
		// ---
		Node nodeLastm1 = nf.createNode( Id.createNodeId("trNodeLastm1") , new Coord( (lastNodeIdx-1)*deltaX , deltaY ) ) ;
		scenario.getNetwork().addNode( nodeLastm1) ;
		createAndAddTransitLink( scenario, nodeMiddleRight, nodeLastm1, TR_LONG_LINK_RIGHT_ID );

		// ---
		Node nodeLast = nf.createNode(Id.createNodeId("trNodeLast"), new Coord(lastNodeIdx*deltaX, deltaY ) ) ;
		scenario.getNetwork().addNode(nodeLast);
		createAndAddTransitLink( scenario, nodeLastm1, nodeLast, TR_LINK_LASTM1_LAST_ID );
		// ---
		Node nodeLastp1 = nf.createNode(Id.createNodeId("trNodeLastp1"), new Coord(lastNodeIdx*deltaX+100., deltaY ) ) ;
		scenario.getNetwork().addNode(nodeLastp1);
		createAndAddTransitLink( scenario, nodeLast, nodeLastp1, TR_LINK_LAST_LASTp1_ID );
	}

	private static void createAndAddPopulation( Scenario scenario, String mode, long numberOfPersons ){
		PopulationFactory pf = scenario.getPopulation().getFactory();
		List<ActivityFacility> facilitiesAsList = new ArrayList<>( scenario.getActivityFacilities().getFacilities().values() ) ;
		final Id<ActivityFacility> activityFacilityId = facilitiesAsList.get( facilitiesAsList.size()-1 ).getId() ;
		for( int jj = 0 ; jj < numberOfPersons ; jj++ ){
			Person person = pf.createPerson( Id.createPersonId( jj ) );
			{
				scenario.getPopulation().addPerson( person );
				Plan plan = pf.createPlan();
				person.addPlan( plan );

				// --- 1st location at randomly selected facility:
				int idx = MatsimRandom.getRandom().nextInt( facilitiesAsList.size() );;
				Id<ActivityFacility> homeFacilityId = facilitiesAsList.get( idx ).getId();;
				Activity home = pf.createActivityFromActivityFacilityId( "dummy", homeFacilityId );
				if ( jj==0 ){
					home.setEndTime( 7. * 3600. ); // one agent one sec earlier so that for all others the initial acts are visible in VIA
				} else {
					home.setEndTime( 7. * 3600. + 1. );
				}
				plan.addActivity( home );
				{
					Leg leg = pf.createLeg( mode );
					leg.setDepartureTime( 7. * 3600. );
					leg.setTravelTime( 1800. );
					plan.addLeg( leg );
				}
				{
					Activity shop = pf.createActivityFromActivityFacilityId( "dummy", activityFacilityId );
					plan.addActivity( shop );
				}
			}
		}
	}

	private static void createAndAddCarNetwork( Scenario scenario, int lastNodeIdx, double deltaX ){
		// Construct a network and facilities along a line:
		// 0 --(0-1)-- 1 --(2-1)-- 2 -- ...
		// with a facility of same ID attached to each link.

		NetworkFactory nf = scenario.getNetwork().getFactory();
		ActivityFacilitiesFactory ff = scenario.getActivityFacilities().getFactory();

		Node prevNode;
		{
			Node node = nf.createNode( Id.createNodeId( 0 ), new Coord( 0., 0. ) );
			scenario.getNetwork().addNode( node );
			prevNode = node;
		}
		for( int ii = 1 ; ii <= lastNodeIdx ; ii++ ){
			Node node = nf.createNode( Id.createNodeId( ii ), new Coord( ii * deltaX, 0. ) );
			scenario.getNetwork().addNode( node );
			// ---
			addLinkAndFacility( scenario, nf, ff, prevNode, node );
			addLinkAndFacility( scenario, nf, ff, node, prevNode );
			// ---
			prevNode = node;
		}
	}

	private static void createAndAddTransitLink( Scenario scenario, Node node0, Node node1, Id<Link> TR_LINK_0_1_ID ){
		Link trLink = scenario.getNetwork().getFactory().createLink( TR_LINK_0_1_ID, node0, node1 );
		trLink.setFreespeed( 100. / 3.6 );
		trLink.setCapacity( 100000. );
		scenario.getNetwork().addLink( trLink );
	}

	private static NetworkRoute createNetworkRoute( Id<Link> startLinkId, List<Id<Link>> linkIds, Id<Link> endLinkId, PopulationFactory pf ){
		NetworkRoute route = pf.getRouteFactories().createRoute( NetworkRoute.class, startLinkId, endLinkId ) ;
		route.setLinkIds( startLinkId, linkIds, endLinkId ) ;
		return route;
	}

	private static void addLinkAndFacility( Scenario scenario, NetworkFactory nf, ActivityFacilitiesFactory ff, Node prevNode, Node node ){
		final String str = prevNode.getId() + "-" + node.getId();
		Link link = nf.createLink( Id.createLinkId( str ), prevNode, node ) ;
		Set<String> set = new HashSet<>() ;
		set.add("car" ) ;
		link.setAllowedModes( set ) ;
		link.setLength( CoordUtils.calcEuclideanDistance( prevNode.getCoord(), node.getCoord() ) );
		link.setCapacity( 3600. );
		link.setFreespeed( 50./3.6 );
		scenario.getNetwork().addLink( link );
		// ---
		ActivityFacility af = ff.createActivityFacility( Id.create( str, ActivityFacility.class ), link.getCoord(), link.getId() ) ;
		ActivityOption option = ff.createActivityOption( "shop" ) ;
		af.addActivityOption( option );
		scenario.getActivityFacilities().addActivityFacility( af );
	}

	static AbstractDvrpModeModule createGeneratedFleetSpecificationModule(String mode, String vehPrefix,
			int numberofVehicles, Id<Link> startLinkId, int capacity) {
		return new AbstractDvrpModeModule(mode) {
			@Override
			public void install() {
				bindModal(FleetSpecification.class).toProvider(
						() -> PtAlongALineTest.createDrtFleetSpecifications(vehPrefix, numberofVehicles, startLinkId,
								capacity)).asEagerSingleton();
			}
		};
	}

	static FleetSpecification createDrtFleetSpecifications(String vehPrefix, int numberofVehicles, Id<Link> startLinkId,
			int capacity) {
		FleetSpecification fleetSpecification = new FleetSpecificationImpl();
		for (int i = 0; i < numberofVehicles; i++) {
			//for multi-modal networks: Only links where drts can ride should be used.
			fleetSpecification.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create(vehPrefix + i, DvrpVehicle.class))
					.startLinkId(startLinkId)
					.capacity(capacity)
					.serviceBeginTime(0)
					.serviceEndTime(36 * 3600)
					.build());
		}
		return fleetSpecification;
	}

	static void addModeToAllLinksBtwnGivenNodes( Network network, int fromNodeNumber, int toNodeNumber, String drtMode ) {
		for (int i = fromNodeNumber; i < toNodeNumber; i++) {
			Set<String> newAllowedModes = new HashSet<>( network.getLinks().get( Id.createLinkId( i + "-" + (i + 1) ) ).getAllowedModes() );
			newAllowedModes.add(drtMode);
			network.getLinks().get(Id.createLinkId( i + "-" + (i+1) )).setAllowedModes( newAllowedModes );

			newAllowedModes = new HashSet<>( network.getLinks().get( Id.createLinkId( (i + 1) + "-" + i ) ).getAllowedModes() );
			newAllowedModes.add(drtMode);
			network.getLinks().get(Id.createLinkId( (i+1) + "-" + i )).setAllowedModes( newAllowedModes );
		}
	}

}

package org.matsim.codeexamples.pt;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

import java.util.HashSet;
import java.util.Set;

public class PtAlongALine2Test{

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	enum DrtMode { none, teleportBeeline, teleportBasedOnNetworkRoute, full }
	private DrtMode drtMode = DrtMode.teleportBasedOnNetworkRoute ;

	@Test
	public void testPtAlongALineWithRaptorAndDrtServiceArea() {


		Config config = PtAlongALineTest.createConfig( utils.getOutputDirectory() );

		config.qsim().setSimStarttimeInterpretation( QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime );
		// yy why?  kai, jun'19

		switch ( drtMode ) {
			case none:
				break;
			case teleportBeeline:
			{
				PlansCalcRouteConfigGroup.ModeRoutingParams pars = new PlansCalcRouteConfigGroup.ModeRoutingParams();
				pars.setMode( TransportMode.drt );
				pars.setTeleportedModeSpeed( 100. / 3.6 );
				config.plansCalcRoute().addModeRoutingParams( pars );
			}
//			{
//				PlansCalcRouteConfigGroup.ModeRoutingParams pars = new PlansCalcRouteConfigGroup.ModeRoutingParams();
//				pars.setMode( "drt2" );
//				pars.setTeleportedModeSpeed( 100. / 3.6 );
//				config.plansCalcRoute().addModeRoutingParams( pars );
//			}
			break;
			case teleportBasedOnNetworkRoute: {
//				config.plansCalcRoute().removeModeRoutingParams( TransportMode.walk );
				Set<String> networkModes = new HashSet<>( config.plansCalcRoute().getNetworkModes() );
//				networkModes.add( TransportMode.walk ) ;
				networkModes.add( TransportMode.drt );
				networkModes.add( "drt2" );
				config.plansCalcRoute().setNetworkModes( networkModes );
				break; }
			case full:
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + drtMode );
		}

		config.plansCalcRoute().setInsertingAccessEgressWalk( true );

		switch( drtMode ) {
			case none:
				break;
			case teleportBeeline:
			case teleportBasedOnNetworkRoute:
			case full:
			{
				PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.drt);
				config.planCalcScore().addModeParams(modeParams);
			}
			{
				PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams("drt2");
				config.planCalcScore().addModeParams(modeParams);
			}
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + drtMode );
		}


		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );
		// (as of today, will also influence router. kai, jun'19)

		config.controler().setLastIteration( 0 );

		{
			SwissRailRaptorConfigGroup configRaptor = ConfigUtils.addOrGetModule( config, SwissRailRaptorConfigGroup.class ) ;

			// Walk
//			SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet paramSetWalk = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
//			paramSetWalk.setMode(TransportMode.walk);
//			paramSetWalk.setRadius(1000000);
//			paramSetWalk.setPersonFilterAttribute(null);
//			paramSetWalk.setStopFilterAttribute(null);
//			configRaptor.addIntermodalAccessEgress(paramSetWalk );
			// (in principle, walk as alternative to drt will not work, since drt is always faster.  Need to give the ASC to the router!  However, with
			// the reduced drt network we should be able to see differentiation.)

			if ( drtMode!=DrtMode.none){

				configRaptor.setUseIntermodalAccessEgress(true);

				// drt
				SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet paramSetDrt = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
				paramSetDrt.setMode( TransportMode.drt );
				paramSetDrt.setRadius( 1000000 );
//				paramSetDrt.setStopFilterAttribute( DRT_ACCESSIBLE );
//				paramSetDrt.setStopFilterValue( "true" );
				configRaptor.addIntermodalAccessEgress( paramSetDrt );

//				// drt2
//				SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet paramSetDrt2 = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
//				paramSetDrt2.setMode( "drt2" );
//				paramSetDrt2.setRadius( 1000000 );
//				paramSetDrt2.setPersonFilterAttribute( null );
//				paramSetDrt2.setStopFilterAttribute( null );
//				configRaptor.addIntermodalAccessEgress( paramSetDrt2 );

			}

		}


		String drtVehiclesFile = "drt_vehicles.xml";
		String drt2VehiclesFile = "drt2_vehicles.xml";
		if ( drtMode==DrtMode.full ){

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
				drtConfig.setMode( TransportMode.drt );
				mm.addParameterSet( drtConfig );
			}
//			{
//				DrtConfigGroup drtConfig = new DrtConfigGroup();
//				drtConfig.setMaxTravelTimeAlpha( 1.3 );
//				drtConfig.setVehiclesFile( drt2VehiclesFile );
//				drtConfig.setMaxTravelTimeBeta( 5. * 60. );
//				drtConfig.setStopDuration( 60. );
//				drtConfig.setMaxWaitTime( Double.MAX_VALUE );
//				drtConfig.setMode( "drt2" );
//				mm.addParameterSet( drtConfig );
//			}
//
			for( DrtConfigGroup drtConfigGroup : mm.getModalElements() ){
				DrtConfigs.adjustDrtConfig( drtConfigGroup, config.planCalcScore() );
			}
		}

		// ---

		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn );

		// ===

		Scenario scenario = PtAlongALineTest.createScenario(config , 100 );

		if ( drtMode==DrtMode.full ) {
				scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory( DrtRoute.class, new DrtRouteFactory() );
				// TODO: avoid really writing out these files. However so far it is unclear how
				// to configure DRT and load the vehicles otherwise
				PtAlongALineTest.createDrtVehiclesFile(drtVehiclesFile, "DRT-", 10, Id.createLinkId("0-1" ) );
				PtAlongALineTest.createDrtVehiclesFile(drt2VehiclesFile, "DRT2-", 1, Id.createLinkId("1000-999" ) );
		}

//		// TODO: reference somehow network creation, to ensure that these link ids exist
//		// add drt modes to the car links' allowed modes in their respective service area
		PtAlongALineTest.addDrtModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 0, 1000, TransportMode.drt );
//		PtAlongALineTest.addDrtModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 950, 1000, "drt2" );


		// The following is for the _router_, not the qsim!  kai, jun'19
		VehiclesFactory vf = scenario.getVehicles().getFactory();
		{
			VehicleType vehType = vf.createVehicleType( Id.create( "drt2", VehicleType.class ) );
			vehType.setMaximumVelocity( 50./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}
		{
			VehicleType vehType = vf.createVehicleType( Id.create( TransportMode.drt, VehicleType.class ) );
			vehType.setMaximumVelocity( 50./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}
		{
			VehicleType vehType = vf.createVehicleType( Id.create( TransportMode.car, VehicleType.class ) );
			vehType.setMaximumVelocity( 50./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}
		{
			VehicleType vehType = vf.createVehicleType( Id.create( TransportMode.walk, VehicleType.class ) );
			vehType.setMaximumVelocity( 4./3.6 );
			scenario.getVehicles().addVehicleType( vehType );
		}

		// ===

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new SwissRailRaptorModule() ) ;

		if ( drtMode==DrtMode.full ){
			controler.addOverridingModule( new DvrpModule() );
			controler.addOverridingModule( new MultiModeDrtModule() );
//			controler.configureQSimComponents( DvrpQSimComponents.activateModes( TransportMode.drt, "drt2" ) );
			controler.configureQSimComponents( DvrpQSimComponents.activateModes( TransportMode.drt ) );
		}

		// This will start otfvis.  Comment out if not needed.
		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.run();
	}



}

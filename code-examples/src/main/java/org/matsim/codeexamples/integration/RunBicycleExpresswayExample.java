package org.matsim.codeexamples.integration;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.AccessEgressType;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.util.Arrays;
import java.util.HashSet;

/**
 * This example does not use the bicycle contrib.  It instead plugs things together manually, to show how it is done, but simpler than the bicycle
 * contrib.  For an example that uses the Bicycle contrib see {@link org.matsim.codeexamples.extensions.bicycle.RunBicycleContribExample} or {@link
 * org.matsim.contrib.bicycle.run.RunBicycleExample}
 */
public final class RunBicycleExpresswayExample{
	private static final Logger log = LogManager.getLogger( RunBicycleExpresswayExample.class );

	/**
	 * There is a {@link TransportMode#bike}.  We are deliberately not using that here in order to avoid picking up any possible default for that mode.  kai, jan'23
	 */
	private static final String BICYCLE="bicycle";
	private static final String IS_BICYCLE_EXPRESSWAY = "isBicycleExpressway";

	public static void main ( String [] args ) {

		Config config ;
		if ( args != null && args.length>=1 ) {
			config = ConfigUtils.loadConfig( args ) ;
		} else {
			config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) ) ;
		}

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		// set traffic dynamics and visualization to kinematic waves (should be matsim default, but currently (jan'23) is not):
		config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.kinematicWaves );
		config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.kinematicWaves );

		// run only the zeroth iteration so that we do not have to worry about replanning strategies:
		config.controler().setLastIteration( 0 );

		// add bike routing as network routing:
		config.plansCalcRoute().setNetworkModes( Arrays.asList( TransportMode.car, BICYCLE ) );

		config.plansCalcRoute().setAccessEgressType( AccessEgressType.accessEgressModeToLink );
		// (NetworkRoutingModule w/o access/egress does not pass the vehicle to the router --> cannot take vehicle max speed into account.  See comment in code in NetworkRoutingModule#calcRoute(...).  kai, jan'23)

		// add (arbitrary) mode params for bike so that the scoring works:
		config.planCalcScore().addModeParams( new PlanCalcScoreConfigGroup.ModeParams( BICYCLE ) );

		// add bike as network mode to qsim:
		config.qsim().setMainModes( Arrays.asList( TransportMode.car, BICYCLE ) );

		// ---

		Scenario scenario = ScenarioUtils.loadScenario( config );

		for( Link link : scenario.getNetwork().getLinks().values() ){
			// allow all links both for car and for bike:
			link.setAllowedModes( new HashSet<>( Arrays.asList( TransportMode.car, BICYCLE ) ) ) ;

			// label one link as bicycle expressway (to be picked up later):
			if( link.getId().toString().equals( "7" ) ) {
				link.getAttributes().putAttribute( IS_BICYCLE_EXPRESSWAY, true );
			}
		}
		for( Person person : scenario.getPopulation().getPersons().values() ){
			for( PlanElement planElement : person.getSelectedPlan().getPlanElements() ){
				if ( planElement instanceof Leg ) {
					// set all legs to bicycle mode and remove the route to force recomputation.  This way, we do not have to worry about a mode choice strategy.
					((Leg) planElement).setMode( BICYCLE );
					((Leg) planElement).setRoute( null );
				}
			}
		}

		// set config such that the mode vehicles come from vehicles data:
		scenario.getConfig().qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );

		// now put hte mode vehicles into the vehicles data:
		final VehiclesFactory vf = VehicleUtils.getFactory();
		scenario.getVehicles().addVehicleType( vf.createVehicleType( Id.create(TransportMode.car, VehicleType.class) ).setMaximumVelocity( 100./3.6 ).setPcuEquivalents( 1. ) );
		scenario.getVehicles().addVehicleType( vf.createVehicleType( Id.create(BICYCLE, VehicleType.class) ).setNetworkMode( BICYCLE ).setMaximumVelocity(10./3.6 ).setPcuEquivalents( 0.25 ) );

		// ---

		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new OTFVisLiveModule() );
		ConfigUtils.addOrGetModule( config, OTFVisConfigGroup.class ).setDrawNonMovingItems( true ).setAgentSize( 100 );

		controler.addOverridingModule( new AbstractModule(){
			// preparation: compute max speed given link speed limit and vehicle maximum speed:
			private double getMaxSpeedFromVehicleAndLink( Link link, double time, Vehicle vehicle ) {

				double maxSpeedFromLink = link.getFreespeed( time );

				double maxSpeedFromVehicle = vehicle.getType().getMaximumVelocity();
				final Boolean isBicycleExpressway = (Boolean) link.getAttributes().getAttribute( IS_BICYCLE_EXPRESSWAY );
				if (isBicycleExpressway!=null && isBicycleExpressway && vehicle.getType().getNetworkMode().equals( BICYCLE )){
					// under normal circumstances, the bicycle vehicle type has a lowish maximum speed (see earlier).  On bicycle expressways, we increase it:
					maxSpeedFromVehicle = 25. / 3.6;
				}

				// as usual, we return the min of all the speeds:
				return Math.min ( maxSpeedFromLink, maxSpeedFromVehicle );
			}

			@Override public void install(){

				// set meaningful travel time binding for routing:
				this.addTravelTimeBinding( BICYCLE ).toInstance( new TravelTime(){
					@Inject @Named(BICYCLE) TravelTimeCalculator bikeCalculator ;
					// (not very obvious why this is the correct syntax.  kai, jan'23)

					@Override public double getLinkTravelTime( Link link, double time, Person person, Vehicle vehicle ){

						// we get the max speed from vehicle and link, as defined in the preparation above:
						final double maxSpeedFromVehicleAndLink = getMaxSpeedFromVehicleAndLink( link, time, vehicle );

						// we also get the speed from observation:
						double speedFromObservation = bikeCalculator.getLinkTravelTimes().getLinkTravelTime( link, time, person, vehicle );

						// we compute the min of the two:
						double actualSpeed = Math.min( speedFromObservation, maxSpeedFromVehicleAndLink );

						// the link travel time is computed from that speed:
						return link.getLength()/actualSpeed ;
					}
				} );

				// make the qsim such that bicycle son bicycle expressways are faster than their normal speed:
				this.installOverridingQSimModule( new AbstractQSimModule(){
					@Inject EventsManager events;
					@Inject Scenario scenario;
					@Override protected void configureQSim(){
						// instantiate the configurable network factory:
						final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);

						// set the speed calculation as declared above in the preparation:
						factory.setLinkSpeedCalculator( ( qVehicle, link, time ) -> getMaxSpeedFromVehicleAndLink( link, time, qVehicle.getVehicle() ) );

						// set (= overwrite) the QNetworkFactory with the factory defined here:
						bind( QNetworkFactory.class ).toInstance(factory );
						// (this is a bit dangerous since other pieces of code might overwrite the QNetworkFactory as well.  In the longer run, need to find a different solution.)
					}
				} );
				// (without this, 1st bicycle at node at 7:31, with this, at 6:55.)
			}
		} ) ;

		// ---

		controler.run() ;

	}

}

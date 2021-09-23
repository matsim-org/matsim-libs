/* *********************************************************************** *
 * project: org.matsim.*
 * EditRoutesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


package playground.vsp.pt.ptdisturbances;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.examples.ExamplesUtils;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.TransitScheduleChangedEvent;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.withinday.utils.EditTrips;
import org.matsim.withinday.utils.ReplanningException;

import com.google.inject.Inject;

/**
* @author smueller, gleich
*/

public class RunExamplePtDisturbances {
	private static final Logger log = Logger.getLogger( RunExamplePtDisturbances.class ) ;
	private static final URL configURL = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("ptdisturbances"),"config.xml");

	public static void main(String[] args) {
		if ( args==null || args.length == 0 ){
			run( ConfigUtils.loadConfig( configURL ) );
		} else {
			run( ConfigUtils.loadConfig(  args ) ) ;
		}
	}
	
	static void adaptConfig(Config config) {
		config.transit().setBoardingAcceptance( TransitConfigGroup.BoardingAcceptance.checkStopOnly );

		config.qsim().setSnapshotStyle( QSimConfigGroup.SnapshotStyle.kinematicWaves );

		// This configures otfvis:
		OTFVisConfigGroup visConfig = ConfigUtils.addOrGetModule( config, OTFVisConfigGroup.class );
		visConfig.setDrawTransitFacilities( false );
		visConfig.setColoringScheme( OTFVisConfigGroup.ColoringScheme.bvg ) ;
		visConfig.setDrawTime(true);
		visConfig.setDrawNonMovingItems(true);
		visConfig.setAgentSize(125);
		visConfig.setLinkWidth(10);
		visConfig.setShowTeleportedAgents( true );
	}
	
	public static Controler prepareControler( Scenario scenario ) {

		Controler controler = new Controler( scenario ) ;
		{
			QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( controler.getConfig(), QSimComponentsConfigGroup.class );

			// the following requests that a component registered under the name "...NAME" will be used:
			List<String> cmps = qsimComponentsConfig.getActiveComponents();
			cmps.add( DisturbanceAndReplanningEngine.NAME );
			qsimComponentsConfig.setActiveComponents( cmps );

			controler.addOverridingQSimModule( new AbstractQSimModule(){
				@Override
				protected void configureQSim(){
					// the following registers the component under the name "...NAME":
					this.addQSimComponentBinding( DisturbanceAndReplanningEngine.NAME ).to( DisturbanceAndReplanningEngine.class );
				}
			} );
		}

		return controler;
	}

	/*
	 * General settings for this and similar scenarios moved into static methods.
	 * Settings specific to this scenario in here.
	 */
	static void run(Config config) {
		
		adaptConfig(config);
		config.network().setTimeVariantNetwork(true);
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
//		Time variant network is used to depict the disturbance
		NetworkChangeEvent networkChangeEvent1 = new NetworkChangeEvent(7.5*3600);
		Link link = scenario.getNetwork().getLinks().get(Id.createLinkId("pt6b"));
		networkChangeEvent1.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, link.getLength()/3600));
		networkChangeEvent1.addLink(link);
		NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), networkChangeEvent1);
		NetworkChangeEvent networkChangeEvent2 = new NetworkChangeEvent(8.5*3600);
		networkChangeEvent2.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, 30. / 3.6));
		networkChangeEvent2.addLink(link);
		NetworkUtils.addNetworkChangeEvent(scenario.getNetwork(), networkChangeEvent2);

		// ---
		Controler controler = prepareControler( scenario );
		
		// This will print the event onto the console/into the logfile.  Sometimes useful for debugging.
		controler.addOverridingModule( new AbstractModule(){
			@Override
			public void install(){
				this.addEventHandlerBinding().toInstance( new BasicEventHandler(){
					@Override
					public void handleEvent( Event event ){
						if ( event instanceof HasPersonId && ((HasPersonId) event).getPersonId().toString().equals( "b1810066" ) ){
							log.info( event.toString() );
						}
					}
				} );
			}
		} );

		// This will start otfvis.  Comment out if not needed.
		controler.addOverridingModule( new OTFVisLiveModule() );
		controler.run();
	}

	private static class DisturbanceAndReplanningEngine implements MobsimEngine{
		public static final String NAME="disturbanceAndReplanningEngine" ;

		@Inject private Scenario scenario;
		@Inject private EventsManager events;
		@Inject private Provider<TripRouter> tripRouterProvider ;
		private InternalInterface internalInterface;

		@Override public void doSimStep( double now ) {
			
			// replan after an affected bus has already departed -> pax on the bus are replanned to get off earlier
			double replanTime = 7.5 * 3600.; 

			if ( (int) now == replanTime -1. ){ // yyyyyy this needs to come one sec earlier. :-(
				// clear transit schedule from transit router provider:
				events.processEvent( new TransitScheduleChangedEvent( now ) );
			}

			if ( (int) now == replanTime ){

				// modify transit schedule:

				final Id<TransitLine> disturbedLineId = Id.create( "2", TransitLine.class );
				TransitLine disturbedLine = scenario.getTransitSchedule().getTransitLines().get( disturbedLineId ) ;
				Gbl.assertNotNull(disturbedLine);

				TransitRoute disturbedRoute = disturbedLine.getRoutes().get( Id.create("345", TransitRoute.class ) );
				Gbl.assertNotNull( disturbedRoute );

				log.warn("before removal: nDepartures=" + disturbedRoute.getDepartures().size() ) ;

				List<Departure> toRemove = new ArrayList<>() ;
				for( Departure departure : disturbedRoute.getDepartures().values() ){
					if ( departure.getDepartureTime() >= 7.5*3600. && departure.getDepartureTime() < 8.5*3600.) {
						toRemove.add( departure ) ;
					}
				}
				for( Departure departure : toRemove ){
					disturbedRoute.removeDeparture( departure ) ;
				}

				log.warn("after removal: nDepartures=" + disturbedRoute.getDepartures().size() ) ;

				// ---

				replanPtPassengers(now, disturbedLineId, tripRouterProvider, scenario, internalInterface);

			}
		}

		@Override
		public void onPrepareSim(){
		}

		@Override
		public void afterSim(){
		}

		@Override
		public void setInternalInterface( InternalInterface internalInterface ){
			this.internalInterface = internalInterface ;
		}

	}
	
	static void replanPtPassengers(double now, final Id<TransitLine> disturbedLineId, Provider<TripRouter> tripRouterProvider, Scenario scenario, InternalInterface internalInterface) {
		
		final QSim qsim = internalInterface.getMobsim() ;

		// force new transit router:
		final TripRouter tripRouter = tripRouterProvider.get();
		EditTrips editTrips = new EditTrips( tripRouter, scenario, internalInterface, TimeInterpretation.create(scenario.getConfig()) );;

		// find the affected agents and replan affected trips:
		
		for( MobsimAgent agent : (qsim).getAgents().values() ){
			if( agent instanceof TransitDriverAgentImpl ){
				/* This is a pt vehicle driver. TransitDriverAgentImpl does not support getModifiablePlan(...). So we should skip him.
				 * This probably means that the driver continues driving the pt vehicle according to the old schedule.
				 * However, this cannot be resolved by the editTrips.replanCurrentTrip() method anyway.
				 */
				continue;
			}

			Plan plan = WithinDayAgentUtils.getModifiablePlan( agent );

			{
				PlanElement pe = WithinDayAgentUtils.getCurrentPlanElement( agent );
				if ( pe instanceof Activity ) {
					if ( StageActivityTypeIdentifier.isStageActivity( ((Activity) pe).getType() ) ) {
						log.warn( "agent with ID=" + agent.getId() + " is at stage activity of type=" + ((Activity) pe).getType() ) ;
					} else {
						log.warn( "agent with ID=" + agent.getId() + " is at real activity of type=" + ((Activity) pe).getType() ) ;
					}
				} else if ( pe instanceof Leg ) {
					log.warn( "agent with ID=" + agent.getId() + " is at leg with mode=" + ((Leg) pe).getMode() ) ;
				}
			}

			int currentPlanElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex( agent );

			TripStructureUtils.Trip currentTrip;

			try{
				currentTrip = EditTrips.findCurrentTrip( agent );
			} catch( ReplanningException e ){
				// The agent might not be on a trip at the moment (but at a "real" activity).
				currentTrip = null;
			}
			
			Activity nextRealActivity = null; // would be nicer to use TripStructureUtils to find trips, but how can we get back to the original plan to modify it?


			for( int ii = currentPlanElementIndex ; ii < plan.getPlanElements().size() ; ii++ ){
				PlanElement pe = plan.getPlanElements().get( ii );
				// Replan each trip at maximum once, otherwise bad things might happen.
				// So we either have to keep track which Trip has already been re-planned 
				// or move on manually to the next real activity after we re-planned.
				// Trips seem hard to identify, so try the latter approach.
				// Replanning the same trip twice could happen e.g. if first replanCurrentTrip is called and keeps or re-inserts 
				// a leg with the disturbed line. So on a later plan element (higher ii) of the same trip replanCurrentTrip or 
				// replanFutureTrip might be called. - gl, jul '19
				if (nextRealActivity != null) {
					// we are trying to move on to the next trip in order not to replan twice the same trip
					if( pe instanceof Activity && nextRealActivity.equals((Activity) pe)) {
							nextRealActivity = null;
						}
					// continue to next pe if we still are on the trip we just replanned.
					continue;
				} else if( pe instanceof Leg ){
					Leg leg = (Leg) pe;
					if( leg.getMode().equals( TransportMode.pt ) ){
						TransitPassengerRoute transitRoute = (TransitPassengerRoute) leg.getRoute();
						if( transitRoute.getLineId().equals( disturbedLineId ) ){
							TripStructureUtils.Trip affectedTrip = editTrips.findTripAtPlanElement( agent, pe );
							if( currentTrip != null && currentTrip.getTripElements().contains( pe ) ){
								// current trip is disturbed
								editTrips.replanCurrentTrip( agent, now, TransportMode.pt );
//								break;
							} else {
								// future trip is disturbed
								editTrips.replanFutureTrip( affectedTrip, plan, TransportMode.pt );
							}
							nextRealActivity = affectedTrip.getDestinationActivity();
						}
					}
				}
			}

			{
				// agents that abort their leg before boarding a vehicle need to be actively advanced:
				PlanElement pe = WithinDayAgentUtils.getCurrentPlanElement( agent );
				if ( pe instanceof Activity ) {
					if ( StageActivityTypeIdentifier.isStageActivity( ((Activity) pe).getType() ) ){
						internalInterface.arrangeNextAgentState( agent );
						internalInterface.unregisterAdditionalAgentOnLink( agent.getId(), agent.getCurrentLinkId() ) ;
					}
				}
				// yyyyyy would be much better to hide this inside EditXxx. kai, jun'19
			}

		}
	}
	
	

}

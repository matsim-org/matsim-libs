package org.matsim.contrib.freight.controler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.events.FreightEventCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This keeps track of a scheduledTour during simulation and can thus be seen as the driver of the vehicle that runs the tour.
 *
 * <p>In addition, the driver knows which planElement is associated to a shipment and service, respectively.
 *
 * @author mzilske, sschroeder
 */
final class CarrierDriverAgent{
	private static final Logger log = LogManager.getLogger( CarrierDriverAgent.class );

	private Leg currentLeg;

	private Activity currentActivity;

	private List<Id<Link>> currentRoute;

	private final Id<Person> driverId;

	private final ScheduledTour scheduledTour;

	private int planElementCounter = 0;
	private final ScoringFunction scoringFunction;
	private final Carrier carrier;
	private final EventsManager events;
	private final Collection<FreightEventCreator> freightEventCreators;

	private final Driver2VehicleEventHandler driver2EventHandler = new Driver2VehicleEventHandler() ;



	CarrierDriverAgent( Id<Person> driverId, ScheduledTour tour, ScoringFunction scoringFunction, Carrier carrier,
			    EventsManager events, Collection<FreightEventCreator> freightEventCreators){
		this.scoringFunction = scoringFunction;
		this.carrier = carrier;
		this.events = events;
		this.freightEventCreators = freightEventCreators;
		log.debug( "creating CarrierDriverAgent with driverId=" + driverId );
		this.driverId = driverId;
		this.scheduledTour = tour;
	}

	void handleAnEvent(Event event){
		// the event comes to here from CarrierAgent#handleEvent only for events concerning this driver

		int previousPlanElementCounter = this.planElementCounter;

		if( event instanceof PersonArrivalEvent ){
			handleEvent( (PersonArrivalEvent) event);
		} else if( event instanceof PersonDepartureEvent ){
			handleEvent( (PersonDepartureEvent) event );
		} else if( event instanceof LinkEnterEvent ){
			handleEvent( (LinkEnterEvent) event );
		} else if( event instanceof ActivityEndEvent ){
			handleEvent( (ActivityEndEvent) event );
		} else if( event instanceof ActivityStartEvent ){
			handleEvent( (ActivityStartEvent) event );
		} else{
			createAdditionalEvents( event, null, scheduledTour, driverId, planElementCounter);
		}
	}

	private void handleEvent( PersonArrivalEvent event ){
		currentLeg.setTravelTime( event.getTime() - currentLeg.getDepartureTime().seconds() );
		double travelTime = currentLeg.getDepartureTime().seconds()
						    + currentLeg.getTravelTime().seconds() - currentLeg.getDepartureTime().seconds();
		currentLeg.setTravelTime( travelTime );
		if( currentRoute.size() > 1 ){
			NetworkRoute networkRoute = RouteUtils.createNetworkRoute( currentRoute );
			networkRoute.setTravelTime( travelTime );
			networkRoute.setVehicleId( getVehicle().getId() );
			currentLeg.setRoute( networkRoute );
			currentRoute = null;
		} else{
			Id<Link> startLink;
			if( currentRoute.size() != 0 ){
				startLink = currentRoute.get( 0 );
			} else{
				startLink = event.getLinkId();
			}
			Route genericRoute = RouteUtils.createGenericRouteImpl( startLink, event.getLinkId() );
			genericRoute.setDistance( 0.0 );
			currentLeg.setRoute( genericRoute );
		}
		if( scoringFunction != null ){
			scoringFunction.handleLeg( currentLeg );
		}
		createAdditionalEvents( event, null, scheduledTour, driverId, planElementCounter );
	}

	private void handleEvent( PersonDepartureEvent event ){
		Leg leg = PopulationUtils.createLeg( event.getLegMode() );
		leg.setDepartureTime( event.getTime() );
		currentLeg = leg;
		currentRoute = new ArrayList<>();
		createAdditionalEvents( event, null, scheduledTour, driverId, planElementCounter );
	}

	private void handleEvent( LinkEnterEvent event ){
		if( scoringFunction != null ){
			scoringFunction.handleEvent( new LinkEnterEvent( event.getTime(), getVehicle().getId(), event.getLinkId() ) );
		}
		currentRoute.add( event.getLinkId() );
		createAdditionalEvents( event, null, scheduledTour, driverId, planElementCounter );
	}

	private void handleEvent( ActivityEndEvent event ){
		if( currentActivity == null ){
			Activity firstActivity = PopulationUtils.createActivityFromLinkId( event.getActType(), event.getLinkId() );
			firstActivity.setFacilityId( event.getFacilityId() );
			currentActivity = firstActivity;
		}
		currentActivity.setEndTime( event.getTime() );
		if( scoringFunction != null ){
			scoringFunction.handleActivity( currentActivity );
		}

		createAdditionalEvents( event, currentActivity, scheduledTour, driverId, planElementCounter );

		log.debug( "handling activity end event=" + event );
		if( FreightConstants.START.equals( event.getActType() ) ){
			planElementCounter += 1;
			return;
		}
		if( FreightConstants.END.equals( event.getActType() ) ) return;
		if( FreightConstants.PICKUP.equals( event.getActType() ) ){
			planElementCounter += 2;
		} else if( FreightConstants.DELIVERY.equals( event.getActType() ) ){
			planElementCounter += 2;
		} else{
			planElementCounter += 2;
		}
	}

	private void handleEvent( ActivityStartEvent event ){
		Activity activity = PopulationUtils.createActivityFromLinkId( event.getActType(), event.getLinkId() );
		activity.setFacilityId( event.getFacilityId() );
		activity.setStartTime( event.getTime() );
		if( event.getActType().equals( FreightConstants.END ) ){
			activity.setEndTimeUndefined();
			if( scoringFunction != null ){
				scoringFunction.handleActivity( activity );
			}
		} else{
			Tour.TourActivity tourActivity = getTourActivity();
			if( !activity.getLinkId().toString().equals( tourActivity.getLocation().toString() ) )
				throw new AssertionError( "linkId of activity is not equal to linkId of tourActivity. This must not be." );
			currentActivity = new FreightActivity( activity, tourActivity.getTimeWindow() );
		}
		createAdditionalEvents( event, currentActivity, scheduledTour, driverId, planElementCounter );
		// yyyyyy uses the previous activity, not the current (end) activity.  Bug or feature?  Only used by LSP, not by carrier.  kai, jul'22
	}

	private void createAdditionalEvents( Event event, Activity activity, ScheduledTour scheduledTour, Id<Person> driverId, int activityCounter){
//		if( scoringFunction == null ){
			// (means "called from LSP".  kai, jul'22)

		driver2EventHandler.handleAnEvent(event);
		Id<Vehicle> vehicleId = driver2EventHandler.getVehicleOfDriver(driverId);

			// Reason why this here is needed is that the more informative objects such as ScheduledTour cannot be
			// filled from just listening to events.  kai, jul'22
			for( FreightEventCreator freightEventCreator : freightEventCreators) {
				Event freightEvent = freightEventCreator.createEvent( event, carrier, activity, scheduledTour, activityCounter, vehicleId );
				if(freightEvent != null) {
					this.events.processEvent( freightEvent );
					if( scoringFunction != null ){
						scoringFunction.handleEvent( freightEvent );
					}
				}
			}
//		}
	}

	private Tour.TourActivity getTourActivity(){
		return (Tour.TourActivity) this.scheduledTour.getTour().getTourElements().get( planElementCounter );
	}

	CarrierVehicle getVehicle(){
		return scheduledTour.getVehicle();
	}

	Tour.TourElement getPlannedTourElement( int elementIndex ){
		int index = elementIndex - 1;
		int elementsSize = scheduledTour.getTour().getTourElements().size();
		if( index < 0 ) return scheduledTour.getTour().getStart();
		else if( index == elementsSize ) return scheduledTour.getTour().getEnd();
		else if( index < elementsSize ){
			return scheduledTour.getTour().getTourElements().get( index );
		} else throw new IllegalStateException( "index out of bounds" );
	}

	/**
	 * Basic event handler that collects the relation between vehicles and drivers.
	 * Necessary since link enter and leave events do not contain the driver anymore.
	 * <p>
	 * This is the vice-versa implementation of {@link org.matsim.core.events.algorithms.Vehicle2DriverEventHandler}.
	 * <p>
	 * In a first step only used internally. When needed more often, I have nothing against putting it more central.
	 *
	 * @author kturner
	 */
	private final class Driver2VehicleEventHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

		private final Map<Id<Person>, Id<Vehicle>> driversVehicles = new ConcurrentHashMap<>();

		@Override
		public void reset(int iteration) {
			driversVehicles.clear();
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			driversVehicles.put(event.getPersonId(), event.getVehicleId());
		}

		//I do not think, that I need it --> keep the id of the "last" used vehicle in record
		//maybe change, that it is removed when a ActivityStartEvent- as last point of a leg - is thrown.
		//kmt sep'22
		@Override
		public void handleEvent(VehicleLeavesTrafficEvent event) {
//			driversVehicles.remove(event.getPersonId());
		}

		/**
		 * @param personId the unique driver identifier.
		 * @return vehicle id of the driver's vehicle
		 */
		public Id<Vehicle> getVehicleOfDriver(Id<Person> personId){
			return driversVehicles.get(personId);
		}

		public void handleAnEvent(Event event){
			if (event instanceof VehicleEntersTrafficEvent vehicleEntersTrafficEvent) {
				driver2EventHandler.handleEvent(vehicleEntersTrafficEvent);
			}
			if (event instanceof VehicleEntersTrafficEvent vehicleEntersTrafficEvent) {
				driver2EventHandler.handleEvent(vehicleEntersTrafficEvent);
			}
		}

	}

}

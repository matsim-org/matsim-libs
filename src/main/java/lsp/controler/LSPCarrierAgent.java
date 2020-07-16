package lsp.controler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.TourActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.controler.FreightActivity;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

class LSPCarrierAgent implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler,  LinkEnterEventHandler, LinkLeaveEventHandler,
							VehicleLeavesTrafficEventHandler, PersonEntersVehicleEventHandler, VehicleEntersTrafficEventHandler, PersonLeavesVehicleEventHandler {

	
	class CarrierDriverAgent {

		private Leg currentLeg;

		private Activity currentActivity;

		private List<Id<Link>> currentRoute;

		private final Id<Person> driverId;

		private final ScheduledTour scheduledTour;

		private int activityCounter = 0;

		CarrierDriverAgent(Id<Person> driverId, ScheduledTour tour) {
			this.driverId = driverId;
			this.scheduledTour = tour;
			new HashMap<Integer, CarrierShipment>();
		}

		/**
		 * 
		 * @param event
		 */
		public void handleEvent(PersonArrivalEvent event) {
			currentLeg.setTravelTime( event.getTime() - currentLeg.getDepartureTime().seconds() );
			double travelTime = currentLeg.getDepartureTime().seconds() + currentLeg.getTravelTime().seconds() - currentLeg.getDepartureTime().seconds();
			currentLeg.setTravelTime(travelTime);
			if (currentRoute.size() > 1) {
				NetworkRoute networkRoute = RouteUtils.createNetworkRoute(currentRoute, null);
				networkRoute.setTravelTime(travelTime);
				networkRoute.setVehicleId(getVehicle().getVehicleId());
				currentLeg.setRoute(networkRoute);
				currentRoute = null;
			} else {
				Id<Link> startLink;
				if(currentRoute.size() != 0){
					startLink = currentRoute.get(0);
				}
				else{
					startLink = event.getLinkId();
				}
				Route genericRoute = RouteUtils.createGenericRouteImpl(startLink, event.getLinkId());    
				genericRoute.setDistance(0.0);
				currentLeg.setRoute(genericRoute);
			}
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}

		public void handleEvent(PersonDepartureEvent event) {
			Leg leg = PopulationUtils.createLeg(event.getLegMode());
			leg.setDepartureTime(event.getTime());
			currentLeg = leg;
			currentRoute = new ArrayList<Id<Link>>();
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}

		public void handleEvent(LinkEnterEvent event) {
            currentRoute.add(event.getLinkId());
            notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}

		public void handleEvent(LinkLeaveEvent event) {
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}
		
		public void handleEvent(ActivityEndEvent event) {
			if (currentActivity == null) {
				Activity  firstActivity = PopulationUtils.createActivityFromLinkId(event.getActType(), event.getLinkId());
				firstActivity.setFacilityId(event.getFacilityId());
				currentActivity = firstActivity;
			}
			currentActivity.setEndTime(event.getTime());
			activityFinished(event); 
		}

		private TourActivity getTourActivity() {
			return (TourActivity) this.scheduledTour.getTour().getTourElements().get(activityCounter);
		}

		public void handleEvent(ActivityStartEvent event) {
			Activity activity = PopulationUtils.createActivityFromLinkId(event.getActType(), event.getLinkId()); 
			activity.setFacilityId(event.getFacilityId());
			activity.setStartTime(event.getTime());
			if(event.getActType().equals(FreightConstants.END)){
				activity.setEndTime(event.getTime());
				activityStarted(event);
			}
			else{
				TourActivity tourActivity = getTourActivity();
				assert activity.getLinkId().toString().equals(tourActivity.getLocation().toString()) : "linkId of activity is not equal to linkId of tourActivity. This must not be.";
				FreightActivity freightActivity = new FreightActivity(activity, tourActivity.getTimeWindow());
				currentActivity = freightActivity;
				activityStarted(event);
			}
		}

		public void handleEvent(VehicleLeavesTrafficEvent event) {
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}
		
		public void handleEvent(PersonEntersVehicleEvent event) {
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}
		
		public void handleEvent(VehicleEntersTrafficEvent event) {
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}
		
		public void handleEvent(PersonLeavesVehicleEvent event) {
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}
		
		private void activityStarted(ActivityStartEvent event) {
			notifyEventHappened(event, currentActivity, scheduledTour, driverId, activityCounter);
		}
		
		private void activityFinished(ActivityEndEvent event) {
			if(event.getActType().equals(FreightConstants.START)) {
				notifyEventHappened(event, currentActivity, scheduledTour, driverId, activityCounter);
				activityCounter += 1;
			}
			else {
				notifyEventHappened(event, currentActivity, scheduledTour, driverId, activityCounter);
				activityCounter += 2;
			}
		}
		
		
		CarrierVehicle getVehicle() {
			return scheduledTour.getVehicle();
		}

		TourElement getPlannedTourElement(int elementIndex){
			int index = elementIndex-1;
			int elementsSize = scheduledTour.getTour().getTourElements().size();
			if(index < 0) return scheduledTour.getTour().getStart();
			else if(index == elementsSize) return scheduledTour.getTour().getEnd();
			else if(index < elementsSize){
				return scheduledTour.getTour().getTourElements().get(index);
			}
			else throw new IllegalStateException("index out of bounds");
		}
	}

	private final Id<Carrier> id;

	private final Carrier carrier;

	private final LSPCarrierTracker tracker;

	private Collection<Id<Person>> driverIds = new ArrayList<>();

	private int nextId = 0;

	private Map<Id<Person>, CarrierDriverAgent> carrierDriverAgents = new HashMap<>();

	private Map<Id<Person>, ScheduledTour> driverTourMap = new HashMap<>();

	private final Vehicle2DriverEventHandler vehicle2DriverEventHandler;

	LSPCarrierAgent( LSPCarrierTracker carrierResourceTracker, Carrier carrier, Vehicle2DriverEventHandler vehicle2DriverEventHandler ) {
		this.tracker = carrierResourceTracker;
		this.carrier = carrier;
		this.id = carrier.getId();
		this.vehicle2DriverEventHandler = vehicle2DriverEventHandler;
	}

	public Id<Carrier> getId() {
		return id;
	}

	/**
	 * Returns a list of plans created on the basis of the carrier's plan.
	 * 
	 * <p>A carrier plan consists usually of many tours (activity chains). Each plan in the returned list represents a carrier tour.
	 *  
	 * @return list of plans
	 * @see Plan, CarrierPlan
	 */
	List<LSPMobSimVehicleRoute> createFreightDriverPlans() {
		clear();
		System.out.flush();
		System.err.flush() ;
		List<LSPMobSimVehicleRoute> routes = new ArrayList<LSPMobSimVehicleRoute>();
		//		List<Plan> plans = new ArrayList<Plan>();
		if (carrier.getSelectedPlan() == null) {
			return routes;
		}
		for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
			Id<Person> driverId = createDriverId(scheduledTour.getVehicle());
			CarrierVehicle carrierVehicle = scheduledTour.getVehicle();
			Person driverPerson = createDriverPerson(driverId);
			Vehicle vehicle = createVehicle(driverPerson,carrierVehicle);
			CarrierDriverAgent carrierDriverAgent = new CarrierDriverAgent(driverId, scheduledTour);
			Plan plan = PopulationUtils.createPlan();
			Activity startActivity = PopulationUtils.createActivityFromLinkId(FreightConstants.START,scheduledTour.getVehicle().getLocation());
			startActivity.setEndTime(scheduledTour.getDeparture());
			plan.addActivity(startActivity);
			for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {				
				if (tourElement instanceof org.matsim.contrib.freight.carrier.Tour.Leg) {
					org.matsim.contrib.freight.carrier.Tour.Leg tourLeg = (org.matsim.contrib.freight.carrier.Tour.Leg) tourElement;
					Route route = tourLeg.getRoute();
					if(route == null) throw new IllegalStateException("missing route for carrier " + this.getId());
					Leg leg = PopulationUtils.createLeg(TransportMode.car);
					leg.setRoute(route);
					leg.setDepartureTime(tourLeg.getExpectedDepartureTime());
					leg.setTravelTime(tourLeg.getExpectedTransportTime());
					leg.setTravelTime( tourLeg.getExpectedDepartureTime() + tourLeg.getExpectedTransportTime() - leg.getDepartureTime().seconds() );
					plan.addLeg(leg);
				} else if (tourElement instanceof TourActivity) {
					TourActivity act = (TourActivity) tourElement;
					Activity tourElementActivity = PopulationUtils.createActivityFromLinkId(act.getActivityType(), act.getLocation());					
					double duration = act.getDuration() ;
					tourElementActivity.setMaximumDuration(duration); // "maximum" has become a bit of a misnomer ...
					plan.addActivity(tourElementActivity);
				}
			}
			Activity endActivity = PopulationUtils.createActivityFromLinkId(FreightConstants.END,scheduledTour.getVehicle().getLocation());
			plan.addActivity(endActivity);
			driverPerson.addPlan(plan);
			plan.setPerson(driverPerson);
			LSPMobSimVehicleRoute mobsimRoute = new LSPMobSimVehicleRoute(plan, vehicle);
			routes.add(mobsimRoute);
			//			plans.add(plan);
			carrierDriverAgents.put(driverId, carrierDriverAgent);
			driverTourMap.put(driverId, scheduledTour);
		}
		return routes;
	}

	private Vehicle createVehicle(Person driverPerson, CarrierVehicle carrierVehicle) {
		return VehicleUtils.getFactory().createVehicle(Id.create(driverPerson.getId(), Vehicle.class), carrierVehicle.getVehicleType());
	}

	private void clear() {
		carrierDriverAgents.clear();
		driverTourMap.clear();
		driverIds.clear();
		nextId = 0;
	}

	public Collection<Id<Person>> getDriverIds() {
		return Collections.unmodifiableCollection(driverIds);
	}

	private Person createDriverPerson(Id<Person> driverId) {
		Person person = PopulationUtils.getFactory().createPerson(driverId);
		return person;
	}

	private Id<Person> createDriverId(CarrierVehicle carrierVehicle) {
		Id<Person> id = Id.create("freight_" + carrier.getId() + "_veh_" + carrierVehicle.getVehicleId() + "_" + nextId, Person.class);
		driverIds.add(id);
		++nextId;
		return id;
	}

	public void notifyEventHappened(Event event, Activity activity, ScheduledTour scheduledTour, Id<Person> driverId, int activityCounter) {
		tracker.notifyEventHappened(event, carrier, activity, scheduledTour, driverId, activityCounter);
	}
	 
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		getDriver(vehicle2DriverEventHandler.getDriverOfVehicle(event.getVehicleId())).handleEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		getDriver(vehicle2DriverEventHandler.getDriverOfVehicle(event.getVehicleId())).handleEvent(event);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
			getDriver(event.getPersonId()).handleEvent(event);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}

	CarrierDriverAgent getDriver(Id<Person> driverId){
		return carrierDriverAgents.get(driverId);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);	
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);	
	}

}

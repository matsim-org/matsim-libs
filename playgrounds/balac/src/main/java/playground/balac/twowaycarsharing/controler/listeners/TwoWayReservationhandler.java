package playground.balac.twowaycarsharing.controler.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.balac.twowaycarsharing.router.CarSharingStation;
import playground.balac.twowaycarsharing.router.PlansCalcRouteFtInfo;
import playground.balac.twowaycarsharing.router.CarSharingStations;

/**
 * EventHandlerer checking if the car, that is supposed to be used in a carsharing trip, is available.
 * If the car is not available the agent is rerouted to the nearest station that has an available car.
 * An agent is reserving the car so that nobody else can use it.
 * @author balacm
 * 
 */

public class TwoWayReservationhandler implements  PersonArrivalEventHandler, ActivityStartEventHandler,  ActivityEndEventHandler {
	
	private static final Logger log = Logger.getLogger(TwoWayReservationhandler.class);
	private HashMap<IdImpl, String> map = new HashMap<IdImpl, String>();
	private HashMap<Id, CarSharingStation> stations = new HashMap<Id, CarSharingStation>();
	private HashMap<Id, CarSharingStation> stationsByLink = new HashMap<Id, CarSharingStation>();
	private HashMap<Id, Integer> reserved = new HashMap<Id, Integer>();
	private HashMap<Id, Integer> activitiesCount = new HashMap<Id, Integer>();
	private CarSharingStations carSharingStations;
	private ArrayList<Id> personsId = new ArrayList<Id>();
	private PlansCalcRouteFtInfo plansCalcRouteFtInfo;
	private Controler controler;
	private int counter = 0;
	
	TwoWayReservationhandler(PlansCalcRouteFtInfo plansCalcRouteFtInfo, Controler controler) {
		this.plansCalcRouteFtInfo = plansCalcRouteFtInfo;
		this.carSharingStations = plansCalcRouteFtInfo.getCarStations();
		this.controler = controler;
	}
	  @Override
	  public void handleEvent(PersonArrivalEvent event) {	  		
		 map.put((IdImpl) event.getPersonId(), event.getLegMode()); 
	  }	  
	 
	  @Override
	  public void reset(int iteration) {
		  //set the capacities to the initial ones
		  personsId = new ArrayList<Id>();
		  map = new HashMap<IdImpl, String>();
		  if (iteration != 0)
		  for(CarSharingStation css: plansCalcRouteFtInfo.getCarStations().getStations().values()) {
			  stations.put(css.getLinkId(), css);
			  stationsByLink.put(css.getLinkId(), css);
			  
		  }
		  reserved = new HashMap<Id, Integer>();
		  activitiesCount = new HashMap<Id, Integer>();
		  counter = 0;
	  }	

	@Override
	public void handleEvent(ActivityStartEvent event) {
		// TODO Auto-generated method stub
		
		  if (event.getActType().equals( "carsharingInteraction" ) && !map.get(event.getPersonId()).equals( "carsharing" )) {
			  if (reserved.get(event.getLinkId()) != null)
				  reserved.put(event.getLinkId(), reserved.get(event.getLinkId()) - 1);
			  
			  int numberOfCars = stations.get(event.getLinkId()).getCars() - 1;
			  CarSharingStation newCarsStation = new CarSharingStation(stations.get(event.getLinkId()).getId(), stations.get(event.getLinkId()).getCoord(), stations.get(event.getLinkId()).getLink(), numberOfCars);
			  stations.put(event.getLinkId(), newCarsStation);
			  
		  }
		  else if (event.getActType().equals( "carsharingInteraction" ) && map.get(event.getPersonId()).equals( "carsharing" )) {			  
				  
				  int numberOfCars = stations.get(event.getLinkId()).getCars() + 1;
				  CarSharingStation newCarsStation = new CarSharingStation(stations.get(event.getLinkId()).getId(), stations.get(event.getLinkId()).getCoord(), stations.get(event.getLinkId()).getLink(), numberOfCars);
				  stations.put(event.getLinkId(), newCarsStation);
				  
			  }		  
		  if ( activitiesCount.containsKey(event.getPersonId())) {
			  
			  activitiesCount.put(event.getPersonId(), activitiesCount.get(event.getPersonId()) + 1);
		  }
		  else {
			  activitiesCount.put(event.getPersonId(), 1);
		  }  
	}	
	public int getNumberOfReroutedLegs() {
		return counter;
	}
	public ArrayList<Id> getPersonIdsWithoutCar () {
		
		return personsId;
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		// TODO Auto-generated method stub
		
		
		if (!(map.containsKey(event.getPersonId())) ||  (!event.getActType().equals("onewaycarsharingInteraction") && !map.get(event.getPersonId()).equals( "onewaycarsharing" ))) {
			
			//we need to check if the next activity is going to be carsharinginteraction
		
			int carsharingwalkIndex = 0;
			Plan plan = controler.getPopulation().getPersons().get(event.getPersonId()).getSelectedPlan();
			boolean nextCS = false;   //indicator if the next leg is carsharingwalk leg			
			if (activitiesCount.get(event.getPersonId()) == null) { //if this event is the end of the first activity in the plan
				
				if (((Leg)plan.getPlanElements().get(1)).getMode().equals("onewaycarsharingwalk")) {
					carsharingwalkIndex  = 1;  //index of the carsharingwalk leg
					nextCS = true;
					
				}
				
			}
			else if (nextCS) {
				
				//find the closest station that has an available car
				//reserve it, reroute the next carsharing leg if necessary 
				//continue with the simulation
				
				Vector<CarSharingStation> closest = carSharingStations.getClosestStations(controler.getNetwork().getLinks().get(event.getLinkId()).getCoord(), 20, 50000);
				boolean first = true;
				for (CarSharingStation css: closest) {
					
					if ((reserved.get(css.getLinkId()) != null && stations.get(css.getLinkId()).getCars() - reserved.get(css.getLinkId()).intValue() > 0) || (reserved.get(css.getLinkId()) == null && stations.get(css.getLinkId()).getCars()  > 0)) {
						
						if (activitiesCount.get(event.getPersonId()) == null) 
							reRoute( (Activity)plan.getPlanElements().get(0), css, plan, carsharingwalkIndex);
							
						
						else 
							reRoute( (Activity)plan.getPlanElements().get((2 * activitiesCount.get(event.getPersonId()) - 2)), css, plan, carsharingwalkIndex);
						
							if (!first) {
								log.info("reRouting a person to a station different than the closest one");
								counter++;
							}
						if (reserved.get(css.getLinkId()) != null)
							reserved.put(css.getLinkId(), (int)reserved.get(css.getLinkId()) + 1); //reserved plus 1 if it exists instead 1
						else
							reserved.put(css.getLinkId(), 1);
						
						break;
					}
					first = false;
				}
				
			}			
			
		}		
		
	}
	
	public void reRoute(Activity activity, CarSharingStation station, Plan plan, int index) {
		Route carsharingWalkRoute;
		LinkNetworkRouteImpl carsharingRoute;		
		
		final TripRouterFactoryInternal delegate = controler.getTripRouterFactory();
		final TripRouter router = delegate.instantiateAndConfigureTripRouter();
		double distance = CoordUtils.calcDistance(activity.getCoord(), station.getCoord());
		double walkSpeed = (((PlansCalcRouteConfigGroup)controler.getConfig().getModule("planscalcroute")).getTeleportedModeSpeeds().get("walk"));
		double beelineDistanceFactor = ((PlansCalcRouteConfigGroup)controler.getConfig().getModule("planscalcroute")).getBeelineDistanceFactor();
	
		PopulationFactory populationFactory = controler.getScenario().getPopulation().getFactory();
		ModeRouteFactory modeRouteFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
		
		carsharingWalkRoute = modeRouteFactory.createRoute("carsharingwalk", activity.getLinkId(),	station.getLinkId());
		carsharingWalkRoute.setTravelTime(  distance * beelineDistanceFactor / walkSpeed );
		
		
		((LegImpl)plan.getPlanElements().get(index)).setTravelTime(distance * beelineDistanceFactor / walkSpeed);
		((LegImpl)plan.getPlanElements().get(index)).setDepartureTime(activity.getEndTime());
		((LegImpl)plan.getPlanElements().get(index)).setArrivalTime(distance * beelineDistanceFactor / walkSpeed + activity.getEndTime());
		((LegImpl)plan.getPlanElements().get(index)).setRoute( carsharingWalkRoute );
		
		double time = distance * beelineDistanceFactor / walkSpeed + activity.getEndTime();
		
		ActivityImpl secondInteraction =
 				(ActivityImpl) populationFactory.createActivityFromLinkId(
				"carsharingInteraction",
				station.getLinkId());
 			secondInteraction.setMaximumDuration( 0 );
 			
 			((ActivityImpl)plan.getPlanElements().get(index + 1)).setLinkId(station.getLinkId());
 			
 		List<Id> ids = new ArrayList<Id>();
		String type = ((Activity)plan.getPlanElements().get(index + 3)).getType();
		double travelTime = 0.0;
		if (type.equals( "carsharingInteraction" )) {
					
			//CarSharingStation toStation = stationsByLink.get((((Activity)plan.getPlanElements().get(index + 3)).getLinkId()));
			double departureTime = activity.getEndTime() + distance * beelineDistanceFactor / walkSpeed;
			
			for(PlanElement pe: router.calcRoute("car", station, station, departureTime, plan.getPerson())) 
				 
				if (pe instanceof Leg) {
						ids = ((NetworkRoute)((Leg) pe).getRoute()).getLinkIds();
			    		travelTime += ((Leg) pe).getTravelTime();
					}
				   
			carsharingRoute = (LinkNetworkRouteImpl) modeRouteFactory.createRoute("car", station.getLinkId(),	station.getLinkId());
		
			carsharingRoute.setLinkIds(station.getLinkId(),ids, station.getLinkId());
				   
			carsharingRoute.setTravelTime( travelTime );
				   
			((LegImpl)plan.getPlanElements().get(index + 2)).setTravelTime(travelTime);
			((LegImpl)plan.getPlanElements().get(index + 2)).setDepartureTime(time);
			((LegImpl)plan.getPlanElements().get(index + 2)).setArrivalTime(time + travelTime);
			((LegImpl)plan.getPlanElements().get(index + 2)).setRoute( carsharingRoute );
					
			secondInteraction =
					(ActivityImpl) populationFactory.createActivityFromLinkId(
					"carsharingInteraction",
					station.getLinkId());
					secondInteraction.setMaximumDuration( 0 );
			 			
			((ActivityImpl)plan.getPlanElements().get(index + 3)).setLinkId(station.getLinkId());
			
			//carsharingwlak leg change at the end of the carsharing tour
			 			//distance = CoordUtils.calcDistance(activity.getCoord(), station.getCoord());
			time += travelTime;
			carsharingWalkRoute = modeRouteFactory.createRoute("carsharingwalk", station.getLinkId(),	((ActivityImpl)plan.getPlanElements().get(index + 3)).getLinkId());
			carsharingWalkRoute.setTravelTime(  distance * beelineDistanceFactor / walkSpeed );
			 			
			 			
			((LegImpl)plan.getPlanElements().get(index + 4)).setTravelTime(distance * beelineDistanceFactor / walkSpeed);
			((LegImpl)plan.getPlanElements().get(index + 4)).setDepartureTime(time);
			((LegImpl)plan.getPlanElements().get(index + 4)).setArrivalTime(distance * beelineDistanceFactor / walkSpeed + time);
			((LegImpl)plan.getPlanElements().get(index + 4)).setRoute( carsharingWalkRoute );
					
					
		
		}
		else {
			
			ActivityFacilityImpl toFacility = (ActivityFacilityImpl) controler.getScenario().getActivityFacilities().getFacilities().get(((Activity)plan.getPlanElements().get(index + 3)).getFacilityId());
			toFacility.setLinkId(((Activity)plan.getPlanElements().get(index + 3)).getLinkId());
			//ActivityFacilityImpl toFacility = new ActivityFacilityImpl(((Activity)plan.getPlanElements().get(index + 3)).getFacilityId(), ((Activity)plan.getPlanElements().get(index + 3)).getCoord());
			
			double departureTime = activity.getEndTime() + distance * beelineDistanceFactor / walkSpeed;
			travelTime = 0.0;
			   for(PlanElement pe: router.calcRoute("car", station, toFacility, departureTime, plan.getPerson())) 
		    	
				   if (pe instanceof Leg) {
						ids = ((NetworkRoute)((Leg) pe).getRoute()).getLinkIds();
			    		travelTime += ((Leg) pe).getTravelTime();
					}
				  
			   carsharingRoute = (LinkNetworkRouteImpl) modeRouteFactory.createRoute("car", station.getLinkId(),	((Activity)plan.getPlanElements().get(index + 3)).getLinkId());
			  
			   carsharingRoute.setLinkIds(station.getLinkId(),ids, toFacility.getLinkId());

			   carsharingRoute.setTravelTime( travelTime );
			   ((LegImpl)plan.getPlanElements().get(index + 2)).setTravelTime(travelTime);
			   ((LegImpl)plan.getPlanElements().get(index + 2)).setDepartureTime(time);
			   ((LegImpl)plan.getPlanElements().get(index + 2)).setArrivalTime(time + travelTime);
				((LegImpl)plan.getPlanElements().get(index + 2)).setRoute( carsharingRoute );
				int indexEnd = -1;
				for (int i = index + 3; i < plan.getPlanElements().size(); i++) {
						
					if (plan.getPlanElements().get(i) instanceof Leg) {
						if (((LegImpl)plan.getPlanElements().get(i)).getMode().equals( "carsharingwalk" )) {
							indexEnd = i - 2;
							break;
						}
					}
						
				}				
				
					//changing the end of the carsharing route, since we changed the starting station
				departureTime = ((Activity)plan.getPlanElements().get(indexEnd - 1)).getEndTime();
				ActivityFacilityImpl fromFacility =  (ActivityFacilityImpl) controler.getScenario().getActivityFacilities().getFacilities().get(((Activity)plan.getPlanElements().get(indexEnd - 1)).getFacilityId());
				fromFacility.setLinkId(((Activity)plan.getPlanElements().get(indexEnd - 1)).getLinkId());
				travelTime = 0.0;
			    for(PlanElement pe: router.calcRoute("car", fromFacility, station, departureTime, plan.getPerson())) 
				    	
			    	if (pe instanceof Leg) {
						ids = ((NetworkRoute)((Leg) pe).getRoute()).getLinkIds();
			    		travelTime += ((Leg) pe).getTravelTime();
					}
						  
			    carsharingRoute = (LinkNetworkRouteImpl) modeRouteFactory.createRoute("car", fromFacility.getLinkId(),station.getLinkId());
				
				carsharingRoute.setLinkIds(fromFacility.getLinkId(),ids, station.getLinkId());

				carsharingRoute.setTravelTime( travelTime );
				((LegImpl)plan.getPlanElements().get(indexEnd )).setTravelTime(travelTime);
				((LegImpl)plan.getPlanElements().get(indexEnd )).setDepartureTime(departureTime);
				((LegImpl)plan.getPlanElements().get(indexEnd )).setArrivalTime(departureTime + travelTime);
				((LegImpl)plan.getPlanElements().get(indexEnd )).setRoute( carsharingRoute );
				time = departureTime + travelTime;
				//carsharinginteraction change
					
				secondInteraction =
					(ActivityImpl) populationFactory.createActivityFromLinkId(
					"carsharingInteraction",
					station.getLinkId());
					secondInteraction.setMaximumDuration( 0 );
					 			
				((ActivityImpl)plan.getPlanElements().get(indexEnd + 1)).setLinkId(station.getLinkId());
					
				//carsharingwlak leg change at the end of the carsharing tour
				carsharingWalkRoute = modeRouteFactory.createRoute("carsharingwalk", station.getLinkId(),	((ActivityImpl)plan.getPlanElements().get(indexEnd + 3)).getLinkId());
				carsharingWalkRoute.setTravelTime(  distance * beelineDistanceFactor / walkSpeed );
					 			
				((LegImpl)plan.getPlanElements().get(indexEnd + 2)).setTravelTime(distance * beelineDistanceFactor / walkSpeed);
				((LegImpl)plan.getPlanElements().get(indexEnd + 2)).setDepartureTime(time);
				((LegImpl)plan.getPlanElements().get(indexEnd + 2)).setArrivalTime(distance * beelineDistanceFactor / walkSpeed + time);
				((LegImpl)plan.getPlanElements().get(indexEnd + 2)).setRoute( carsharingWalkRoute );
					
		}
			
	}
	
}

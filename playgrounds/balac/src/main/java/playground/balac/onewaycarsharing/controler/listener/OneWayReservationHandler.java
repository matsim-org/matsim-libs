package playground.balac.onewaycarsharing.controler.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
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
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.balac.onewaycarsharing.router.CarSharingStation;
import playground.balac.onewaycarsharing.router.CarSharingStations;
import playground.balac.onewaycarsharing.router.PlansCalcRouteFtInfo;


/**
 * EventHandlerer checking if the car that is supposed to be used in a carsharing trip is available
 * If the car is not available the agent is rerouted to the nearest station that has an available car
 * An agent is reserving the car so that nobody else can use it.
 * @author balacm
 *  
 */

public class OneWayReservationHandler implements  PersonArrivalEventHandler, ActivityStartEventHandler,  ActivityEndEventHandler {
	  private static final Logger log = Logger.getLogger(OneWayReservationHandler.class);

	private HashMap<IdImpl, String> map = new HashMap<IdImpl, String>();
	private HashMap<Id, CarSharingStation> stations = new HashMap<Id, CarSharingStation>();
	private HashMap<Id, CarSharingStation> stationsByLink = new HashMap<Id, CarSharingStation>();
	private HashMap<Id, Integer> reserved = new HashMap<Id, Integer>();
	private CarSharingStations CarSharingStations;
	private ArrayList<Id> personsId = new ArrayList<Id>();
	private PlansCalcRouteFtInfo plansCalcRouteFtInfo;
	private Controler controler;
	private int reRoutedLegs = 0;
	
	OneWayReservationHandler(PlansCalcRouteFtInfo plansCalcRouteFtInfo, Controler controler) {
		this.plansCalcRouteFtInfo = plansCalcRouteFtInfo;
		this.CarSharingStations = plansCalcRouteFtInfo.getCarStations();
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
		  reRoutedLegs = 0;
	  }	

	@Override
	public void handleEvent(ActivityStartEvent event) {
		// TODO Auto-generated method stub
		
		  if (event.getActType() == "onewaycarsharingInteraction" && map.get(event.getPersonId()) != "onewaycarsharing") {
			  if (reserved.get(event.getLinkId()) != null)
				  reserved.put(event.getLinkId(), reserved.get(event.getLinkId()) - 1);
			  
			  int x = stations.get(event.getLinkId()).getCars() - 1;
			  CarSharingStation newCarsStation = new CarSharingStation(stations.get(event.getLinkId()).getId(), stations.get(event.getLinkId()).getCoord(), stations.get(event.getLinkId()).getLink(), x);
			  stations.put(event.getLinkId(), newCarsStation);
			  
		  }
		  else if (event.getActType() == "onewaycarsharingInteraction" && map.get(event.getPersonId()) == "onewaycarsharing") {			  
				  
				  int x = stations.get(event.getLinkId()).getCars() + 1;
				  CarSharingStation newCarsStation = new CarSharingStation(stations.get(event.getLinkId()).getId(), stations.get(event.getLinkId()).getCoord(), stations.get(event.getLinkId()).getLink(), x);
				  stations.put(event.getLinkId(), newCarsStation);
				  
			  }		  
		  
	}	
	public int getNumberOfReroutedLegs() {
		return reRoutedLegs;
	}
	public ArrayList<Id> getPersonIdsWithoutCar () {
		
		return personsId;
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		// TODO Auto-generated method stub
		if (event.getActType() != "onewaycarsharingInteraction" && map.get(event.getPersonId()) != "onewaycarsharing") {
			
			//we need to check if the next activity is going to be carsharinginteraction
		
			int carsharingwalkIndex = 0;
			Plan plan = controler.getPopulation().getPersons().get(event.getPersonId()).getSelectedPlan();
			boolean nextCS = false;   //indicator if the next leg is carsharingwalk leg
			PlanElement fromPlanElement = null;
			for (PlanElement pe: plan.getPlanElements()) {
				if (pe instanceof Activity) {
					if (((Activity) pe).getEndTime() == event.getTime()) {
					
						if ( ( (Leg)(plan.getPlanElements().get(plan.getPlanElements().indexOf(pe) + 1) )).getMode() == "onewaycarsharingwalk" ) {
							carsharingwalkIndex  = plan.getPlanElements().indexOf(pe) + 1;  //index of the carsharingwalk leg
							nextCS = true;			
							fromPlanElement = pe;
							break;
						}
					}
					
				}
				
			}
			
			if (nextCS) {
				
				//find the closest station that has an available car
				//reserve it, reroute the next carsharing leg if necessary 
				//continue with the simulation
				
				Vector<CarSharingStation> closest = CarSharingStations.getClosestStations(controler.getNetwork().getLinks().get(event.getLinkId()).getCoord(), 20, 50000);
				boolean first = true;
				for (CarSharingStation css: closest) {
					
					if ((reserved.get(css.getLinkId()) != null && stations.get(css.getLinkId()).getCars() - reserved.get(css.getLinkId()).intValue() > 0) || (reserved.get(css.getLinkId()) == null && stations.get(css.getLinkId()).getCars()  > 0)) {
						
							reRoute( (Activity)fromPlanElement, css, plan, carsharingwalkIndex);
							if (!first) {
								log.info("reRouting a person to a station different than the closest one");
								reRoutedLegs++;
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
	
	public void reRoute( Activity activity, CarSharingStation station, Plan plan, int index) {
		Route carsharingWalkRoute;
		LinkNetworkRouteImpl carsharingRoute;		
		double travelTime = 0.0;
		double distance = CoordUtils.calcDistance(activity.getCoord(), station.getCoord());
		double walkSpeed = (((PlansCalcRouteConfigGroup)controler.getConfig().getModule("planscalcroute")).getTeleportedModeSpeeds().get("walk"));
		double beelineDistanceFactor = ((PlansCalcRouteConfigGroup)controler.getConfig().getModule("planscalcroute")).getBeelineDistanceFactor();
		
		double currentTime = distance * beelineDistanceFactor / walkSpeed + activity.getEndTime();
		
		final TripRouterFactoryInternal delegate = controler.getTripRouterFactory();
		final TripRouter router = delegate.instantiateAndConfigureTripRouter();
		
		TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculatorFactoryImpl()
		.createTravelTimeCalculator(controler.getNetwork(), controler.getScenario().getConfig().travelTimeCalculator());
	   
	   TravelDisutility travelDisutility = new TravelCostCalculatorFactoryImpl()
		.createTravelDisutility(travelTimeCalculator
				.getLinkTravelTimes(), controler.getConfig()
				.planCalcScore());
	   Dijkstra carDijkstra = new Dijkstra(controler.getNetwork(),
			   travelDisutility, travelTimeCalculator.getLinkTravelTimes());
	   
	   
		PopulationFactory populationFactory = controler.getScenario().getPopulation().getFactory();
		ModeRouteFactory modeRouteFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
		
		carsharingWalkRoute = modeRouteFactory.createRoute("onewaycarsharingwalk", activity.getLinkId(),	station.getLinkId());
		carsharingWalkRoute.setTravelTime(  distance * beelineDistanceFactor / walkSpeed );		
		
		((LegImpl)plan.getPlanElements().get(index)).setTravelTime(distance * beelineDistanceFactor / walkSpeed);
		((LegImpl)plan.getPlanElements().get(index)).setDepartureTime(activity.getEndTime());
		((LegImpl)plan.getPlanElements().get(index)).setArrivalTime(distance * beelineDistanceFactor / walkSpeed + activity.getEndTime());
		((LegImpl)plan.getPlanElements().get(index)).setRoute( carsharingWalkRoute );
		
		
		
		final ActivityImpl secondInteraction =
 				(ActivityImpl) populationFactory.createActivityFromLinkId(
				"onewaycarsharingInteraction",
				station.getLinkId());
 			secondInteraction.setMaximumDuration( 0 );
 			
 			((ActivityImpl)plan.getPlanElements().get(index + 1)).setLinkId(station.getLinkId());
 			
 		//get the next activity after the first carsharing leg
		String typeNextActivity = ((Activity)plan.getPlanElements().get(index + 3)).getType();
		
		if (typeNextActivity == "onewaycarsharingInteraction") {
					
			CarSharingStation toStation = stationsByLink.get((((Activity)plan.getPlanElements().get(index + 3)).getLinkId()));
			double departureTime = activity.getEndTime() + distance * beelineDistanceFactor/ walkSpeed;
			
			   for(PlanElement planElement: router.calcRoute("car", station, toStation, departureTime, plan.getPerson())) 
		    	
				   if (planElement instanceof Leg) {
		   		
					   travelTime += ((Leg) planElement).getTravelTime();
				   }
				   
			   carsharingRoute = (LinkNetworkRouteImpl) modeRouteFactory.createRoute("car", station.getLinkId(),	toStation.getLinkId());
				   
			   List<Id> ids = new ArrayList<Id>();
			   for (Link l: carDijkstra.calcLeastCostPath(controler.getNetwork().getLinks().get(station.getLinkId()).getToNode(), toStation.getLink().getFromNode(), 0, null, null).links) {
				   ids.add(l.getId());
					   
			   }
			   carsharingRoute.setLinkIds(station.getLinkId(),ids, toStation.getLinkId());
			   
			   carsharingRoute.setTravelTime( travelTime );
				   
			   ((LegImpl)plan.getPlanElements().get(index + 2)).setTravelTime(travelTime);
			   ((LegImpl)plan.getPlanElements().get(index + 2)).setDepartureTime(currentTime);
			   ((LegImpl)plan.getPlanElements().get(index + 2)).setArrivalTime(currentTime + travelTime);
			   ((LegImpl)plan.getPlanElements().get(index + 2)).setRoute( carsharingRoute );	
		
		}
		else {
			
			ActivityFacilityImpl toFacility = (ActivityFacilityImpl) controler.getScenario().getActivityFacilities().getFacilities().get(((Activity)plan.getPlanElements().get(index + 3)).getFacilityId());
			toFacility.setLinkId(((Activity)plan.getPlanElements().get(index + 3)).getLinkId());
			
			double departureTime = activity.getEndTime() + distance * beelineDistanceFactor/ walkSpeed;
			travelTime = 0.0;
			   for(PlanElement planElement: router.calcRoute("car", station, toFacility, departureTime, plan.getPerson())) 
		    	
				   if (planElement instanceof Leg) {
		   		
					   travelTime += ((Leg) planElement).getTravelTime();
				   }
				  
				   carsharingRoute = (LinkNetworkRouteImpl) modeRouteFactory.createRoute("car", station.getLinkId(),	((Activity)plan.getPlanElements().get(index + 3)).getLinkId());
				   List<Id> ids = new ArrayList<Id>();
				   for (Link l: carDijkstra.calcLeastCostPath(controler.getNetwork().getLinks().get(station.getLinkId()).getToNode(), controler.getNetwork().getLinks().get(toFacility.getLinkId()).getFromNode(), 0, null, null).links) {
					   ids.add(l.getId());
					   
				   }
				   carsharingRoute.setLinkIds(station.getLinkId(),ids, toFacility.getLinkId());

				   carsharingRoute.setTravelTime( travelTime );
				   ((LegImpl)plan.getPlanElements().get(index + 2)).setTravelTime(travelTime);
				   ((LegImpl)plan.getPlanElements().get(index + 2)).setDepartureTime(currentTime);
				   ((LegImpl)plan.getPlanElements().get(index + 2)).setArrivalTime(currentTime + travelTime);
					((LegImpl)plan.getPlanElements().get(index + 2)).setRoute( carsharingRoute );			
		}
		
			
	}


	
	}
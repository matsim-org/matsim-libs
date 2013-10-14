package playground.balac.onewaycarsharing.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;




/**
 * @author balacm
 */

public class OneWayCarsharingRoutingModule implements RoutingModule {

	
	private final RoutingModule carDelegate;
	private final PopulationFactory populationFactory;
	private final ModeRouteFactory modeRouteFactory;
	private final PlansCalcRouteFtInfo plansCalcRouteFtInfo;
	private final PlansCalcRouteConfigGroup group;
	private Controler controler;
	public OneWayCarsharingRoutingModule(PlansCalcRouteConfigGroup group,
			final RoutingModule carDelegate,
			final PopulationFactory populationFactory, final PlansCalcRouteFtInfo plansCalcRouteFtInfo, Controler controler) {
		this.group = group;
		this.carDelegate = carDelegate;
		this.populationFactory = populationFactory;
		this.modeRouteFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
		this.plansCalcRouteFtInfo = plansCalcRouteFtInfo;
		this.controler = controler;
	}
	

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person) {
		// TODO Auto-generated method stub
		
		boolean start = false;   //represents if the carsharing trip being routes is the start of carsharing tour
		boolean end = false;	 //represents if the carsharing trip being routes is the END of carsharing tour
		boolean first = true;
		int index = -1;
		int indexEnd  = -1;
		double time = 0.0;
				
		for (PlanElement pe:person.getSelectedPlan().getPlanElements()) {
			
			LegImpl leg;
			if (pe instanceof Activity)  {
				if ((((Activity) pe).getFacilityId() == fromFacility.getId()) &&((Activity) pe).getEndTime() == departureTime) {
					
					index = person.getSelectedPlan().getPlanElements().indexOf((Activity)pe);
					
					if (index != 0) {
						leg = (LegImpl) person.getSelectedPlan().getPlanElements().get(index - 1);
						if (leg.getMode() != "onewaycarsharing" && leg.getMode() != "onewaycarsharingwalk") 
							start = true;
					}
					else
						start = true;					
					
				}
				else if ((((Activity) pe).getFacilityId() == toFacility.getId()) && index != -1 && first) {
						first = false;						
						
						indexEnd = person.getSelectedPlan().getPlanElements().indexOf((Activity)pe);
						if (person.getSelectedPlan().getPlanElements().size() == indexEnd + 1 )
							end = true;
						else {
							leg = (LegImpl) person.getSelectedPlan().getPlanElements().get(indexEnd + 1);	
						
							if (leg.getMode() != "onewaycarsharing" && leg.getMode() != "onewaycarsharingwalk") 
								end = true;
						}				
				}				
				
		}
		}
		CarSharingStation fromStation = this.plansCalcRouteFtInfo.getCarStations().getClosestLocation(fromFacility.getCoord());
		CarSharingStation toStation = this.plansCalcRouteFtInfo.getCarStations().getClosestLocation(toFacility.getCoord());

		FtCarSharingRoute newRoute = new FtCarSharingRoute(fromFacility.getLinkId(), toFacility.getLinkId(), this.plansCalcRouteFtInfo, fromStation, toStation);
		LinkNetworkRouteImpl carsharingRoute;
		Route teleportationRoute;
		LegImpl carsharingwalkLeg;	
		
		final List<PlanElement> trip = new ArrayList<PlanElement>();
		
		
		if (start == true) {
		//create a carsharingwalk leg because this is the start of the tour
			
		carsharingwalkLeg =	(LegImpl) populationFactory.createLeg("onewaycarsharingwalk");
		carsharingwalkLeg.setTravelTime( getAccessEgressTime(newRoute.calcAccessDistance(fromFacility.getCoord()), group) );
		teleportationRoute = modeRouteFactory.createRoute("onewaycarsharingwalk", fromFacility.getLinkId(),	toStation.getLinkId());
		teleportationRoute.setTravelTime( getAccessEgressTime(newRoute.calcAccessDistance(fromFacility.getCoord()), group)  );
		
		carsharingwalkLeg.setRoute( teleportationRoute );
		carsharingwalkLeg.setDepartureTime(departureTime);
		carsharingwalkLeg.setArrivalTime(departureTime + getAccessEgressTime(newRoute.calcAccessDistance(fromFacility.getCoord()), group) );
		trip.add( carsharingwalkLeg );		

		// create a dummy activity at the carsharing origin
		final Activity firstInteraction = populationFactory.createActivityFromLinkId("onewaycarsharingInteraction",fromStation.getLinkId());
		firstInteraction.setMaximumDuration( 0 );
		trip.add( firstInteraction );		
			
		}
		List<Id> ids = new ArrayList<Id>();
		if (start && !end) {
			double travelTime = 0.0;
			
			for(PlanElement pe: carDelegate.calcRoute(fromStation, toFacility, departureTime, person)) {
		    	
				if (pe instanceof Leg) {
					ids = ((NetworkRoute)((Leg) pe).getRoute()).getLinkIds();
					//((NetworkRoute)((Leg) pe).getRoute()).getLinkIds()
		    			travelTime += ((Leg) pe).getTravelTime();
				}
				}
				final LegImpl carsharingLeg =
					(LegImpl) populationFactory.createLeg("onewaycarsharing");
				carsharingLeg.setTravelTime( travelTime );
				carsharingRoute = (LinkNetworkRouteImpl) modeRouteFactory.createRoute("car", fromStation.getLinkId(),	toFacility.getLinkId());
				
				carsharingRoute.setLinkIds(fromStation.getLinkId(),ids, toFacility.getLinkId());
				carsharingRoute.setTravelTime( travelTime);
				carsharingLeg.setRoute( carsharingRoute );
				carsharingLeg.setDepartureTime(departureTime + getAccessEgressTime(newRoute.calcAccessDistance(fromFacility.getCoord()), group) );
				time = travelTime + getAccessEgressTime(newRoute.calcAccessDistance(fromFacility.getCoord()), group);
				carsharingLeg.setArrivalTime(departureTime + getAccessEgressTime(newRoute.calcAccessDistance(fromFacility.getCoord()), group) + travelTime);
				trip.add( carsharingLeg );
		    	
			}
		
		else if (start && end) {
			   
			double travelTime = 0.0;
			for(PlanElement pe: carDelegate.calcRoute(fromStation, toStation, departureTime, person)) {
				
				if (pe instanceof Leg) {
					ids = ((NetworkRoute)((Leg) pe).getRoute()).getLinkIds();
					//((NetworkRoute)((Leg) pe).getRoute()).getLinkIds()
		    			travelTime += ((Leg) pe).getTravelTime();
				}
				    	
			}			
			final LegImpl carsharingLeg =
						(LegImpl) populationFactory.createLeg("onewaycarsharing");
			carsharingLeg.setTravelTime( travelTime );
			carsharingRoute = (LinkNetworkRouteImpl) modeRouteFactory.createRoute("car", fromStation.getLinkId(),	toStation.getLinkId());
			
			carsharingRoute.setLinkIds(fromStation.getLinkId(),ids, toStation.getLinkId());
			carsharingRoute.setTravelTime( travelTime );
			carsharingLeg.setDepartureTime(departureTime + getAccessEgressTime(newRoute.calcAccessDistance(fromFacility.getCoord()), group) );
			time = travelTime + getAccessEgressTime(newRoute.calcAccessDistance(fromFacility.getCoord()), group);
			carsharingLeg.setArrivalTime(departureTime + getAccessEgressTime(newRoute.calcAccessDistance(fromFacility.getCoord()), group) + travelTime);

			carsharingLeg.setRoute( carsharingRoute );
			trip.add( carsharingLeg );			  
			   
		}
		else if (!start && end){
			double travelTime = 0.0;
			for(PlanElement pe: carDelegate.calcRoute(fromFacility, toStation, departureTime, person)) {
				    	
				if (pe instanceof Leg) {
					ids = ((NetworkRoute)((Leg) pe).getRoute()).getLinkIds();
					//((NetworkRoute)((Leg) pe).getRoute()).getLinkIds()
		    			travelTime += ((Leg) pe).getTravelTime();
				}		    	
				    	
			}
				
			final LegImpl carsharingLeg =
					(LegImpl) populationFactory.createLeg("onewaycarsharing");
			carsharingLeg.setTravelTime( travelTime );
					
			carsharingRoute = (LinkNetworkRouteImpl) modeRouteFactory.createRoute("car", fromFacility.getLinkId(),	toStation.getLinkId());
			
			carsharingRoute.setLinkIds(fromFacility.getLinkId(),ids, toStation.getLinkId());
			carsharingRoute.setTravelTime( travelTime );
			carsharingLeg.setDepartureTime(departureTime);
			time = travelTime;
			carsharingLeg.setArrivalTime(departureTime + travelTime);

			carsharingLeg.setRoute( carsharingRoute );
			trip.add( carsharingLeg );
			   
	   }
	   else {
			   
			   double travelTime = 0.0;
				for(PlanElement pe: carDelegate.calcRoute(fromFacility, toFacility, departureTime, person)) {
				    	
					if (pe instanceof Leg) {
						ids = ((NetworkRoute)((Leg) pe).getRoute()).getLinkIds();
						//((NetworkRoute)((Leg) pe).getRoute()).getLinkIds()
			    			travelTime += ((Leg) pe).getTravelTime();
					}
				    	
				    	
				}
				final LegImpl carsharingLeg =
						(LegImpl) populationFactory.createLeg("onewaycarsharing");
					carsharingLeg.setTravelTime( travelTime );
					carsharingRoute = (LinkNetworkRouteImpl) modeRouteFactory.createRoute("car", fromFacility.getLinkId(),	toFacility.getLinkId());
										
					carsharingRoute.setLinkIds(fromStation.getLinkId(),ids, toFacility.getLinkId());
					
					carsharingRoute.setTravelTime(  travelTime );
					carsharingLeg.setRoute( carsharingRoute );
					carsharingLeg.setDepartureTime(departureTime);
					carsharingLeg.setArrivalTime(departureTime + travelTime);

					time = travelTime;
					trip.add( carsharingLeg );
		   }		
			
		   if (end) {
			   			   
			   // create a dummy activity at the carsharing end
			   final Activity secondInteraction =
		 				populationFactory.createActivityFromLinkId(
		 						"onewaycarsharingInteraction",
		 						toStation.getLinkId());
			   secondInteraction.setMaximumDuration( 0 );
			   trip.add( secondInteraction );		
		   
			   carsharingwalkLeg = (LegImpl) populationFactory.createLeg("onewaycarsharingwalk");
			   carsharingwalkLeg.setTravelTime( getAccessEgressTime(newRoute.calcEgressDistance(toFacility.getCoord()), group) );
			   teleportationRoute = modeRouteFactory.createRoute("onewaycarsharingwalk", toFacility.getLinkId(),	toStation.getLinkId());
			   teleportationRoute.setTravelTime( getAccessEgressTime(newRoute.calcEgressDistance(toFacility.getCoord()), group)  );
			   carsharingwalkLeg.setRoute( teleportationRoute );
			   carsharingwalkLeg.setDepartureTime(time + departureTime);
			   carsharingwalkLeg.setArrivalTime(time + departureTime + getAccessEgressTime(newRoute.calcEgressDistance(toFacility.getCoord()), group) );
			   trip.add( carsharingwalkLeg );
		   }
		
		return trip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		final CompositeStageActivityTypes stageTypes = new CompositeStageActivityTypes();

		// trips for this mode contain the ones we create, plus the ones of the
		// car router we use.
		stageTypes.addActivityTypes( carDelegate.getStageActivityTypes() );
		stageTypes.addActivityTypes(new StageActivityTypesImpl("onewaycarsharingInteraction"));

		return stageTypes;
	}
	
	 public static double getAccessEgressTime(double distance, PlansCalcRouteConfigGroup group) {
		    return (distance / group.getTeleportedModeSpeeds().get("walk"));
		  }
		  
	
	
}

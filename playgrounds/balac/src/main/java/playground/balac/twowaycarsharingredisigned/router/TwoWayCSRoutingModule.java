package playground.balac.twowaycarsharingredisigned.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;

public class TwoWayCSRoutingModule implements RoutingModule {
	
	
	private ArrayList<Id> startCoord = new ArrayList<Id>();
	private ArrayList<Id> endCoord = new ArrayList<Id>();

	private Person person = null;
	private int legcount = 0;
	public TwoWayCSRoutingModule() {		
		
	}
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person) {
		final List<PlanElement> trip = new ArrayList<PlanElement>();
		
		//lets count legs and check if the next leg is twoway carsharing if it is then keep the car
		if (this.person == null || person != this.person) {
			legcount = 0;
			startCoord = new ArrayList<Id>();
			endCoord = new ArrayList<Id>();
			this.person = person;
			
		}
		
		Leg leg;
		if (startCoord.isEmpty()) {
			
			startCoord.add(fromFacility.getLinkId());
			leg = createWalkLeg(fromFacility, toFacility);
			trip.add( leg );
			legcount++;
			leg = createCarLeg(fromFacility, toFacility);
			trip.add( leg );
			legcount++;
			endCoord.add(toFacility.getLinkId());
			if (fromFacility.getLinkId() == toFacility.getLinkId()) {
				leg = createWalkLeg(fromFacility, toFacility);
				legcount++;
				trip.add( leg );
				startCoord.remove(fromFacility.getLinkId());
				endCoord.remove(toFacility.getLinkId());
				
			}
		}
		else {
			
			if (endCoord.contains(fromFacility.getLinkId())) {
				endCoord.remove(fromFacility.getLinkId());
				if (startCoord.contains(toFacility.getLinkId())) {
					
					Plan plan = person.getSelectedPlan();
					int temp = 0;
					boolean ind = false;
					boolean act = false;
					for (PlanElement pe: plan.getPlanElements()) {
						if (pe instanceof Activity) {
							if (temp == legcount + 1) {
								
								act = true;
								
							}
						}
						else if (pe instanceof Leg) {
							
							if (temp == legcount + 1 && act) {
								
								if (((Leg) pe).getMode().equals("twowaycarsharing") || ((Leg) pe).getMode().equals("walk_rb")) {
									leg = createCarLeg(fromFacility, toFacility);
									legcount++;
									trip.add( leg );
									endCoord.add(toFacility.getLinkId());
									ind = true;
									
									
								}
								break;
							}
							
							else if (temp < legcount + 1 && ((Leg) pe).getMode().equals("twowaycarsharing") || ((Leg) pe).getMode().equals("walk_rb")) 
								temp++;
							
							
						}
					}
					
					if (!ind) {
					
						leg = createCarLeg(fromFacility, toFacility);
						legcount++;
						trip.add( leg );
						leg = createWalkLeg(fromFacility, toFacility);
						legcount++;
						trip.add( leg );
						startCoord.remove(toFacility.getLinkId());
					}
				}
				else {
					endCoord.add(toFacility.getLinkId());
					leg = createCarLeg(fromFacility, toFacility);
					legcount++;
					trip.add(leg);
				}
				
			}
			else {
				
				startCoord.add(fromFacility.getLinkId());
				leg = createWalkLeg(fromFacility, toFacility);
				legcount++;
				trip.add( leg );
				leg = createCarLeg(fromFacility, toFacility);
				legcount++;
				trip.add( leg );
				endCoord.add(toFacility.getLinkId());				
				if (fromFacility.getLinkId() == toFacility.getLinkId()) {
					leg = createWalkLeg(fromFacility, toFacility);
					legcount++;
					trip.add( leg );
					
				}
			}
		}
		
		
		return trip;
	}
	
	private Leg createWalkLeg(Facility fromFacility,
			Facility toFacility) {
		final Leg leg = new LegImpl( "walk_rb" );
		GenericRouteImpl route = new GenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		leg.setRoute(route);
		return leg;
		
	}
	
	private Leg createCarLeg(Facility fromFacility,
			Facility toFacility) {
		final Leg leg1 = new LegImpl( "twowaycarsharing" );
		LinkNetworkRouteImpl route1 = new LinkNetworkRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		leg1.setRoute(route1);
		
		return leg1;
	}
	@Override
	public StageActivityTypes getStageActivityTypes() {
		// TODO Auto-generated method stub
		
		return EmptyStageActivityTypes.INSTANCE;
	}
}

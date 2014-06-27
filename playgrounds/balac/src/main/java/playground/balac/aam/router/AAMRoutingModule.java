package playground.balac.aam.router;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.io.IOUtils;


public class AAMRoutingModule implements RoutingModule {

	private Scenario scenario;
	private	WalkTravelTime timeObject;
	HashMap<Id, Double> linkSpeeds;
	TreeSet<Id> pathwayLinks = new TreeSet<Id>();
	
	public AAMRoutingModule (Scenario scenario) {
		
		this.scenario = scenario;
		linkSpeeds = new HashMap<Id, Double>();

		BufferedReader reader;
		reader = IOUtils.getBufferedReader(this.scenario.getConfig().getModule("MovingPathways").getParams().get("movingPathwaysInputFile"));
		String s;
		try {
			s = reader.readLine();
		
			while (s != null) {
			
				String[] array = s.split("\t", -1);
				if (array.length != 2) throw (new IOException("Error parsing a line " + s +" from the input file, check the length of the line"));
				linkSpeeds.put(new IdImpl(array[0]), Double.parseDouble(array[1]));
				pathwayLinks.add(new IdImpl(array[0]));
				s = reader.readLine();
			}
		
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		
		
		this.timeObject = new WalkTravelTime(scenario, linkSpeeds);
		
		
	}
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person) {
		// TODO Create list of legs with moving pathways and walking
		final List<PlanElement> trip = new ArrayList<PlanElement>();

		FreespeedTravelTimeAndDisutility freespeed = new FreespeedTravelTimeAndDisutility(-0.8/3600, +6.0/3600, 0.0);
				
		LeastCostPathCalculator routeAlgo = new Dijkstra(scenario.getNetwork(), freespeed, this.timeObject);
		
		Path path = routeAlgo.calcLeastCostPath(scenario.getNetwork().getLinks().get(fromFacility.getLinkId()).getToNode(),
				scenario.getNetwork().getLinks().get(toFacility.getLinkId()).getFromNode(), departureTime, person, null);
		double travelTime = 0.0;
		double distance = 0.0;
		
		boolean walk = false;
		
		boolean pathway = false;
		
		Id startLinkId = fromFacility.getLinkId();
		
		Id endLinkId = null;
	
		path.links.add(0, scenario.getNetwork().getLinks().get(fromFacility.getLinkId()));
		path.links.add(scenario.getNetwork().getLinks().get(toFacility.getLinkId()));
		
		for (Link l : path.links) {
			
			if (!pathwayLinks.contains(l.getId()) && pathway) {
				
				GenericRouteImpl route = new GenericRouteImpl(startLinkId, endLinkId);
				
				route.setTravelTime(travelTime);
				route.setDistance(distance);
				final Leg leg = new LegImpl( "movingpathways" );
				leg.setRoute(route);
				trip.add( leg );
				
				walk = true;
				pathway = false;
				distance = l.getLength();
				startLinkId = l.getId();
				endLinkId = l.getId();
								
			}
			else if (!pathwayLinks.contains(l.getId()) && !pathway) {
				
				distance += l.getLength();				
				endLinkId = l.getId();
				walk = true;
				
				if (l.getId().toString().equals(toFacility.getLinkId().toString())) {
					GenericRouteImpl route = new GenericRouteImpl(startLinkId, endLinkId);
					
					route.setTravelTime(distance / Double.parseDouble(scenario.getConfig().getModule("planscalcroute").getParams().get("teleportedModeSpeed_walk")));
					route.setDistance(distance);
					final Leg leg = new LegImpl( "walk" );
					leg.setRoute(route);
					trip.add( leg );
					
				}
				
			}
			else if (pathwayLinks.contains(l.getId()) && !walk) {
				
				distance += l.getLength();
				travelTime += l.getLength()/linkSpeeds.get(l.getId());
				endLinkId = l.getId();
				pathway = true;
				
				if (l.getId().toString().equals(toFacility.getLinkId().toString())) {
					GenericRouteImpl route = new GenericRouteImpl(startLinkId, endLinkId);
					
					route.setTravelTime(travelTime);
					route.setDistance(distance);
					final Leg leg = new LegImpl( "movingpathways" );
					leg.setRoute(route);
					trip.add( leg );
					
				}
				
			}
			else if (pathwayLinks.contains(l.getId()) && walk) {
				
				GenericRouteImpl route = new GenericRouteImpl(startLinkId, endLinkId);
				
				route.setTravelTime(distance / Double.parseDouble(scenario.getConfig().getModule("planscalcroute").getParams().get("teleportedModeSpeed_walk")));
				route.setDistance(distance);
				final Leg leg = new LegImpl( "walk" );
				leg.setRoute(route);
				trip.add( leg );
				
				walk = false;
				pathway = true;
				distance = l.getLength();
				travelTime = l.getLength()/linkSpeeds.get(l.getId());
				startLinkId = l.getId();
				endLinkId = l.getId();
								
			}
			
		}	
		
		return trip;
		
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		// TODO SHould include the station where the moving pathway was entered
		return EmptyStageActivityTypes.INSTANCE;

	}
	
	

}

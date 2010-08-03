package playground.christoph.multimodal.router;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;

/*
 * Agents try to reach an Activity on a Link which does not support
 * the transport mode of the Leg.
 * 
 * PT is currently possible on car/bike/walk links, therefore we never 
 * have to relocate a PT leg.
 * 
 * If an Activities Location is changed, the Routes of its to and from Legs
 * are set to null.
 */
public class CheckActivityReachability {

	private static final Logger log = Logger.getLogger(CheckActivityReachability.class);
	
	private Scenario scenario;
		
	private QuadTree<Facility> carLinkQuadTree;
	private QuadTree<Facility> bikeLinkQuadTree;
	private QuadTree<Facility> walkLinkQuadTree;
	private QuadTree<Facility> allModesLinkQuadTree;
	
	public CheckActivityReachability(Scenario scenario) {
		this.scenario = scenario;
		buildFacilityQuadTrees();
	}
	
	public void checkAndUpdateActivityFacilities() {
		int counter = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			int index = 0;
			int planElements = person.getSelectedPlan().getPlanElements().size();
			
			// if the plan contains no legs
			if (planElements <= 1) continue; 
						
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
								
				if (planElement instanceof Activity) {
					boolean relocateActivity = false;
					Set<String> requiredModes = new HashSet<String>();
					
					Activity activity = (Activity) planElement;
					
					// if no LinkId is available we try to get one
					if (activity.getLinkId() == null) {
						// if also no FacilityId we cannot assign a link -> do nothing
						if (activity.getFacilityId() == null) continue;
						
						/*
						 *  A FacilityId was found. Try to get Facility and assign its LinkId to
						 *  the Activity. It that is not possible, nothing is done.
						 */
						else {
							Facility facility = ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().get(activity.getFacilityId());
							if (facility != null) {
								if (facility.getLinkId() != null) {
									((ActivityImpl)activity).setLinkId(facility.getLinkId());
								}
								else continue;
							}
							else continue;
						}
					}
					
					Link link = scenario.getNetwork().getLinks().get(activity.getLinkId());
					
					Set<String> allowedModes = link.getAllowedModes();
					
					// PT is currently allowed on all kinds of links
					allowedModes.add(TransportMode.pt);
					
					// if car mode is allowed, also ride mode is allowed
					if (allowedModes.contains(TransportMode.car)) allowedModes.add(TransportMode.ride);
					
					// if its the first Activity
					if (index == 0) {
						Leg nextLeg = (Leg) person.getSelectedPlan().getPlanElements().get(index + 1);
						if (!allowedModes.contains(nextLeg.getMode())) {
							relocateActivity = true;
							requiredModes.add(nextLeg.getMode());
							nextLeg.setRoute(null);
						}
					}
					// if it is the last Activity
					else if (index == planElements - 1) {
						Leg previousLeg = (Leg) person.getSelectedPlan().getPlanElements().get(index - 1);
						if (!allowedModes.contains(previousLeg.getMode())) {
							relocateActivity = true;
							requiredModes.add(previousLeg.getMode());
							previousLeg.setRoute(null);
						}
					}
					// in between Activity
					else {
						
						Leg previousLeg = (Leg) person.getSelectedPlan().getPlanElements().get(index - 1);
						Leg nextLeg = (Leg) person.getSelectedPlan().getPlanElements().get(index + 1);

						if (!allowedModes.contains(nextLeg.getMode()) || !allowedModes.contains(previousLeg.getMode())) {
							relocateActivity = true;
							requiredModes.add(nextLeg.getMode());
							requiredModes.add(previousLeg.getMode());
							nextLeg.setRoute(null);
							previousLeg.setRoute(null);
						}
					}			

					// if we have to relocate the Activity
					if (relocateActivity) {
						boolean carLeg = requiredModes.contains(TransportMode.car);
						boolean bikeLeg = requiredModes.contains(TransportMode.bike);
						boolean walkLeg = requiredModes.contains(TransportMode.walk);
						boolean rideLeg = requiredModes.contains(TransportMode.ride);
						/*
						 *  If the modes of from- and toLeg differs we select a facility
						 *  on a link which supports all transport modes. 
						 */
						Facility newFacility = null;
						if ((carLeg || rideLeg) && (bikeLeg || walkLeg)) {
							newFacility = allModesLinkQuadTree.get(activity.getCoord().getX(), activity.getCoord().getY());
						} else if (carLeg || rideLeg) {
							newFacility = carLinkQuadTree.get(activity.getCoord().getX(), activity.getCoord().getY());
						} else if (bikeLeg) {
							newFacility = bikeLinkQuadTree.get(activity.getCoord().getX(), activity.getCoord().getY());
						} else if (walkLeg) {
							newFacility = walkLinkQuadTree.get(activity.getCoord().getX(), activity.getCoord().getY());
						}
						
						if (newFacility != null) {
							((ActivityImpl) activity).setFacilityId(newFacility.getId());
							((ActivityImpl) activity).setLinkId(newFacility.getLinkId());
							((ActivityImpl) activity).setCoord(newFacility.getCoord());
						}
						else {
							log.error("Could not relocate Activity");
						}
						counter++;
					}
				}				
				index++;
				
			}
		}
		log.info("Relocated Activities: " + counter);
	}
	
	private void buildFacilityQuadTrees() {

		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		
		for (Facility facility : ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().values()) {
			if (facility.getCoord().getX() < minx) { minx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() < miny) { miny = facility.getCoord().getY(); }
			if (facility.getCoord().getX() > maxx) { maxx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() > maxy) { maxy = facility.getCoord().getY(); }
		}
		
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		
		log.info("QuadTrees: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		
		carLinkQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		bikeLinkQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		walkLinkQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		allModesLinkQuadTree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		
		for (Facility facility : ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().values()) {
			Link link = scenario.getNetwork().getLinks().get(facility.getLinkId());
			
			Set<String> allowedModes = link.getAllowedModes();
			if (allowedModes.contains(TransportMode.car)) carLinkQuadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			if (allowedModes.contains(TransportMode.bike)) bikeLinkQuadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			if (allowedModes.contains(TransportMode.walk)) walkLinkQuadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			
			if (allowedModes.contains(TransportMode.car) && allowedModes.contains(TransportMode.bike) && allowedModes.contains(TransportMode.walk))
				allModesLinkQuadTree.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);			
		}
		
		log.info("CarLinks:     " + carLinkQuadTree.size());
		log.info("BikeLinks:    " + bikeLinkQuadTree.size());
		log.info("WalkLinks:    " + walkLinkQuadTree.size());
		log.info("AllModeLinks: " + allModesLinkQuadTree.size());
	}
}

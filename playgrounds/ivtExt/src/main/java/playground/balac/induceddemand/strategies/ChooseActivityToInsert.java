package playground.balac.induceddemand.strategies;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ChooseActivityToInsert implements PlanAlgorithm {

	private final Random rng;
	private final StageActivityTypes stageActivityTypes;
	private Scenario scenario;
	
	private QuadTree<ActivityFacility> shopFacilityQuadTree;
	private QuadTree<ActivityFacility> leisureFacilityQuadTree;
	
	private Map<Id<Person>, Set<String>> perPersonAllActivities;

	
	public ChooseActivityToInsert(Random localInstance,
			StageActivityTypes stageActivityTypes, Scenario scenario,
			QuadTree<ActivityFacility> shopFacilityQuadTree, QuadTree<ActivityFacility> leisureFacilityQuadTree) {		
			
		this.rng = localInstance;
		this.stageActivityTypes = stageActivityTypes;
		this.scenario = scenario;
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;

	}
	
	private Set<String> findPossibleInsertActivities(Plan plan) {
		
		
		Person person = plan.getPerson();
		
		Set<String> possibleActivities = perPersonAllActivities.get(person.getId());
		
		for (PlanElement pe : plan.getPlanElements()) {
			
			if (pe instanceof Activity) {
				possibleActivities.remove(((Activity) pe).getType());
			}			
		}
		
		return possibleActivities;
		
	}

	@Override
	public void run(Plan plan) {
		if (!Boolean.parseBoolean(this.scenario.getConfig().getModule("ActivityStrategies").getValue("useInsertActivityStrategy"))) 
			return;
		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);
		
		
		if (t.size() > 10)
			return;
		
		else {
			
			//String actTypes = (String) this.scenario.getPopulation().getPersonAttributes().getAttribute(plan.getPerson().getId().toString(),
			//		"activities");
			
			//String[] allActTypes = actTypes.split(",");
			
			String[] allActTypes = (String[]) findPossibleInsertActivities(plan).toArray();
			
			int index = this.rng.nextInt(allActTypes.length);

			int randomIndex = this.rng.nextInt(t.size() - 1) + 1;
			
			int actIndex = plan.getPlanElements().indexOf(t.get(randomIndex));
			
			ActivityFacility actFacility;
			
			Activity primaryActivity;
			
			Activity newActivity;
			
			if (allActTypes[index].startsWith("home")) {
				
				primaryActivity = getPersonHomeLocation(t);					
				
				newActivity = PopulationUtils.createActivityFromLinkId(allActTypes[index], primaryActivity.getLinkId());
				
				newActivity.setFacilityId(primaryActivity.getFacilityId());
				newActivity.setCoord(primaryActivity.getCoord());
				newActivity.setEndTime(  t.get(randomIndex - 1).getEndTime() + 3600.0);

			//	newActivity.setMaximumDuration(3600);
				
			}
			
			else if (allActTypes[index].startsWith("work")) {
				
				primaryActivity = getPersonWorkLocation(t);
				
				newActivity = PopulationUtils.createActivityFromLinkId(allActTypes[index], primaryActivity.getLinkId());
				
				newActivity.setFacilityId(primaryActivity.getFacilityId());
				newActivity.setCoord(primaryActivity.getCoord());
				newActivity.setEndTime(  t.get(randomIndex - 1).getEndTime() + 3600.0);

			//	newActivity.setMaximumDuration(3600);

			}
			
			else if (allActTypes[index].startsWith("education")) {
				
				primaryActivity = getPersonEducationLocation(t);
				
				newActivity = PopulationUtils.createActivityFromLinkId(allActTypes[index], primaryActivity.getLinkId());
				
				newActivity.setFacilityId(primaryActivity.getFacilityId());
				newActivity.setCoord(primaryActivity.getCoord());
				newActivity.setEndTime(  t.get(randomIndex - 1).getEndTime() + 3600.0);
				//newActivity.setMaximumDuration(3600);

			}
			
			else {
				
				actFacility = findActivityLocation(allActTypes[index], 
						((Activity)plan.getPlanElements().get(actIndex)).getCoord());
				
				newActivity = PopulationUtils.createActivityFromLinkId(allActTypes[index], actFacility.getLinkId());
				
				newActivity.setFacilityId(actFacility.getId());
				newActivity.setCoord(actFacility.getCoord());
				//TODO: put the end time to the typical duration from this person's desire
				newActivity.setEndTime(  t.get(randomIndex - 1).getEndTime() + 3600.0);

				//newActivity.setMaximumDuration(3600);
				
			}
			
			
			
			plan.getPlanElements().add(actIndex, newActivity);
			if (actIndex == 0)
				plan.getPlanElements().add(actIndex + 1, PopulationUtils.createLeg(( (Leg) plan.getPlanElements().get(actIndex + 2) ).getMode()));			
			else
				plan.getPlanElements().add(actIndex + 1, PopulationUtils.createLeg(( (Leg) plan.getPlanElements().get(actIndex - 1) ).getMode()));			
		}		
	}

	private Activity getPersonEducationLocation(List<Activity> allActivities) {
		for(Activity a : allActivities) 
			
			if (a.getType().equals("education"))
				
				return a;
		
		throw new NullPointerException("The activity type education is not known to the agent!");
	}

	private Activity getPersonWorkLocation(List<Activity> allActivities) {
		
		for(Activity a : allActivities) 
			
			if (a.getType().equals("work"))
				
				return a;
		
		throw new NullPointerException("The activity type work is not known to the agent!");
	}

	private Activity getPersonHomeLocation(List<Activity> allActivities) {
		
		for(Activity a : allActivities) 
			
			if (a.getType().equals("home"))
				
				return a;
		
		throw new NullPointerException("The activity type home is not known to the agent!");
	}

	private ActivityFacility findActivityLocation(String actType, Coord coord) {		
		
		if (actType.equals("leisure"))
			return (ActivityFacility)leisureFacilityQuadTree.getClosest(coord.getX(), coord.getY());		

		else if (actType.equals("shopping"))
		
			return (ActivityFacility)shopFacilityQuadTree.getClosest(coord.getX(), coord.getY());		
		else 
			throw new NullPointerException("The activity type: " + actType + " ,is not known!");
		
	}

}

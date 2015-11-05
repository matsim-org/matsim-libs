package playground.balac.iduceddemand.strategies;

import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ChooseActivityToInsert implements PlanAlgorithm {

	private final Random rng;
	private final StageActivityTypes stageActivityTypes;
	private Scenario scenario;
	
	private QuadTree<ActivityFacility> shopFacilityQuadTree;
	private QuadTree<ActivityFacility> leisureFacilityQuadTree;

	
	public ChooseActivityToInsert(Random localInstance,
			StageActivityTypes stageActivityTypes, Scenario scenario,
			QuadTree<ActivityFacility> shopFacilityQuadTree, QuadTree<ActivityFacility> leisureFacilityQuadTree) {		
			
		this.rng = localInstance;
		this.stageActivityTypes = stageActivityTypes;
		this.scenario = scenario;
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;

	}

	@Override
	public void run(Plan plan) {

		List<Activity> t = TripStructureUtils.getActivities(plan, this.stageActivityTypes);
		
		if (t.size() > 10)
			return;
		
		else {
			
			String actTypes = (String) this.scenario.getPopulation().getPersonAttributes().getAttribute(plan.getPerson().getId().toString(),
					"activities");
			
			String[] allActTypes = actTypes.split(",");
			
			int index = this.rng.nextInt(allActTypes.length);

			int actIndex = plan.getPlanElements().indexOf(t.get(this.rng.nextInt(t.size() - 1)));
			
			ActivityFacility actFacility;
			
			Activity primaryActivity;
			
			ActivityImpl newActivity;
			
			if (allActTypes[index].equals("home")) {
				
				primaryActivity = getPersonHomeLocation(t);					
				
				newActivity = new ActivityImpl(allActTypes[index],
						((NetworkImpl) scenario.getNetwork() ).getNearestLinkExactly(primaryActivity.getCoord() ).getId());
				
				newActivity.setFacilityId(primaryActivity.getFacilityId());
				newActivity.setCoord(primaryActivity.getCoord());
				newActivity.setMaximumDuration(3600);
				
			}
			
			else if (allActTypes[index].equals("work")) {
				
				primaryActivity = getPersonWorkLocation(t);
				
				newActivity = new ActivityImpl(allActTypes[index],
						((NetworkImpl) scenario.getNetwork() ).getNearestLinkExactly(primaryActivity.getCoord() ).getId());
				
				newActivity.setFacilityId(primaryActivity.getFacilityId());
				newActivity.setCoord(primaryActivity.getCoord());
				newActivity.setMaximumDuration(3600);

			}
			
			else {
				
				actFacility = findActivityLocation(allActTypes[index], 
						((Activity)plan.getPlanElements().get(actIndex)).getCoord());
				
				newActivity = new ActivityImpl(allActTypes[index],
						((NetworkImpl) scenario.getNetwork() ).getNearestLinkExactly(actFacility.getCoord() ).getId());
				
				newActivity.setFacilityId(actFacility.getId());
				newActivity.setCoord(actFacility.getCoord());
				newActivity.setMaximumDuration(3600);
				
			}
			
			
			
			plan.getPlanElements().add(actIndex, newActivity);
			if (actIndex == 0)
				plan.getPlanElements().add(actIndex + 1, new LegImpl( ( (Leg) plan.getPlanElements().get(actIndex + 2) ).getMode() ));			
			else
				plan.getPlanElements().add(actIndex + 1, new LegImpl( ( (Leg) plan.getPlanElements().get(actIndex - 1) ).getMode() ));			
		}		
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

		else if (actType.equals("shop"))
		
			return (ActivityFacility)shopFacilityQuadTree.getClosest(coord.getX(), coord.getY());		
		else 
			throw new NullPointerException("The activity type is not known!");
		
	}

}

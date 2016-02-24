package playground.sergioo.weeklySimulation.preprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;

import playground.sergioo.weeklySimulation.population.PopulationWriter;

public class WeeklyPlans {
	
	private static final double MON_FRI_PROB = 0.8;
	private static final double MON_SAT_PROB = 0.1;
	private static final double PROB_HOME = 0.5;
	private static final double MAX_DISTANCE = 10000;
	private static Map<String, Set<ActivityFacility>> facilitiesWithType;

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, args[0]);
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimPopulationReader(scenario).readFile(args[1]);
		new MatsimFacilitiesReader(scenario).readFile(args[2]);
		facilitiesWithType = new HashMap<String, Set<ActivityFacility>>();
		for(String type:config.findParam("locationchoice", "flexible_types").split(","))
			facilitiesWithType.put(type.trim(), new HashSet<ActivityFacility>());
		for(ActivityFacility facility:scenario.getActivityFacilities().getFacilities().values())
			for(String typeOption:facility.getActivityOptions().keySet())
				if(facilitiesWithType.get(typeOption)!=null)
					facilitiesWithType.get(typeOption).add(facility);
		int k=0;
		for(Person person:scenario.getPopulation().getPersons().values()) {
			if(k++%1000==0)
				System.out.println(k);
			double random = Math.random();
			for(Plan plan:person.getPlans()) {
				List<PlanElement> dailyPlanElements = new ArrayList<PlanElement>();
				for(PlanElement planElement:plan.getPlanElements()) {
					dailyPlanElements.add(planElement);
					if(planElement instanceof Activity && ((Activity)planElement).getEndTime()>Time.MIDNIGHT) {
						((ActivityImpl)planElement).setEndTime(Time.MIDNIGHT);	
						break;
					}	
				}
				for(int i=1; i<5; i++)
					addCopiedDay(plan, dailyPlanElements, i, scenario.getActivityFacilities());
				if(random>MON_FRI_PROB)
					addCopiedDay(plan, dailyPlanElements, 5, scenario.getActivityFacilities());
				else
					addSecondaryActivitiesDay(plan, 5, config);
				if(random>MON_FRI_PROB+MON_SAT_PROB)
					addCopiedDay(plan, dailyPlanElements, 6, scenario.getActivityFacilities());
				else
					addSecondaryActivitiesDay(plan, 6, config);
				((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1)).setEndTime(Time.UNDEFINED_TIME);
			}
		}
		new PopulationWriter(scenario.getPopulation()).write(args[3]);
	}

	private static void addCopiedDay(Plan plan, List<PlanElement> dailyPlanElements,
			int dayPos, ActivityFacilities activityFacilities) {
		boolean carAvailable = !PersonUtils.getCarAvail(plan.getPerson()).equals("never") && PersonUtils.hasLicense(plan.getPerson());
		for(PlanElement planElement:dailyPlanElements) {
			PlanElement planElementCopy;
			if(planElement instanceof Activity) {
				planElementCopy = new ActivityImpl((Activity)planElement);
				((ActivityImpl)planElementCopy).setEndTime(dayPos*Time.MIDNIGHT+((Activity)planElement).getEndTime());
				PlanElement last = plan.getPlanElements().get(plan.getPlanElements().size()-1);
				if(last instanceof Activity)
					if(((Activity)planElement).getType().equals(((Activity)last).getType()) && ((Activity)planElement).getFacilityId().equals(((Activity)last).getFacilityId()))
						plan.getPlanElements().remove(last);
					else if(((Activity)planElement).getFacilityId().equals(((Activity)last).getFacilityId()))
						plan.addLeg(new LegImpl(TransportMode.transit_walk));
					else
						plan.addLeg(new LegImpl(carAvailable?TransportMode.car:TransportMode.pt));
			}
			else
				planElementCopy = new LegImpl((LegImpl)planElement);
			plan.getPlanElements().add(planElementCopy);
		}
	}

	private static void addSecondaryActivitiesDay(Plan plan, int dayPos, Config config) {
		Id<ActivityFacility> homeFacilityId = null;
		Coord homeCoord = null;
		for(PlanElement planElement:plan.getPlanElements())
			if(planElement instanceof Activity && ((Activity)planElement).getType().equals("home")) {
				homeFacilityId = ((Activity)planElement).getFacilityId();
				homeCoord = ((Activity)planElement).getCoord();
			}
		boolean carAvailable = !PersonUtils.getCarAvail(plan.getPerson()).equals("never") && PersonUtils.hasLicense(plan.getPerson());
		double totalDurations = 9*3600;
		try {
			totalDurations = new NormalDistribution(9*3600, 2*3600).inverseCumulativeProbability(Math.random());
		} catch (NotStrictlyPositiveException e) {
			e.printStackTrace();
		}
		((ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1)).setEndTime(dayPos*Time.MIDNIGHT+totalDurations);
		String typeLast = ((ActivityImpl)plan.getPlanElements().get(0)).getType();
		Coord lastCoord = ((ActivityImpl)plan.getPlanElements().get(0)).getCoord();
		Id<ActivityFacility> lastFacilityId = ((ActivityImpl)plan.getPlanElements().get(0)).getFacilityId();
		Activity activity = null;
		while(totalDurations<Time.MIDNIGHT-2*3600) {
			String prevActivityType = ((ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1)).getType();
			Coord prevActivityCoord = ((ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1)).getCoord();
			plan.addLeg(new LegImpl(carAvailable?TransportMode.car:TransportMode.pt));
			String type = !prevActivityType.equals("home") && Math.random()<PROB_HOME?"home":getRandomActivityType(config.findParam("locationchoice", "flexible_types").split(","));
			double duration;
			try {
				duration = new NormalDistribution(config.planCalcScore().getActivityParams(type).getTypicalDuration(), config.planCalcScore().getActivityParams(type).getMinimalDuration()/2).inverseCumulativeProbability(Math.random());
			} catch (NotStrictlyPositiveException e) {
				e.printStackTrace();
				return;
			}
			totalDurations += duration;
			if(type.equals("home")) {
				activity = new ActivityImpl(type, homeCoord);
				((ActivityImpl)activity).setFacilityId(homeFacilityId);
			}
			else {
				ActivityFacility facility = getRandomFacilityAndLinkId(type, prevActivityCoord);
				activity = new ActivityImpl(type, facility.getCoord());
				((ActivityImpl)activity).setFacilityId(facility.getId());
			}
			((ActivityImpl)activity).setEndTime(dayPos*Time.MIDNIGHT+totalDurations);
			plan.addActivity(activity);
		}
		if(activity==null || !activity.getType().equals(typeLast) || !activity.getFacilityId().equals(lastFacilityId)) {
			if(totalDurations>=Time.MIDNIGHT) {
				totalDurations = Time.MIDNIGHT;
				((ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1)).setEndTime(dayPos*Time.MIDNIGHT+totalDurations-1);
			}
			plan.addLeg(new LegImpl(carAvailable?TransportMode.car:TransportMode.pt));
			totalDurations = Time.MIDNIGHT;
			activity = new ActivityImpl(typeLast, lastCoord);
			((ActivityImpl)activity).setFacilityId(lastFacilityId);
			((ActivityImpl)activity).setEndTime(dayPos*Time.MIDNIGHT+totalDurations);
			plan.addActivity(activity);
		}
	}
	
	private static ActivityFacility getRandomFacilityAndLinkId(String type, Coord lastCoord) {
		do {
			int random = (int) (Math.random()*facilitiesWithType.size());
			boolean next = false;
			for(ActivityFacility facility:facilitiesWithType.get(type)) {
				if(random==0)
					next = true;
				if(next && CoordUtils.calcEuclideanDistance(facility.getCoord(), lastCoord)<MAX_DISTANCE)
					return facility;
				else
					random--;
			}
		}
		while(true);
	}

	private static String getRandomActivityType(String[] strings) {
		int random = (int) (Math.random()*strings.length);
		for(String type:strings) {
			if(random==0)
				return type.trim();
			random--;
		}
		return null;
	}
}

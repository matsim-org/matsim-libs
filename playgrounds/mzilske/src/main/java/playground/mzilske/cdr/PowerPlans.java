package playground.mzilske.cdr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import playground.mzilske.util.PowerList;

public class PowerPlans {

	public static void writePermutations(Scenario scenario, File file) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		TripRouter tripRouter = new TripRouterFactoryImpl(
				ConfigUtils.createConfig(), 
				scenario.getNetwork(), 
				new OnlyTimeDependentTravelDisutilityFactory(), 
				new FreeSpeedTravelTime(), new DijkstraFactory(), new PopulationFactoryImpl(ScenarioUtils.createScenario(ConfigUtils.createConfig())), new ModeRouteFactory(), null, null)
		.instantiateAndConfigureTripRouter();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			double distance = PowerPlans.distance(scenario.getNetwork(), plan);
			List<Activity> activities = TripStructureUtils.getActivities(plan.getPlanElements(), new StageActivityTypesImpl());
			for (List<Activity> someActivities : PowerList.powerList(activities)) {
				List<Activity> otherActivities = new ArrayList<Activity>(activities);
				otherActivities.removeAll(someActivities);
				List<PlanElement> pes = PowerPlans.makePlan(someActivities);
				double partialDistance = PowerPlans.distance(scenario.getNetwork(), tripRouter, pes);	
				pw.printf("%.0f\t%s\t%s\n", distance - partialDistance, PowerPlans.formatDurations(someActivities), PowerPlans.formatDurations(otherActivities));
			}
	
		}
		pw.close();
	}

	static List<PlanElement> makePlan(List<Activity> someActivities) {
		List<PlanElement> pes = new ArrayList<PlanElement>();
		int k = 0;
		for (Activity activity : someActivities) {
			if (k != 0) {
				pes.add(new LegImpl("car"));
			}
			pes.add(activity);
			k++;
		}
		return pes;
	}

	static double distance(Network network, TripRouter tripRouter,
			List<PlanElement> pes) {
		List<Trip> trips = TripStructureUtils.getTrips(pes, new StageActivityTypesImpl());
		List<Leg> legs = new ArrayList<Leg>();
		for (Trip trip : trips) {
			legs.addAll(TripStructureUtils.getLegs((List<PlanElement>) tripRouter.calcRoute("car", new ActivityWrapperFacility(trip.getOriginActivity()), new ActivityWrapperFacility(trip.getDestinationActivity()), trip.getOriginActivity().getEndTime(), null)));
		}
		double travelDistance = 0.0;
		for (Leg leg : legs) {
			travelDistance += PowerPlans.calcDistance(((NetworkRoute) leg.getRoute()), network);
		}
		return travelDistance;
	}

	public static double calcDistance(final NetworkRoute route, final Network network) {
		// In contrast to the core implementation, this version also adds the length of the endLink.
		// If I don't do that, "travelled distances" calculated by this measure become _less_ with increased
		// accuracy, because of intermediate activities and one lost link per route.
		double dist = 0;
		for (Id linkId : route.getLinkIds()) {
			dist += network.getLinks().get(linkId).getLength();
		}
		dist += network.getLinks().get(route.getEndLinkId()).getLength();
		return dist;
	}

	public static double distance(Network network, Plan plan) {
		double personKm = 0.0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (leg.getRoute() instanceof NetworkRoute) {
					NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
					personKm += calcDistance(networkRoute, network);
				}
			}
		}
		return personKm;
	}

	static String formatDurations(List<Activity> someActivities) {
		StringBuilder builder = new StringBuilder();
		for (int i=0; i < someActivities.size(); i++) {
			if (i>0) {
				builder.append(',');
			}
			builder.append(String.format("%.0f", PowerPlans.duration(someActivities.get(i))));
		}
		return builder.toString();
	}

	static double duration(Activity act) {
		double startTime = act.getStartTime();
		if (Double.isInfinite(startTime)) {
			startTime = 0.0;
		}
		double endTime = act.getEndTime();
		if (Double.isInfinite(endTime)) {
			endTime = 30*60*60;
		}
		double duration = endTime - startTime;
		duration = Math.max(0, duration);
		return duration;
	}

	static double calculatePlanDistanceWithout(int i, Plan plan, Network network, TripRouter tripRouter) {
		List<PlanElement> pes = new ArrayList<PlanElement>();
		Activity without = (Activity) plan.getPlanElements().get(i);
		for (int k = 0; k < plan.getPlanElements().size(); k++) {
			if (k < i-1 || k > i+1) {
				pes.add(plan.getPlanElements().get(k));
			}
		}
	
		return distance(network, tripRouter, pes);
	}

	static double calculatePlanDistanceWithoutThisAndShorter(double duration,
			Plan plan, Network network, TripRouter tripRouter) {
		List<PlanElement> pes = new ArrayList<PlanElement>();
		int k = 0;
		for (Activity activity : TripStructureUtils.getActivities(plan, new StageActivityTypesImpl())) {
			if (activity.getMaximumDuration() > duration) {
				if (k != 0) {
					pes.add(new LegImpl("car"));
				}
				pes.add(activity);
				k++;
			}
		}
	
		return distance(network, tripRouter, pes);
	}

	public static void writeActivityDurations(Scenario scenario, File file)
			throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		TripRouter tripRouter = new TripRouterFactoryImpl(
				ConfigUtils.createConfig(), 
				scenario.getNetwork(), 
				new OnlyTimeDependentTravelDisutilityFactory(), 
				new FreeSpeedTravelTime(), new DijkstraFactory(), new PopulationFactoryImpl(ScenarioUtils.createScenario(ConfigUtils.createConfig())), new ModeRouteFactory(), null, null)
		.instantiateAndConfigureTripRouter();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			double distance = distance(scenario.getNetwork(), plan);
			for (int i=0; i < plan.getPlanElements().size(); i++) {
				PlanElement pe = plan.getPlanElements().get(i);
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					double duration = duration(act);
					double without = calculatePlanDistanceWithout(i, plan, scenario.getNetwork(), tripRouter);
					double withoutThisAndWithoutAllShorter = calculatePlanDistanceWithoutThisAndShorter(duration, plan, scenario.getNetwork(), tripRouter);
					pw.printf("%f\t%f\t%f\n", duration, distance - without, distance - withoutThisAndWithoutAllShorter);
				}
				i++;
			}
		}
		pw.close();
	}

	public static double drivenKilometersWholeDay(Scenario scenario,
			VolumesAnalyzer volumes) {
		double sum = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			int[] volumesForLink1 = CompareMain.getVolumesForLink(volumes, link);
			int sum1 = 0;
			for (int i = 0; i < volumesForLink1.length; ++i) {
				sum1 += volumesForLink1[i];
			}
			sum += sum1 * link.getLength();
		}
		return sum;
	}

	public static Map<Id, Double> travelledDistancePerPerson(Population population,
			Network network) {
		Map<Id, Double> result = new HashMap<Id, Double>();
		for (Person person : population.getPersons().values()) {	
			Plan plan = person.getSelectedPlan();
			double personKm = distance(network, plan);
			result.put(person.getId(), personKm);
		}
		return result;
	}

}

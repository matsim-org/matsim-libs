package playground.mzilske.cdr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
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

public class BerlinActivityDurations {

	final static int TIME_BIN_SIZE = 60*60;
	final static int MAX_TIME = 30 * TIME_BIN_SIZE - 1;


	public static void main(String[] args) throws FileNotFoundException {

		Scenario baseScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(baseScenario).readFile("car/output-berlin/ITERS/it.0/2kW.15.0.experienced_plans.xml.gz");
		new MatsimNetworkReader(baseScenario).readFile(BerlinRun.BERLIN_PATH + "network/bb_4.xml.gz");
//		{
//			File file = new File("car/durations-simulated.txt");
//			writeActivityDurations(baseScenario, file);
//		}
		{
			File file = new File("car/permutations.txt");
			writePermutations(baseScenario, file);
		}

		//		sumDistancesAndPutInBasePopulation(baseScenario, baseScenario, "base");
		//
		//		EventsManager events = EventsUtils.createEventsManager();
		//		VolumesAnalyzer baseVolumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
		//		events.addHandler(baseVolumes);
		//		new MatsimEventsReader(events).readFile("car/output-berlin/ITERS/it.0/2kW.15.0.events.xml.gz");
		//		{
		//			File file = new File("car/distances.txt");
		//			PrintWriter pw = new PrintWriter(file);
		//
		//			pw.printf("callrate\troutesum\tvolumesum\tvolumesumdiff\n");
		//			for (int dailyRate : BerlinPhone.CALLRATES) {
		//				distances(pw, dailyRate, baseVolumes, baseScenario);
		//			}
		//			pw.close();
		//		}
		//		
		//		
		//		
		//		PrintWriter pw = new PrintWriter(new File("car/person-kilometers.txt"));
		//		pw.printf("person\tkilometers-base\t");
		//		for (int dailyRate : BerlinPhone.CALLRATES) {
		//			pw.printf("kilometers-%d\t", dailyRate);
		//		}
		//		pw.printf("\n");
		//		for (Person person : baseScenario.getPopulation().getPersons().values()) {
		//			pw.printf("%s\t", person.getId().toString());
		//			pw.printf("%f\t", person.getCustomAttributes().get("kilometers-base"));
		//			for (int dailyRate : BerlinPhone.CALLRATES) {
		//				Double km = (Double) person.getCustomAttributes().get("kilometers-"+dailyRate);
		//				if (km == null) {
		//					// person not seen even once
		//					km = 0.0;
		//				}
		//				pw.printf("%f\t", km);
		//			}
		//
		//			pw.printf("\n");
		//		}
		//		pw.close();

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

	private static void distances(PrintWriter pw, int callrate, VolumesAnalyzer baseVolumes, Scenario baseScenario) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new MatsimPopulationReader(scenario).readFile("car/output-"+callrate+"/ITERS/it.0/0.experienced_plans.xml.gz");

		String suffix = Integer.toString(callrate);
		double km = sumDistancesAndPutInBasePopulation(scenario, baseScenario, suffix);


		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer volumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
		events.addHandler(volumes);
		new MatsimEventsReader(events).readFile("car/output-"+callrate+"/ITERS/it.0/0.events.xml.gz");


		double baseSum = drivenKilometersWholeDay(baseScenario, baseVolumes);
		double sum = drivenKilometersWholeDay(baseScenario, volumes);



		pw.printf("%d\t%f\t%f\t%f\n", callrate, km, sum, baseSum - sum);
		pw.flush();
	}


	private static double sumDistancesAndPutInBasePopulation(Scenario scenario,
			Scenario baseScenario, String suffix) {
		double km = 0.0;
		for (Person person : scenario.getPopulation().getPersons().values()) {

			Plan plan = person.getSelectedPlan();
			double personKm = distance(baseScenario, plan);
			Person basePerson = baseScenario.getPopulation().getPersons().get(person.getId());
			basePerson.getCustomAttributes().put("kilometers-"+suffix, personKm);
			km += personKm;
		}
		return km;
	}


	private static double distance(Scenario baseScenario, Plan plan) {
		double personKm = 0.0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (leg.getRoute() instanceof NetworkRoute) {
					NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
					personKm += calcDistance(networkRoute, baseScenario.getNetwork());
				}
			}
		}
		return personKm;
	}


	private static double drivenKilometersWholeDay(Scenario scenario,
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

	private static void writeActivityDurations(Scenario scenario, File file)
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
			double distance = distance(scenario, plan);
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


	private static double duration(Activity act) {
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


	private static void writePermutations(Scenario scenario, File file) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		TripRouter tripRouter = new TripRouterFactoryImpl(
				ConfigUtils.createConfig(), 
				scenario.getNetwork(), 
				new OnlyTimeDependentTravelDisutilityFactory(), 
				new FreeSpeedTravelTime(), new DijkstraFactory(), new PopulationFactoryImpl(ScenarioUtils.createScenario(ConfigUtils.createConfig())), new ModeRouteFactory(), null, null)
		.instantiateAndConfigureTripRouter();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			double distance = distance(scenario, plan);
			List<Activity> activities = TripStructureUtils.getActivities(plan.getPlanElements(), new StageActivityTypesImpl());
			for (List<Activity> someActivities : PowerList.powerList(activities)) {
				List<Activity> otherActivities = new ArrayList<Activity>(activities);
				otherActivities.removeAll(someActivities);
				List<PlanElement> pes = makePlan(someActivities);
				double partialDistance = distance(scenario.getNetwork(), tripRouter, pes);	
				pw.printf("%.0f\t%s\t%s\n", distance - partialDistance, formatDurations(someActivities), formatDurations(otherActivities));
			}
			
		}
		pw.close();
	}


	private static String formatDurations(List<Activity> someActivities) {
		StringBuilder builder = new StringBuilder();
		for (int i=0; i < someActivities.size(); i++) {
			if (i>0) {
				builder.append(',');
			}
			builder.append(String.format("%.0f", duration(someActivities.get(i))));
		}
		return builder.toString();
	}


	private static List<PlanElement> makePlan(List<Activity> someActivities) {
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


	private static double calculatePlanDistanceWithoutThisAndShorter(double duration,
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


	private static double calculatePlanDistanceWithout(int i, Plan plan, Network network, TripRouter tripRouter) {
		List<PlanElement> pes = new ArrayList<PlanElement>();
		Activity without = (Activity) plan.getPlanElements().get(i);
		for (int k = 0; k < plan.getPlanElements().size(); k++) {
			if (k < i-1 || k > i+1) {
				pes.add(plan.getPlanElements().get(k));
			}
		}

		return distance(network, tripRouter, pes);
	}


	private static double distance(Network network, TripRouter tripRouter,
			List<PlanElement> pes) {
		List<Trip> trips = TripStructureUtils.getTrips(pes, new StageActivityTypesImpl());
		List<Leg> legs = new ArrayList<Leg>();
		for (Trip trip : trips) {
			legs.addAll(TripStructureUtils.getLegs((List<PlanElement>) tripRouter.calcRoute("car", new ActivityWrapperFacility(trip.getOriginActivity()), new ActivityWrapperFacility(trip.getDestinationActivity()), trip.getOriginActivity().getEndTime(), null)));
		}
		double travelDistance = 0.0;
		for (Leg leg : legs) {
			travelDistance += calcDistance(((NetworkRoute) leg.getRoute()), network);
		}
		return travelDistance;
	}

}

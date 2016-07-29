package playground.balac.sbbproject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.utils.Events2TTCalculator;
import playground.balac.utils.NetworkLinkUtils;
import playground.balac.utils.TimeConversion;

public class CarRoutingClaude {

	Config config;
	String configfile = null;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		CarRoutingClaude loadedNetworkRouter = new CarRoutingClaude();
		loadedNetworkRouter.run(args);
	}
	

	public void run(final String[] args) throws IOException {
		
		String rootPathOut = args[2];

		String rootPath = args[1];

		String eventsFile = rootPath + "out.events_new.txt.gz";
		String outputPlansFile = rootPathOut +  "outputPlanFileX.xml";

		final Config config = ConfigUtils.loadConfig(args[0]);
		
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		Network network = sc.getNetwork();
		this.config = sc.getConfig();

		
		
		final Population plans = (Population) sc.getPopulation();
		StreamingUtils.setIsStreaming(plans, true);
		final StreamingPopulationWriter plansWriter = new StreamingPopulationWriter(plans, network);
		plansWriter.startStreaming(outputPlansFile);
		
		// add algorithm to map coordinates to links
		StreamingUtils.addAlgorithm(plans, new org.matsim.core.population.algorithms.XY2Links(network, null));

		// add algorithm to estimate travel cost
		// and which performs routing based on that
		TravelTimeCalculator travelTimeCalculator = Events2TTCalculator.getTravelTimeCalculator(sc, eventsFile);
		TravelDisutilityFactory travelCostCalculatorFactory = new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, config.planCalcScore() );
		TravelDisutility travelCostCalculator = travelCostCalculatorFactory.createTravelDisutility(travelTimeCalculator.getLinkTravelTimes());
		StreamingUtils.addAlgorithm(plans, new PlanRouter(
		new TripRouterFactoryBuilderWithDefaults().build(
				sc ).get(
		) ));

		// add algorithm to write out the plans
		StreamingUtils.addAlgorithm(plans, plansWriter);
		final BufferedReader readLink = IOUtils.getBufferedReader(args[3]);

		//final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/coordinates.txt");
		String s = readLink.readLine();
		s = readLink.readLine();
		
		WGS84toCH1903LV03 transformation = new WGS84toCH1903LV03();
		
		TimeConversion timeConv = new TimeConversion();
		
		NetworkLinkUtils lUtils = new NetworkLinkUtils(sc.getNetwork());

		
		while(s != null) {
			String[] arr = s.split("\\t");

			Coord coordStartT = new Coord(Double.parseDouble(arr[2]), Double.parseDouble(arr[1]));
			
			Coord coordStart = transformation.transform(coordStartT);
			
			System.out.println(coordStart.getX());
			
			Link lStart = lUtils.getClosestLink(coordStart);


			Coord coordEndT = new Coord(Double.parseDouble(arr[4]), Double.parseDouble(arr[3]));
			
			Coord coordEnd = transformation.transform(coordEndT);

			Link lEnd = lUtils.getClosestLink(coordEnd);			
			
			Person person = sc.getPopulation().getFactory().createPerson(Id.createPersonId(arr[0]));
			
			Plan plan = (Plan) sc.getPopulation().getFactory().createPlan();
			Activity act = PopulationUtils.createActivityFromLinkId("home", lStart.getId());
			act.setCoord(coordStart);
			//String[] arr2 = arr[4].split(":");
			//double h = Double.parseDouble(arr2[0]);
			//double m = Double.parseDouble(arr2[1]);
			//act.setEndTime(h * 3600 + m * 60);
			
			double m = TimeConversion.convertTimeToDouble(arr[5]);
			
			act.setEndTime(60.0 * m);
			plan.addActivity(act);
			
			Leg leg = PopulationUtils.createLeg("car");
			plan.addLeg(leg);
			
			act = PopulationUtils.createActivityFromLinkId("leisure", lEnd.getId());
			act.setCoord(coordEnd);
			act.setEndTime(48800);
			plan.addActivity(act);
			leg = PopulationUtils.createLeg("car");
			plan.addLeg(leg);
			act = PopulationUtils.createActivityFromLinkId("home", lStart.getId());
			act.setCoord(coordStart);
			plan.addActivity(act);
			person.addPlan(plan);
			
			sc.getPopulation().addPerson(person);
			s = readLink.readLine();
			
			
		}
		
		final BufferedWriter outLink = IOUtils.getBufferedWriter(args[2] +"/travelTimes_" + args[1]+ ".txt");

		
		for(Person per: sc.getPopulation().getPersons().values()) {
			double time = 0.0;
			Plan p = per.getPlans().get(0);
			
			for(PlanElement pe: p.getPlanElements()) {
				
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().equals("leisure")) {
						
						break;
					}
				}
				else if (pe instanceof Leg) {
					
					time += ((Leg) pe).getTravelTime();
					
				}
				
			}
			
			outLink.write(per.getId() + " ");
			outLink.write(Double.toString(time));
			outLink.newLine();
			
			
		}
		outLink.flush();
		outLink.close();
		PopulationUtils.printPlansCount(plans) ;
		plansWriter.closeStreaming();

		System.out.println("done.");
	}
	
	
}

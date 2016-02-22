package playground.sergioo.calibration2013;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import playground.sergioo.singapore2012.transitLocationChoice.TransitActsRemover;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterWSImplFactory;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeCalculator;
import playground.sergioo.typesPopulation2013.population.MatsimPopulationReader;

import javax.inject.Provider;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class GeneticAlgorithmMode {
	
	private static int NUM_PARAMETERS = 11;
	public static double[] limits = {100, 200, 500, 1000, 2000, 5000};
	private static SortedMap<Double, int[]> distancesHits = createMap();
	private static RouterManager routerManager;
	
	private static class ParametersArray {
		
		private double[] parameters = new double[NUM_PARAMETERS];
		private double score;
		
		public ParametersArray(Scenario scenario) {
			int k=0;
			this.parameters[k++] = scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getConstant();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).getConstant();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getModes().get(TransportMode.walk).getMarginalUtilityOfDistance();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getMarginalUtlOfWaiting_utils_hr();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getMarginalUtlOfWaitingPt_utils_hr();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMonetaryDistanceRate();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).getMonetaryDistanceRate();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getUtilityOfLineSwitch();
			calculateScore(scenario);
		}
		public ParametersArray(double[] parameters, Scenario scenario) {
			this.parameters = parameters;
			modifyConfig(scenario);
			calculateScore(scenario);
		}
		private void modifyConfig(Scenario scenario) {
			int k=0;
			double constantCar = this.parameters[k++];
			scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).setConstant(constantCar);
			double constantPt = this.parameters[k++];
			scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).setConstant(constantPt);
			final double marginalUtlOfDistanceWalk = this.parameters[k++];
			scenario.getConfig().planCalcScore().getModes().get(TransportMode.walk).setMarginalUtilityOfDistance(marginalUtlOfDistanceWalk);
			scenario.getConfig().planCalcScore().setMarginalUtlOfWaiting_utils_hr(this.parameters[k++]);
			scenario.getConfig().planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(this.parameters[k++]);
			final double traveling = this.parameters[k++];
			scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
			final double travelingPt = this.parameters[k++];
			scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).setMarginalUtilityOfTraveling(travelingPt);
			final double travelingWalk = this.parameters[k++];
			scenario.getConfig().planCalcScore().getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(travelingWalk);
			double monetaryDistanceRateCar = this.parameters[k++];
			scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar);
			double monetaryDistanceRatePt = this.parameters[k++];
			scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).setMonetaryDistanceRate(monetaryDistanceRatePt);
			scenario.getConfig().planCalcScore().setUtilityOfLineSwitch(this.parameters[k++]);
		}
		private void calculateScore(final Scenario scenario) {
			TransitActsRemover transitActsRemover = new TransitActsRemover();
			for(Person person:scenario.getPopulation().getPersons().values()) {
				Person copyPerson = PopulationUtils.createPerson(person.getId());
				PersonUtils.setCarAvail(copyPerson, PersonUtils.getCarAvail(person));
				PlanImpl copyPlan = new PlanImpl(copyPerson);
				copyPlan.copyFrom(person.getSelectedPlan());
				copyPerson.addPlan(copyPlan);
				transitActsRemover.run(copyPlan);
				for(PlanElement planElement:copyPlan.getPlanElements())
					if(planElement instanceof Leg)
						((Leg)planElement).setMode("car");
				routerManager.addPlan(copyPlan);
			}
			SortedMap<Double, int[]> distanceDist = routerManager.getDistribution();
			score = compare(distanceDist, distancesHits);
		}
		public double compare(SortedMap<Double, int[]> sim, SortedMap<Double, int[]> hits) {
			double [] bias = new double [limits.length];
			int i = 0;
			double sum1 = 0;
			double sum2 = 0;
			double avg = 0;
			for(double limit:limits){
				double bia = Math.abs((double)hits.get(limit)[0]/(hits.get(limit)[0]+hits.get(limit)[1])-(double)sim.get(limit)[0]/(sim.get(limit)[0]+sim.get(limit)[1]))
						+ Math.abs((double)hits.get(limit)[1]/(hits.get(limit)[0]+hits.get(limit)[1])-(double)sim.get(limit)[1]/(sim.get(limit)[0]+sim.get(limit)[1]));				
				bias[i]=bia*(sim.get(limit)[0]+sim.get(limit)[1]); 
				System.out.println(bias[i]);
				sum1 = sum1 + (sim.get(limit)[0]+sim.get(limit)[1]);
				i=i+1;	
			}
			for(int j=0;j<bias.length; j++)
				sum2 = sum2 + bias[j];
			avg=sum2/sum1;
			return avg;
		}
		private ParametersArray mutate(Scenario scenario) {
			double[] parameters = new double[NUM_PARAMETERS];
			for(int i=0; i<parameters.length; i++)
				parameters[i] = mutate(this.parameters[i]);
			return new ParametersArray(parameters, scenario);
		}
		private double mutate(double d) {
			return d*(0.5+Math.random());
		}
		private ParametersArray recombinate(ParametersArray parametersMatrix, Scenario scenario) {
			double[] parameters = new double[NUM_PARAMETERS];
			for(int i=0; i<parameters.length; i++)
				parameters[i] = (this.parameters[i]+parametersMatrix.parameters[i])/2;
			return new ParametersArray(parameters, scenario);
		}
	
	}

	public static SortedMap<Double, int[]> createMap() {
		Connection c = null;
		Statement stmt1 = null;
		Statement stmt2 = null;
		SortedMap <Double, int[]> obsmap = new TreeMap<Double,int[]>();
		double low = 0;
		try {
			Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection("jdbc:postgresql://localhost:5433/module_viii","igorm", "Highrise#123");
			c.setAutoCommit(false);
			System.out.println("Opened database successfully");
			for(double limit:limits){
				int [] obs = new int[2];
				double high = limit;
				stmt1 = c.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY );
				stmt2 = c.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY );
				ResultSet rscar = stmt1.executeQuery("Select count(travdistance) from u_igorm.person_travel_summary where ((travdistance between "+low+" and "+high+") and (\"driveStages\" is not null or \"psgrStages\" is not null));"); 
				ResultSet rspt = stmt2.executeQuery("Select count(travdistance) from u_igorm.person_travel_summary where ((travdistance between "+low+" and "+high+") and (\"driveStages\" is null or \"psgrStages\" is null));");
				low = high;
				while ( rscar.next() )
					obs[0] = rscar.getInt("count");
				while ( rspt.next() )
					obs[1] = rspt.getInt("count");
				obsmap.put(limit, obs);
				rscar.beforeFirst();
				rspt.beforeFirst();
				rscar.close();
				rspt.close();
				stmt1.close(); 
				stmt2.close();
			}
			c.close();
			System.out.println("Operation done successfully");
			System.out.println(obsmap);
			return obsmap;
		}
		catch ( Exception e ) {
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
			System.exit(0);
		}
		return null;
	}

	public static void main(String[] args) throws IOException {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).setConstant((double) 10);
		scenario.getConfig().planCalcScore().getModes().get(TransportMode.walk).setMarginalUtilityOfDistance((double) 10);
		scenario.getConfig().planCalcScore().getModes().get(TransportMode.pt).setMonetaryDistanceRate((double) 10);
		new MatsimPopulationReader(scenario).readFile(args[1]);
		new MatsimFacilitiesReader(scenario).readFile(args[2]);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[3]);
		new TransitScheduleReader(scenario).readFile(args[4]);
		for(Link link:scenario.getNetwork().getLinks().values()) {
			Set<String> modes = new HashSet<String>(link.getAllowedModes());
			modes.add("pt");
			link.setAllowedModes(modes);
		}
		Set<String> carMode = new HashSet<String>();
		carMode.add("car");
		NetworkImpl justCarNetwork = NetworkImpl.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(justCarNetwork, carMode);
		for(Person person:scenario.getPopulation().getPersons().values())
			for(PlanElement planElement:person.getSelectedPlan().getPlanElements())
				if(planElement instanceof Activity)
					((ActivityImpl)planElement).setLinkId(justCarNetwork.getNearestLinkExactly(((ActivityImpl)planElement).getCoord()).getId());
		for(ActivityFacility facility:scenario.getActivityFacilities().getFacilities().values())
			((ActivityFacilityImpl)facility).setLinkId(justCarNetwork.getNearestLinkExactly(facility.getCoord()).getId());
		EventsManager events = new EventsManagerImpl();
		final TravelTimeCalculator travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		events.addHandler(travelTimeCalculator);
		final WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		events.addHandler(waitTimeCalculator);
		final StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		events.addHandler(stopStopTimeCalculator);
		if(!args[5].equals("NO"))
			new MatsimEventsReader(events).readFile(args[5]);
		routerManager = new RouterManager(scenario.getConfig().global().getNumberOfThreads(), scenario, travelTimeCalculator.getLinkTravelTimes(), waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes());
		final TravelDisutilityFactory factory = new Builder( TransportMode.car, scenario.getConfig().planCalcScore() );
		final TravelDisutility disutility = factory.createTravelDisutility(travelTimeCalculator.getLinkTravelTimes());
		final Provider<TransitRouter> transitRouterFactory = new TransitRouterWSImplFactory(scenario, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes());
		int numIterations = new Integer(args[6]);
		int maxElements = new Integer(args[7]);
		NavigableSet<ParametersArray> memory = new TreeSet<ParametersArray>(new Comparator<ParametersArray>() {
			@Override
			public int compare(ParametersArray o1, ParametersArray o2) {
				return Double.compare(o1.score, o2.score);
			}
		});
		memory.add(new ParametersArray(scenario));
		for(int i=0; i<numIterations; i++) {
			Collection<ParametersArray> tempMemory = new ArrayList<ParametersArray>();
			for(ParametersArray parametersMatrix:memory)
				if(parametersMatrix!=null)
					tempMemory.add(parametersMatrix.mutate(scenario));
			for(ParametersArray parametersMatrix:tempMemory) {
				PrintWriter printer2 = new PrintWriter(new FileWriter(args[9], true));
				printer2.println(Arrays.toString(parametersMatrix.parameters));
				printer2.println(parametersMatrix.score);
				printer2.close();
				memory.add(parametersMatrix);
				System.out.println(parametersMatrix.score+": "+Arrays.toString(parametersMatrix.parameters));
				if(memory.size()>maxElements)
					memory.pollLast();
			}
			System.out.println(memory.first().score);
			PrintWriter printer = new PrintWriter(new FileWriter(args[8], true));
			printer.println(memory.first().score);
			printer.close();
		}
		PrintWriter printer = new PrintWriter(new FileWriter(args[8], true));
		printer.println(Arrays.toString(memory.first().parameters));
		printer.close();
		System.out.println(Arrays.toString(memory.first().parameters));
	}

}

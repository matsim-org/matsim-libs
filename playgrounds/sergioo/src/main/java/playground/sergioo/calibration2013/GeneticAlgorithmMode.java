package playground.sergioo.calibration2013;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ReadOrComputeMaxDCScore;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryImpl;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImplFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playground.sergioo.singapore2012.scoringFunction.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.sergioo.singapore2012.transitLocationChoice.TransitActsRemover;
import playground.sergioo.typesPopulation2013.population.MatsimPopulationReader;

public class GeneticAlgorithmMode {

	private static ReplanningContext context;
	private static ReRoute module;
	private static int NUM_PARAMETERS = 11;
	private static double[] limits = {100, 200, 500, 1000, 2000, 5000};
	private static SortedMap<Double, int[]> distancesHits = createMap();
	
	private static class ParametersArray {
		
		private double[] parameters = new double[NUM_PARAMETERS];
		private double score;
		
		public ParametersArray(Scenario scenario) {
			int k=0;
			this.parameters[k++] = scenario.getConfig().planCalcScore().getConstantCar();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getConstantPt();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getMarginalUtlOfDistanceWalk();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getMarginalUtlOfWaiting_utils_hr();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getMarginalUtlOfWaitingPt_utils_hr();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getTraveling_utils_hr();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getTravelingPt_utils_hr();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getTravelingWalk_utils_hr();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getMonetaryDistanceCostRateCar();
			this.parameters[k++] = scenario.getConfig().planCalcScore().getMonetaryDistanceCostRatePt();
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
			scenario.getConfig().planCalcScore().setConstantCar(this.parameters[k++]);
			scenario.getConfig().planCalcScore().setConstantPt(this.parameters[k++]);
			scenario.getConfig().planCalcScore().setMarginalUtlOfDistanceWalk(this.parameters[k++]);
			scenario.getConfig().planCalcScore().setMarginalUtlOfWaiting_utils_hr(this.parameters[k++]);
			scenario.getConfig().planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(this.parameters[k++]);
			scenario.getConfig().planCalcScore().setTraveling_utils_hr(this.parameters[k++]);
			scenario.getConfig().planCalcScore().setTravelingPt_utils_hr(this.parameters[k++]);
			scenario.getConfig().planCalcScore().setTravelingWalk_utils_hr(this.parameters[k++]);
			scenario.getConfig().planCalcScore().setMonetaryDistanceCostRateCar(this.parameters[k++]);
			scenario.getConfig().planCalcScore().setMonetaryDistanceCostRatePt(this.parameters[k++]);
			scenario.getConfig().planCalcScore().setUtilityOfLineSwitch(this.parameters[k++]);
		}
		private void calculateScore(final Scenario scenario) {
			module.prepareReplanning(context);
			TransitActsRemover transitActsRemover = new TransitActsRemover();
			Collection<PlanImpl> copiedPlansCar = new ArrayList<PlanImpl>();
			Collection<PlanImpl> copiedPlansPT = new ArrayList<PlanImpl>();
			for(Person person:scenario.getPopulation().getPersons().values()) {
				Person copyPerson = new PersonImpl(person.getId());
				PlanImpl copyPlan = new PlanImpl(copyPerson);
				copyPlan.copyFrom(person.getSelectedPlan());
				copyPerson.addPlan(copyPlan);
				transitActsRemover.run(copyPlan);
				for(PlanElement planElement:copyPlan.getPlanElements())
					if(planElement instanceof Leg)
						((Leg)planElement).setMode("car");
				module.handlePlan(copyPlan);
				copiedPlansCar.add(copyPlan);
				copyPerson = new PersonImpl(person.getId());
				copyPlan = new PlanImpl(copyPerson);
				copyPlan.copyFrom(person.getSelectedPlan());
				copyPerson.addPlan(copyPlan);
				transitActsRemover.run(copyPlan);
				for(PlanElement planElement:copyPlan.getPlanElements())
					if(planElement instanceof Leg)
						((Leg)planElement).setMode("pt");
				module.handlePlan(copyPlan);
				copiedPlansPT.add(copyPlan);
			}
			module.finishReplanning();
			SortedMap<Double, int[]> distanceDist = new TreeMap<Double, int[]>();
			for(PlanImpl plan:copiedPlansCar)
				for(PlanElement planElement:plan.getPlanElements())
					if(planElement instanceof Leg)
						if(!((Leg) planElement).getMode().equals("car"))
							try {
								throw new Exception();
							} catch (Exception e) {
								e.printStackTrace();
								System.exit(0);
							}
						else {
							double distance = RouteUtils.calcDistance((NetworkRoute) ((Leg) planElement).getRoute(), scenario.getNetwork());
							boolean in = false;
							LIMITS:
							for(double limit:limits)
								if(distance<limit) {
									int[] nums = distanceDist.get(limit);
									if(nums == null) {
										nums = new int[2];
										distanceDist.put(limit, nums);
									}
									nums[0]++;
									in = true;
									break LIMITS;
								}
							if(!in) {
								int[] nums = distanceDist.get(Double.POSITIVE_INFINITY);
								if(nums == null) {
									nums = new int[2];
									distanceDist.put(Double.POSITIVE_INFINITY, nums);
								}
								nums[0]++;
							}
						}
			double distance = 0;
			for(PlanImpl plan:copiedPlansPT)
				for(PlanElement planElement:plan.getPlanElements())
					if(planElement instanceof Leg)
						if(((Leg) planElement).getMode().equals("car"))
							try {
								throw new Exception();
							} catch (Exception e) {
								e.printStackTrace();
								System.exit(0);
							}
						else if(((Leg) planElement).getMode().equals("pt")) {
							String[] parts = ((GenericRoute)((Leg) planElement).getRoute()).getRouteDescription().split("===");
							NetworkRoute lineRoute = scenario.getTransitSchedule().getTransitLines().get(new IdImpl(parts[2])).getRoutes().get(new IdImpl(parts[3])).getRoute();
							distance += RouteUtils.calcDistance(lineRoute.getSubRoute(scenario.getTransitSchedule().getFacilities().get(new IdImpl(parts[1])).getLinkId(),
									scenario.getTransitSchedule().getFacilities().get(new IdImpl(parts[4])).getLinkId()), scenario.getNetwork());
						}
						else {
							Link start = scenario.getNetwork().getLinks().get(((Leg) planElement).getRoute().getStartLinkId());
							Link end = scenario.getNetwork().getLinks().get(((Leg) planElement).getRoute().getEndLinkId());
							distance += CoordUtils.calcDistance(start.getCoord(), end.getCoord());
						}
					else if(!((Activity)planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
						boolean in = false;
						LIMITS:
						for(double limit:limits)
							if(distance<limit) {
								int[] nums = distanceDist.get(limit);
								if(nums == null) {
									nums = new int[2];
									distanceDist.put(limit, nums);
								}
								nums[1]++;
								in = true;
								break LIMITS;
							}
						if(!in) {
							int[] nums = distanceDist.get(Double.POSITIVE_INFINITY);
							if(nums == null) {
								nums = new int[2];
								distanceDist.put(Double.POSITIVE_INFINITY, nums);
							}
							nums[1]++;
						}
						distance = 0;
					}

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
		new MatsimPopulationReader(scenario).readFile(args[1]);
		new MatsimFacilitiesReader(scenario).readFile(args[2]);
		new MatsimNetworkReader(scenario).readFile(args[3]);
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
		final TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		final TravelDisutilityFactory factory = new TravelCostCalculatorFactoryImpl();
		final TravelDisutility disutility = factory.createTravelDisutility(travelTimeCalculator.getLinkTravelTimes(), scenario.getConfig().planCalcScore());
		final TransitRouterFactory transitRouterFactory = new TransitRouterImplFactory(scenario.getTransitSchedule(), new TransitRouterConfig(scenario.getConfig()));
		context = new ReplanningContext() {
			@Override
			public TravelDisutility getTravelDisutility() {
				return disutility;
			}
			@Override
			public TravelTime getTravelTime() {
				return travelTimeCalculator.getLinkTravelTimes();
			}
			@Override
			public ScoringFunctionFactory getScoringFunctionFactory() {
				return new CharyparNagelOpenTimesScoringFunctionFactory(scenario.getConfig().planCalcScore(), scenario);
			}
			@Override
			public int getIteration() {
				return 1;
			}
			@Override
			public TripRouter getTripRouter() {
				return new TripRouterFactoryImpl(scenario, factory, travelTimeCalculator.getLinkTravelTimes(), new DijkstraFactory(), transitRouterFactory).instantiateAndConfigureTripRouter();
			}
		};
		DestinationChoiceBestResponseContext dcContext = new DestinationChoiceBestResponseContext(scenario);
		dcContext.init();
		ReadOrComputeMaxDCScore rcms = new ReadOrComputeMaxDCScore(dcContext);
		rcms.readOrCreateMaxDCScore(new Controler(scenario), dcContext.kValsAreRead());
		rcms.getPersonsMaxEpsUnscaled();
		module = new ReRoute(scenario);
		int numIterations = new Integer(args[5]);
		int maxElements = new Integer(args[6]);
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
				PrintWriter printer2 = new PrintWriter(new FileWriter(args[8], true));
				printer2.println(Arrays.toString(parametersMatrix.parameters));
				printer2.println(parametersMatrix.score);
				printer2.close();
				memory.add(parametersMatrix);
				System.out.println(parametersMatrix.score+": "+Arrays.toString(parametersMatrix.parameters));
				if(memory.size()>maxElements)
					memory.pollLast();
			}
			System.out.println(memory.first().score);
			PrintWriter printer = new PrintWriter(new FileWriter(args[7], true));
			printer.println(memory.first().score);
			printer.close();
		}
		PrintWriter printer = new PrintWriter(new FileWriter(args[7], true));
		printer.println(Arrays.toString(memory.first().parameters));
		printer.close();
		System.out.println(Arrays.toString(memory.first().parameters));
	}

}

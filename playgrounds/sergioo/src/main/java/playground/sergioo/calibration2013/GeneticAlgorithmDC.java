package playground.sergioo.calibration2013;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.BestReplyDestinationChoice;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.Algotype;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ReadOrComputeMaxDCScore;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.pt.router.TransitRouter;
import playground.sergioo.singapore2012.scoringFunction.CharyparNagelOpenTimesScoringFunctionFactory;
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

//import com.imsl.stat.KolmogorovTwoSample;

public class GeneticAlgorithmDC {

	private static Double avgDistance;
	private static ReplanningContext context;
	private static BestReplyDestinationChoice module;
	private static String actTypes = "biz,errand,medi,eat,rec,shop,social,sport,fun";
	private static int NUM_PARAMETERS = actTypes.split(",").length+9;
	private static Map<String, Map<String, List<Double>>> distancesHits = createMap();

	private static class ParametersArray {
		
		private double[] parameters = new double[NUM_PARAMETERS];
		private double[] scores = new double[actTypes.split(",").length];
		private double score;
		
		public ParametersArray(Scenario scenario) {
			int k=0;
			this.parameters[k++] = Double.parseDouble(scenario.getConfig().findParam("locationchoice", "analysisBinSize"));
			this.parameters[k++] = Double.parseDouble(scenario.getConfig().findParam("locationchoice", "analysisBoundary"));
			String[] parts = scenario.getConfig().findParam("locationchoice", "epsilonScaleFactors").split(",");
			for(String part:parts)
				this.parameters[k++] = Double.parseDouble(part.trim());
			this.parameters[k++] = Double.parseDouble(scenario.getConfig().findParam("locationchoice", "probChoiceSetSize") );
			String radius = scenario.getConfig().findParam("locationchoice", "radius");
			this.parameters[k++] = radius.equals("null")?0.0:Double.parseDouble(radius);
			this.parameters[k++] = Double.parseDouble(scenario.getConfig().findParam("locationchoice", "restraintFcnExp"));
			this.parameters[k++] = Double.parseDouble(scenario.getConfig().findParam("locationchoice", "restraintFcnFactor"));
			this.parameters[k++] = Double.parseDouble(scenario.getConfig().findParam("locationchoice", "scaleFactor"));
			this.parameters[k++] = Double.parseDouble(scenario.getConfig().findParam("locationchoice", "travelSpeed_car"));
			this.parameters[k++] = Double.parseDouble(scenario.getConfig().findParam("locationchoice", "travelSpeed_pt"));
			calculateScore(scenario);
		}
		public ParametersArray(double[] parameters, Scenario scenario) {
			this.parameters = parameters;
			modifyConfig(scenario);
			calculateScore(scenario);
		}
		private void modifyConfig(Scenario scenario) {
			int k=0;
			((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setAnalysisBinSize((int) this.parameters[k++]);
			((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setAnalysisBoundary((int) this.parameters[k++]);
			String factors = "";
			for(int j=0; j<scenario.getConfig().findParam("locationchoice", "flexible_types").split(",").length; j++)
				factors += this.parameters[k++]+",";
			((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setEpsilonScaleFactors(factors.substring(0, factors.length()-1));
			((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setProbChoiceSetSize((int)this.parameters[k++]);
			((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setRadius(this.parameters[k++]);
			((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setRestraintFcnExp(this.parameters[k++]);
			((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setRestraintFcnFactor(this.parameters[k++]);
			((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setScaleFactor(this.parameters[k++]);
			((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setTravelSpeed_car(this.parameters[k++]);
			((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setTravelSpeed_pt(this.parameters[k++]);
		}
		private void calculateScore(final Scenario scenario) {
			module.prepareReplanning(null);
			TransitActsRemover transitActsRemover = new TransitActsRemover();
			Collection<PlanImpl> copiedPlans = new ArrayList<PlanImpl>();
			for(Person person:scenario.getPopulation().getPersons().values()) {
				Person copyPerson = PopulationUtils.createPerson(person.getId());
				PlanImpl copyPlan = new PlanImpl(copyPerson);
				copyPlan.copyFrom(person.getSelectedPlan());
				copyPerson.addPlan(copyPlan);
				transitActsRemover.run(copyPlan);
				module.handlePlan(copyPlan);
				copiedPlans.add(copyPlan);
			}
			module.finishReplanning();
			double sumDistances=0;
			int numSec = 0;
			Map<String, Map<String, List<Double>>> distances = new HashMap<String, Map<String,List<Double>>>();
			for(PlanImpl copyPlan:copiedPlans) {
				Activity prevActivity = null;
				String prevMode = null;
				for(PlanElement planElement:copyPlan.getPlanElements())
					if(planElement instanceof Activity) {
						String type = ((Activity)planElement).getType();
						if(scenario.getConfig().findParam("locationchoice", "flexible_types").contains(type)) {
							Map<String, List<Double>> map = distances.get(type);
							if(map==null) {
								map = new HashMap<String, List<Double>>();
								distances.put(type, map);
							}
							if(prevMode!=null) {
								List<Double> list = map.get(prevMode);
								if(list==null) {
									list = new ArrayList<Double>();
									map.put(prevMode, list);
								}
								double distance = CoordUtils.calcEuclideanDistance(prevActivity.getCoord(), ((Activity)planElement).getCoord());
								sumDistances+=distance;
								numSec++;
								list.add(distance);
								prevMode = null;
							}
						}
						else
							prevActivity = (Activity)planElement;
					}
					else
						prevMode = ((Leg)planElement).getMode();
			}
			score = Math.abs(sumDistances/numSec-avgDistance);
			score = compare(distances, distancesHits);
		}
		public double compare(Map<String,Map<String,List<Double>>> bigMapSim, Map<String,Map<String,List<Double>>> bigMapHits) {
			int j = 0;
			int k = 0;
			String[] aTypes = actTypes.split(",");
			double [] KSTests = new double [2*aTypes.length];
			double [] weight = new double [2*aTypes.length];
			for (String type:aTypes) {
				double [] ACarHits = new double [bigMapHits.get(type).get("car").size()];
				double [] APtHits = new double [bigMapHits.get(type).get("pt").size()];
				double [] ACarSim = new double [bigMapSim.get(type).get("car").size()];
				double [] APtSim = new double [bigMapSim.get(type).get("pt").size()];
				for (int i=0; i<bigMapHits.get(type).get("car").size(); i++)
					ACarHits[i] = bigMapHits.get(type).get("car").get(i);
				for (int i=0; i<bigMapHits.get(type).get("pt").size(); i++)
					APtHits[i] = bigMapHits.get(type).get("pt").get(i);
				for (int i=0; i<bigMapSim.get(type).get("car").size(); i++)
					ACarSim[i] = bigMapSim.get(type).get("car").get(i);
				for (int i=0; i<bigMapSim.get(type).get("pt").size(); i++)
					APtSim[i] = bigMapSim.get(type).get("pt").get(i);
				/*KolmogorovTwoSample SamCar = new KolmogorovTwoSample(ACarHits,ACarSim);
				KolmogorovTwoSample SamPt = new KolmogorovTwoSample(APtHits,APtSim);
				double StatCar = SamCar.getTestStatistic();
				double StatPt = SamPt.getTestStatistic();
				KSTests[j]=StatCar;
				j=j+1;
				weight[k]=bigMapSim.get(type).get("car").size();
				k=k+1;
				KSTests[j]=StatPt;
				j=j+1;
				weight[k]=bigMapSim.get(type).get("pt").size();
				k=k+1;*/
			}
			double sum1 = 0;
			double sum2 = 0;
			scores = KSTests;
			for (int i=0; i<2*aTypes.length; i++){
				sum1 = sum1 + KSTests[i]*weight[i];
				sum2 = sum2 + weight[i];
			}
			double avg = sum1/sum2;
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
	public static Map<String,Map<String,List<Double>>> createMap() {
		Connection c = null;
		Statement stmt1 = null;
		Statement stmt2 = null;
		try{
			Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection("jdbc:postgresql://localhost:5433/module_viii","igorm", "Highrise#123");
			c.setAutoCommit(false);
			System.out.println("Opened database successfully");
			String [] atypes = actTypes.split(",");
			HashMap <String,Map<String,List<Double>>> bigMap = new HashMap<String,Map<String,List<Double>>>();
			for(String type:atypes) {
				HashMap<String,List<Double>> map = new HashMap<String,List<Double>>();
				List<Double> lcar = new ArrayList<Double>();
				List<Double> lpt = new ArrayList<Double>();
				map.put("car",lcar);
				map.put("pt",lpt);
				bigMap.put(type, map);
				stmt1 = c.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY );
				stmt2 = c.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY );
				ResultSet rscar = stmt1.executeQuery("SELECT distance FROM u_igorm.hits_activities_from_to WHERE main_mode='car' AND to_type='"+type+"';"); 
				ResultSet rspt = stmt2.executeQuery( "SELECT distance FROM u_igorm.hits_activities_from_to WHERE (main_mode='bus' OR main_mode='mrt') AND to_type='"+type+"';");
				while ( rscar.next() )
					bigMap.get(type).get("car").add(rscar.getDouble("distance"));
				System.out.println(type+" car"+lcar);
				while ( rspt.next() )
					bigMap.get(type).get("pt").add(rspt.getDouble("distance"));
				System.out.println(type+" pt"+lpt);
				rscar.beforeFirst();
				rspt.beforeFirst();
				rscar.close();
				rspt.close();
				stmt1.close();
				stmt2.close();
			}
			c.close();
			System.out.println("Operation done successfully");
			return bigMap;
		}
		catch ( Exception e ) {
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
		}
		return null;
	}
	
	public static void main(String[] args) throws IOException {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		new MatsimPopulationReader(scenario).readFile(args[1]);
		new MatsimFacilitiesReader(scenario).readFile(args[2]);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[3]);
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
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setAlgorithm(Algotype.valueOf("bestResponse"));
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setEpsilonDistribution("gumbel");
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setFlexibleTypes(actTypes);
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setEpsilonScaleFactors("1, 1, 1, 1, 1, 1, 1, 1, 1");
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setDestinationSamplePercent(Double.parseDouble(args[9]));
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setRestraintFcnExp(1);
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setRestraintFcnFactor(1);
		EventsManager events = new EventsManagerImpl();
		final TravelTimeCalculator travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		events.addHandler(travelTimeCalculator);
		final WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		events.addHandler(waitTimeCalculator);
		final StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		events.addHandler(stopStopTimeCalculator);
		new MatsimEventsReader(events).readFile(args[10]);
		final TravelDisutilityFactory factory = new Builder( TransportMode.car, scenario.getConfig().planCalcScore() );
		final Provider<TransitRouter> transitRouterFactory = new TransitRouterWSImplFactory(scenario, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes());
		DestinationChoiceBestResponseContext dcContext = new DestinationChoiceBestResponseContext(scenario);
		dcContext.init();
		ReadOrComputeMaxDCScore rcms = new ReadOrComputeMaxDCScore(dcContext);
        rcms.readOrCreateMaxDCScore(new Controler(scenario).getConfig(), dcContext.kValsAreRead());
        rcms.getPersonsMaxEpsUnscaled();
		Map<String, TravelTime> travelTimes = new HashMap<>();
		travelTimes.put(TransportMode.car, travelTimeCalculator.getLinkTravelTimes());
		Map<String, TravelDisutilityFactory> factories = new HashMap<>();
		factories.put(TransportMode.car, factory);
		module = new BestReplyDestinationChoice(TripRouterFactoryBuilderWithDefaults.createTripRouterProvider(scenario, new DijkstraFactory(), transitRouterFactory), dcContext, rcms.getPersonsMaxEpsUnscaled(), new CharyparNagelOpenTimesScoringFunctionFactory(scenario.getConfig().planCalcScore(), scenario), travelTimes, factories);
		int numIterations = new Integer(args[4]);
		avgDistance = new Double(args[5]);
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
				printer2.println(Arrays.toString(parametersMatrix.scores));
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

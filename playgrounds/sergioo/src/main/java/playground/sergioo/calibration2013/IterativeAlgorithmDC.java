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
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import playground.sergioo.singapore2012.scoringFunction.CharyparNagelOpenTimesScoringFunctionFactory;
import playground.sergioo.singapore2012.transitLocationChoice.TransitActsRemover;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterWSImplFactory;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeCalculator;
import playground.sergioo.typesPopulation2013.population.MatsimPopulationReader;

import javax.inject.Provider;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

//import com.imsl.stat.KolmogorovTwoSample;

public class IterativeAlgorithmDC {

	private static ReplanningContext context;
	private static String actTypes = "biz,errand,medi,eat,rec,shop,social,sport,fun";
	private static Map<String, Map<String, List<Double>>> distancesHits = createMap();
	
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
	
	/**
	 * @param args
	 * 0 - Config file
	 * 1 - Population file
	 * 2 - Facilities file
	 * 3 - Network file
	 * 4 - Transit Schedule file
	 * 5 - Min value for each Epsilon scale factor
	 * 6 - Max value for each Epsilon scale factor
	 * 7 - Number of parts to divide
	 * 8 - Number of iteration per activity tyoe
	 * 9 - Sample percentage
	 * 10 - Events file
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig(args[0]));
		new MatsimPopulationReader(scenario).readFile(args[1]);
		Map<String, Set<Person>> typePopulations = new HashMap<String, Set<Person>>();
		for(String type:actTypes.split(","))
			typePopulations.put(type, new HashSet<Person>());
		for(Person person:scenario.getPopulation().getPersons().values())
			for(PlanElement planElement:person.getSelectedPlan().getPlanElements())
				if(planElement instanceof Activity) {
					Set<Person> persons = typePopulations.get(((Activity)planElement).getType());
					if(persons!=null)
						persons.add(person);
				}
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
		scenario.getConfig().transit().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile(args[4]);
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setAlgorithm(Algotype.valueOf("bestResponse"));
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setEpsilonDistribution("gumbel");
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setFlexibleTypes(actTypes);
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setEpsilonScaleFactors("1, 1, 1, 1, 1, 1, 1, 1, 1");
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setDestinationSamplePercent(Double.parseDouble(args[9]));
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setAnalysisBoundary(0.25);
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setScaleFactor(0.25);
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setTravelSpeed_car(9);
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setTravelSpeed_pt(5);
		EventsManager events = new EventsManagerImpl();
		final TravelTimeCalculator travelTimeCalculator = TravelTimeCalculator.create(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		events.addHandler(travelTimeCalculator);
		final WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		events.addHandler(waitTimeCalculator);
		final StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		events.addHandler(stopStopTimeCalculator);
		new MatsimEventsReader(events).readFile(args[10]);
		final TravelDisutilityFactory factory = new Builder( TransportMode.car, scenario.getConfig().planCalcScore() );
		Map<String, TravelDisutilityFactory> factories = new HashMap<>();
		factories.put(TransportMode.car, factory);

		final Provider<TransitRouter> transitRouterFactory = new TransitRouterWSImplFactory(scenario, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes());
		context = new ReplanningContext() {
			@Override
			public int getIteration() {
				return 1;
			}
		};
		double minValue = new Double(args[5]), maxValue = new Double(args[6]);
		int numParts = new Integer(args[7]), numIterations = new Integer(args[8]);
		TransitActsRemover transitActsRemover = new TransitActsRemover();
		int a=0;
		for(String type:actTypes.split(",")) {
			double jump = (maxValue-minValue)/numParts;
			double minValueI = minValue, maxValueI = maxValue, bestValue = 1;
			for(int it=0; it<numIterations; it++) {
				int best = 0, v=0;
				double bestScore = Double.POSITIVE_INFINITY;
				for(double value = minValueI+jump/2; value<maxValueI; value+=jump) {
					((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setEpsilonScaleFactors(getEpsilonFactors(a, value, actTypes.split(",").length));
					DestinationChoiceBestResponseContext dcContext = new DestinationChoiceBestResponseContext(scenario);
					dcContext.init();
					ReadOrComputeMaxDCScore rcms = new ReadOrComputeMaxDCScore(dcContext);
                    rcms.readOrCreateMaxDCScore(new Controler(scenario).getConfig(), dcContext.kValsAreRead());
                    rcms.getPersonsMaxEpsUnscaled();
					Map<String, TravelTime> travelTimes = new HashMap<>();
					travelTimes.put(TransportMode.car, travelTimeCalculator.getLinkTravelTimes());
					BestReplyDestinationChoice module = new BestReplyDestinationChoice(TripRouterFactoryBuilderWithDefaults.createTripRouterProvider(scenario, new DijkstraFactory(), transitRouterFactory), dcContext, rcms.getPersonsMaxEpsUnscaled(), new CharyparNagelOpenTimesScoringFunctionFactory(scenario.getConfig().planCalcScore(), scenario), travelTimes, factories);
					module.prepareReplanning(context);
					Collection<PlanImpl> copiedPlans = new ArrayList<PlanImpl>();
					for(Person person:typePopulations.get(type)) {
						Person copyPerson = PopulationUtils.createPerson(person.getId());
						PlanImpl copyPlan = new PlanImpl(copyPerson);
						copyPlan.copyFrom(person.getSelectedPlan());
						copyPerson.addPlan(copyPlan);
						transitActsRemover.run(copyPlan);
						module.handlePlan(copyPlan);
						copiedPlans.add(copyPlan);
					}
					module.finishReplanning();
					Map<String, List<Double>> distances = new HashMap<String, List<Double>>();
					for(PlanImpl copyPlan:copiedPlans) {
						Activity prevActivity = null;
						String prevMode = null;
						for(PlanElement planElement:copyPlan.getPlanElements())
							if(planElement instanceof Activity) {
								String typeAct = ((Activity)planElement).getType();
								if(type.equals(typeAct)) {
									if(prevMode!=null) {
										List<Double> list = distances.get(prevMode);
										if(list==null) {
											list = new ArrayList<Double>();
											distances.put(prevMode, list);
										}
										double distance = CoordUtils.calcEuclideanDistance(prevActivity.getCoord(), ((Activity)planElement).getCoord());
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
					double score = compare(distances, distancesHits.get(type));
					if(score<bestScore) {
						best = v;
						bestScore = score;
						bestValue = value;
					}
					v++;
				}
				maxValueI=(best+1)*jump;
				minValueI=best*jump;
				jump = (maxValueI-minValueI)/numParts;
			}
			System.out.println("Best value for "+actTypes.split(",")[a]+": "+bestValue);
			a++;
		}
	}
	public static double compare(Map<String,List<Double>> bigMapSim, Map<String,List<Double>> bigMapHits) {
		int j = 0;
		int k = 0;
		String[] aTypes = actTypes.split(",");
		double [] KSTests = new double [2*aTypes.length];
		double [] weight = new double [2*aTypes.length];
		double [] ACarHits = new double [bigMapHits.get("car").size()];
		double [] APtHits = new double [bigMapHits.get("pt").size()];
		double [] ACarSim = new double [bigMapSim.get("car").size()];
		double [] APtSim = new double [bigMapSim.get("pt").size()];
		for (int i=0; i<bigMapHits.get("car").size(); i++)
			ACarHits[i] = bigMapHits.get("car").get(i);
		for (int i=0; i<bigMapHits.get("pt").size(); i++)
			APtHits[i] = bigMapHits.get("pt").get(i);
		for (int i=0; i<bigMapSim.get("car").size(); i++)
			ACarSim[i] = bigMapSim.get("car").get(i);
		for (int i=0; i<bigMapSim.get("pt").size(); i++)
			APtSim[i] = bigMapSim.get("pt").get(i);
		/*KolmogorovTwoSample SamCar = new KolmogorovTwoSample(ACarHits,ACarSim);
		KolmogorovTwoSample SamPt = new KolmogorovTwoSample(APtHits,APtSim);
		double StatCar = SamCar.getTestStatistic();
		double StatPt = SamPt.getTestStatistic();
		KSTests[j]=StatCar;*/
		j=j+1;
		weight[k]=bigMapSim.get("car").size();
		k=k+1;
		//KSTests[j]=StatPt;
		j=j+1;
		weight[k]=bigMapSim.get("pt").size();
		k=k+1;
		double sum1 = 0;
		double sum2 = 0;
		for (int i=0; i<2*aTypes.length; i++){
			sum1 = sum1 + KSTests[i]*weight[i];
			sum2 = sum2 + weight[i];
		}
		double avg = sum1/sum2;
		return avg;
	}
	private static String getEpsilonFactors(int pos, double value, int numActs) {
		String vals = "";
		for(int i=0; i<numActs; i++)
			if(pos==i)
				vals+=value+",";
			else
				vals+="1,";
		return vals.substring(0, vals.length()-1);
	}
	
}

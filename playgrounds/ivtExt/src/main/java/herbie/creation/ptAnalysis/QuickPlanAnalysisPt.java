
package herbie.creation.ptAnalysis;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.Vehicles;
import utils.Bins;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class QuickPlanAnalysisPt {
	
	private String NETWORKFILE = "P:/Projekte/matsim/data/switzerland/networks/ivtch-multimodal/zh/network.multimodal-wu.xml.gz";
	private String outpath = "P:/Projekte/herbie/output/ptScenarioCreation/ptPlanAnalysis/";
	
	private String PLANSFILE;
	private String transitScheduleFile;
	private String transitVehicleFile;
	
//	private String PLANSFILE0 = "P:/Projekte/herbie/output/20111031/calibrun1/ITERS/it.150/herbie.150.plans.xml.gz";
//	private String transitScheduleFile0 = "P:/Projekte/matsim/data/switzerland/pt/zh/transitSchedule.networkOevModellZH.xml";
//	private String transitVehicleFile0 = "P:/Projekte/matsim/data/switzerland/pt/zh/vehicles.oevModellZH.xml";
//	
//	private String PLANSFILE1 = "P:/Projekte/herbie/output/20111031/calibrun2/ITERS/it.150/herbie.150.plans.xml.gz";
//	private String transitScheduleFile1 = "P:/Projekte/herbie/output/ptScenarioCreation/newTransitSchedule.xml.gz";
//	private String transitVehicleFile1 = "P:/Projekte/herbie/output/ptScenarioCreation/newTransitVehicles.xml.gz";
	
	private String PLANSFILE0 = "P:/Projekte/herbie/output/20111213/calibrun2/ITERS/it.150/herbie.150.plans.xml.gz";
	private String transitScheduleFile0 = "P:/Projekte/matsim/data/switzerland/pt/zh/transitSchedule.networkOevModellZH.xml";
	private String transitVehicleFile0 = "P:/Projekte/matsim/data/switzerland/pt/zh/vehicles.oevModellZH.xml";
	
	private String PLANSFILE1 = "P:/Projekte/herbie/output/20111213/calibrun3/ITERS/it.150/herbie.150.plans.xml.gz";
	private String transitScheduleFile1 = "P:/Projekte/herbie/output/ptScenarioCreation/newTransitSchedule.xml.gz";
	private String transitVehicleFile1 = "P:/Projekte/herbie/output/ptScenarioCreation/newTransitVehicles.xml.gz";
	
	private final static Logger log = Logger.getLogger(PtScenarioAdaption.class);
	private static double[] relevantBinInterval_1h = new double[]{0.0, 70.0}; // relevant interval graphical output
	private static double[] relevantBinInterval1_2h = new double[]{0.0, 2d  *60.0}; // relevant interval graphical output
	private static double[] relevantBinInterval2_24h = new double[]{0.0, 3d  *60.0}; // relevant interval graphical output
	private final static String SEPARATOR = "===";
	private MutableScenario scenario;
	private Population pop;
	private TransitScheduleFactory transitFactory = null;
	private ArrayList<Double> headways = new ArrayList<Double>();
	
	private Bins hdwy_distrib_1h;
	private Bins hdwy_distrib_2h;
	private Bins hdwy_distrib_24h;
	private Bins transportMode;
	
	private double totalTravelTime = 0d;
	private double latestActivityEndTime;
	
	private String scenarioName;
	private TreeMap<Id, Double> travelTimeSz0 = new TreeMap<Id, Double>();
	private TreeMap<Id, Double> scoreSz0 = new TreeMap<Id, Double>();
	private TreeMap<Id, Double> totalActDurations = new TreeMap<Id, Double>();
	private TreeMap<Id, Double> totalTransitWalkTimes = new TreeMap<Id, Double>();
	private TreeMap<Id, Double> totalAccessTimePtWalks = new TreeMap<Id, Double>();
	private TreeMap<Id, Double> totalFirstAccessWalkTimes = new TreeMap<Id, Double>();
	private TreeMap<Id, Double> totalEgressWalkTimes = new TreeMap<Id, Double>();
	private TreeMap<Id, String> firstPtRouteSz0 = new TreeMap<Id, String>();
	private TreeMap<Id, String> firstPtRouteSz1 = new TreeMap<Id, String>();
	private int numberOfAgentsDecreasedTT;
	private double totalTTsaved;
	private double totalMoreScore;
	private int numberOfAgentsGainedScore;
	private int numberOfAgentsLostScore;
	private double totalGain;
	private double totalLost;
	private int numberOfAgentsIncreasedTT;
	private double totalTTDecreased;
	private double totalTTIncreased;
	private double totalActivityDuration;
	private int numberOfAgentsGainedActTime;
	private int numberOfAgentsLostActTime;
	private double totalGainOfActTime;
	private double totalLostOfActTime;
	private int totalNrOfAgents;
	private double totalTransitWalkTime;
	private double totalIncreasedWalkTime;
	private int numberOfAgentsIncreasedWalkTime;
	private int numberOfAgentsReducedWalkTime;
	private double totalReducedWalkTime;
	private boolean activityPerformed;
	private double totalFirstAccessPtWalk;
	private double totalEgressPtWalk;
	private int numberOfAgentsReducedFirstAccessWalkTime;
	private int numberOfAgentsIncreasedFirstAccessWalkTime;
	private double totalReducedFirstAccessWalkTime;
	private double totalIncreasedFirstAccessWalkTime;
	private int numberOfAgentsReducedEgressWalkTime;
	private int numberOfAgentsIncreasedEgressWalkTime;
	private double totalReducedEgressWalkTime;
	private double totalIncreasedEgressWalkTime;
	private int numberOfAgentsDecreasedAccessWalkTimeWithSameRoute;
	private int numberOfAgentsIncreasedAccessWalkTimeWithSameRoute;
	private double totalDecreasedWalkTimeWithSameRoute;
	private double totalIncreasedWalkTimeWithSameRoute;
	private double totalAccessTimePtWalk;
	private int numberOfAgentsDecreasedTotalAccessTime;
	private double totalIncreasedAccessTime;
	private int numberOfAgentsIncreasedTotalAccessTime;
	private double totalDecreasedAccessTime;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		QuickPlanAnalysisPt quickAnalysis =  new QuickPlanAnalysisPt();
		quickAnalysis.run(args);
	}
	
	private void run(String[] args) {
		readParams(args);
		
		setScenario(0);
		inizialize();
		analysePlanFile();
		printResults();
		
		setScenario(1);
		inizialize();
		analysePlanFile();
		printResults();
	}

	private void setScenario(int scenarioNr) {
		
		if(scenarioNr == 0){
			scenarioName = "Sz0";
			
			PLANSFILE = PLANSFILE0;
			transitScheduleFile = transitScheduleFile0;
			transitVehicleFile = transitVehicleFile0;
		}
		else if(scenarioNr ==1){
			scenarioName = "Sz1";
			
			PLANSFILE = PLANSFILE1;
			transitScheduleFile = transitScheduleFile1;
			transitVehicleFile = transitVehicleFile1;
		}
	}

	private void readParams(String[] args) {
	}
	
	public void inizialize(){
		
		log.info("inizialize ... ");
		
		
    	this.scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);
		
		pop = scenario.getPopulation();
		
		new MatsimPopulationReader(scenario).readFile(PLANSFILE);
		
		
//		scenario.getConfig().scenario().setUseVehicles(true);
		scenario.getConfig().transit().setUseTransit(true);
		
//		Network network = scenario.getNetwork();
//		network.setCapacityPeriod(3600.0);
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		this.transitFactory = schedule.getFactory();
		new TransitScheduleReaderV1(scenario).readFile(transitScheduleFile);
		
		Vehicles vehicles = scenario.getTransitVehicles();
		new VehicleReaderV1(vehicles).readFile(transitVehicleFile);
		
		
		this.hdwy_distrib_1h = new Bins(1, relevantBinInterval_1h[1], "Headway Distrib TravelTime lower than 1h");
		this.hdwy_distrib_2h = new Bins(1, relevantBinInterval1_2h[1], "Headway Distrib 1h TravelTime 2h");
		this.hdwy_distrib_24h = new Bins(1, relevantBinInterval2_24h[1], "Headway Distrib 2h TravelTime 24h");
		this.transportMode = new Bins(1, 3, "Transport Mode");
		
		numberOfAgentsDecreasedTT = 0;
		numberOfAgentsIncreasedTT = 0;
		totalTTIncreased = 0d;
		totalTTDecreased = 0d;
		
		
		numberOfAgentsGainedScore = 0;
		numberOfAgentsLostScore = 0;
		
		totalTTsaved = 0d;
		totalMoreScore = 0d;
		totalGain = 0d;
		totalLost = 0d;
		
		numberOfAgentsGainedActTime = 0;
		numberOfAgentsLostActTime = 0;
		totalGainOfActTime = 0d;
		totalLostOfActTime = 0d;
		
		totalIncreasedWalkTime = 0d;
		numberOfAgentsIncreasedWalkTime = 0;
		totalReducedWalkTime = 0d;
		numberOfAgentsReducedWalkTime = 0;
		
		numberOfAgentsReducedFirstAccessWalkTime = 0;
		totalReducedFirstAccessWalkTime = 0d;
		numberOfAgentsIncreasedFirstAccessWalkTime = 0;
		totalIncreasedFirstAccessWalkTime = 0d;
		
		numberOfAgentsReducedEgressWalkTime = 0;
		numberOfAgentsIncreasedEgressWalkTime = 0;
		totalReducedEgressWalkTime = 0d;
		totalIncreasedEgressWalkTime = 0d;
		
		numberOfAgentsDecreasedAccessWalkTimeWithSameRoute = 0;
		numberOfAgentsIncreasedAccessWalkTimeWithSameRoute = 0;
		totalDecreasedWalkTimeWithSameRoute = 0d;
		totalIncreasedWalkTimeWithSameRoute = 0d;
		
		numberOfAgentsDecreasedTotalAccessTime = 0;
		totalIncreasedAccessTime = 0d;
		numberOfAgentsIncreasedTotalAccessTime = 0;
		totalDecreasedAccessTime = 0d;
		
		totalNrOfAgents = 0;
		
		log.info("inizialize ... done");
	}
	
	private void analysePlanFile() {
		
		log.info("analysePlanFile ... ");
		
		for(Person p : pop.getPersons().values()){
			
			totalNrOfAgents++;
			Id<Person> persId = p.getId();
			
			Plan plan = p.getSelectedPlan();
			
			QuickPlanAnalysisPt recordPt = new QuickPlanAnalysisPt();
			
			recordPt.startRecord();
			
			headways = new ArrayList<Double>();
			
			totalActivityDuration = 0d;
			totalTransitWalkTime = 0d;
			totalFirstAccessPtWalk = 0d;
			totalAccessTimePtWalk = 0d;
			totalEgressPtWalk = 0d;
			
			List<PlanElement> planElements = plan.getPlanElements();

			for (PlanElement pE : planElements) {
				
				if(pE instanceof Activity){
					
					ActivityImpl act = (ActivityImpl) pE;
					if(!act.getType().toString().equals("pt interaction")){
						
						Double actDuration = act.getEndTime() - act.getStartTime();
						
						if(actDuration != Time.UNDEFINED_TIME){
							totalActivityDuration += actDuration;
						}
						
						if(!averageHeadway().equals(Double.NaN)){
							
//							addHeadwayToBins(recordPt, p.getId(), plan.getScore());
						}
						
//						recordPt.startRecord();
//						
//						headways = new ArrayList<Double>();
						
						activityPerformed = true;
					}
					else {
						latestActivityEndTime = act.getEndTime();
						
					}
				}
				
				if (pE instanceof Leg) {
					LegImpl leg = (LegImpl) pE;
					if(leg.getMode().equals("pt")){
						
						String routeDescription = (leg.getRoute()).getRouteDescription();
						
						
						String[] description =  routeDescription.split(SEPARATOR);
						
						if(description.length > 2 && isElementOfLine(description[2])){
							Id<TransitLine> idLine = Id.create(description[2], TransitLine.class);
							Id<TransitRoute> idRoute = Id.create(description[3], TransitRoute.class);
							
							if(PLANSFILE.equals(PLANSFILE0) && !firstPtRouteSz0.containsKey(persId)) {
								firstPtRouteSz0.put(persId, idRoute.toString());
							}
							else if (PLANSFILE.equals(PLANSFILE1) && !firstPtRouteSz1.containsKey(persId)) {
								firstPtRouteSz1.put(persId, idRoute.toString());
							}
							
							recordPt.addTraveltime(getTravelTime(leg));
							
							String mode = scenario.getTransitSchedule().getTransitLines().get(idLine).getRoutes().get(idRoute).getTransportMode();
							
							addToTransportModeBin(mode);
							
							TransitRoute route = scenario.getTransitSchedule().getTransitLines().get(idLine).getRoutes().get(idRoute);
							
							double newHdwy = getHeadway(route, true);
							
							headways.add(newHdwy);
						}
						
					}
					else if(leg.getMode().equals("transit_walk")){
						
						totalTransitWalkTime += leg.getTravelTime();
						
						if(activityPerformed){
							totalAccessTimePtWalk += leg.getTravelTime();
							
							if(PLANSFILE.equals(PLANSFILE0) && !firstPtRouteSz0.containsKey(persId)) {
								totalFirstAccessPtWalk += leg.getTravelTime();
							}
							else if(PLANSFILE.equals(PLANSFILE1) && !firstPtRouteSz1.containsKey(persId)) {
								totalFirstAccessPtWalk += leg.getTravelTime();
							}
						}
						else totalEgressPtWalk += leg.getTravelTime();
						
					}
					else{
						if(!averageHeadway().equals(Double.NaN)){
							
//							addHeadwayToBins(recordPt, p.getId(), plan.getScore());
							
						}
//						headways = new ArrayList<Double>();
//						recordPt.startRecord();
					}
					
					activityPerformed = false;
				}
			}
			
			addHeadwayToBins(recordPt, p.getId(), plan.getScore());
		}
		log.info("analysePlanFile ... done");
	}
	
	private void addToTransportModeBin(String mode) {
		if(mode.equals("bus")) transportMode.addVal(1d, 1d);
		else if(mode.equals("tram")) transportMode.addVal(2d, 1d);
		else if(mode.equals("train")) transportMode.addVal(3d, 1d);
	}

	private double getTravelTime(LegImpl leg) {
		if(leg.getTravelTime() != Double.NaN){
			return leg.getTravelTime();
		}
		else if(leg.getArrivalTime() != Double.NaN || leg.getDepartureTime() != Double.NaN){
			return leg.getArrivalTime() - leg.getDepartureTime();
		}
		else {
			return Double.NaN;
		}
	}

	private void addHeadwayToBins(QuickPlanAnalysisPt recordPt, Id<Person> persId, Double score) {
		
		double actualTT = recordPt.getTravelTime();
		
		if(travelTimeSz0.containsKey(persId) && actualTT != 0d){
			double ttSz0 = travelTimeSz0.get(persId);
			
			if(actualTT < ttSz0) {
				numberOfAgentsDecreasedTT++;
				totalTTDecreased += (ttSz0 - actualTT);
			}
			else if(actualTT > ttSz0){
				numberOfAgentsIncreasedTT++;
				totalTTIncreased += (actualTT - ttSz0);
			}
			
			totalTTsaved = totalTTsaved - actualTT + ttSz0;
			
			double actualScore = score;
			double score0 = scoreSz0.get(persId);
			if(score0 > actualScore) {
				numberOfAgentsLostScore++;
				totalLost += (score0 - actualScore);
			}
			else if(score0 < actualScore) {
				numberOfAgentsGainedScore++;
				totalGain += (actualScore - score0);
			}
			
			double actDur0 = totalActDurations.get(persId);
			
			if(totalActivityDuration > actDur0){
				numberOfAgentsGainedActTime++;
				totalGainOfActTime += (totalActivityDuration - actDur0);
			}
			else if(totalActivityDuration < actDur0){
				numberOfAgentsLostActTime++;
				totalLostOfActTime += (actDur0 - totalActivityDuration);
			}
			
			double totalTransitWalkTime0 = totalTransitWalkTimes.get(persId);
			if(totalTransitWalkTime0 > totalTransitWalkTime){
				numberOfAgentsReducedWalkTime++;
				totalReducedWalkTime += (totalTransitWalkTime0 - totalTransitWalkTime);
			}
			else if(totalTransitWalkTime0 < totalTransitWalkTime){
				numberOfAgentsIncreasedWalkTime++;
				totalIncreasedWalkTime += (totalTransitWalkTime - totalTransitWalkTime0);
			}
			
			double totalAccessTimePtWalk0 = totalAccessTimePtWalks.get(persId);
			if(totalAccessTimePtWalk0 > totalAccessTimePtWalk){
				numberOfAgentsDecreasedTotalAccessTime++;
				totalDecreasedAccessTime += (totalAccessTimePtWalk0 - totalAccessTimePtWalk);
			}
			else if(totalAccessTimePtWalk0 < totalAccessTimePtWalk){
				numberOfAgentsIncreasedTotalAccessTime++;
				totalIncreasedAccessTime += (totalAccessTimePtWalk - totalAccessTimePtWalk0);
			}
			
			double totalFirstAccessPtWalk0 = totalFirstAccessWalkTimes.get(persId);
			if(totalFirstAccessPtWalk0 > totalFirstAccessPtWalk){
				numberOfAgentsReducedFirstAccessWalkTime++;
				totalReducedFirstAccessWalkTime += (totalFirstAccessPtWalk0 - totalFirstAccessPtWalk);
			}
			else if(totalFirstAccessPtWalk0 < totalFirstAccessPtWalk){
				numberOfAgentsIncreasedFirstAccessWalkTime++;
				totalIncreasedFirstAccessWalkTime += (totalFirstAccessPtWalk - totalFirstAccessPtWalk0);
			}
			
			double totalEgressPtWalk0 = totalEgressWalkTimes.get(persId);
			if(totalEgressPtWalk0 > totalEgressPtWalk){
				numberOfAgentsReducedEgressWalkTime++;
				totalReducedEgressWalkTime += (totalEgressPtWalk0 - totalEgressPtWalk);
			}
			else if(totalEgressPtWalk0 < totalEgressPtWalk){
				numberOfAgentsIncreasedEgressWalkTime++;
				totalIncreasedEgressWalkTime += (totalEgressPtWalk - totalEgressPtWalk0);
			}
			
			if(firstPtRouteSz0.containsKey(persId) && firstPtRouteSz1.containsKey(persId)){
				
				if(firstPtRouteSz0.get(persId).equals(firstPtRouteSz1.get(persId))){
					
					if(totalFirstAccessPtWalk0 > totalFirstAccessPtWalk){
						numberOfAgentsDecreasedAccessWalkTimeWithSameRoute++;
						totalDecreasedWalkTimeWithSameRoute += (totalFirstAccessPtWalk0 - totalFirstAccessPtWalk);
					}
					else if(totalFirstAccessPtWalk0 < totalFirstAccessPtWalk){
						numberOfAgentsIncreasedAccessWalkTimeWithSameRoute++;
						totalIncreasedWalkTimeWithSameRoute += (totalFirstAccessPtWalk - totalFirstAccessPtWalk0);
					}
				}
			}
			
			totalMoreScore = totalMoreScore + actualScore - score0;
		}
		else{
			travelTimeSz0.put(persId, actualTT);
			scoreSz0.put(persId, score);
			totalActDurations.put(persId, totalActivityDuration);
			totalTransitWalkTimes.put(persId, totalTransitWalkTime);
			totalAccessTimePtWalks.put(persId, totalAccessTimePtWalk);
			totalFirstAccessWalkTimes.put(persId, totalFirstAccessPtWalk);
			totalEgressWalkTimes.put(persId, totalEgressPtWalk);
		}
		
		if(actualTT != 0d){
			if(actualTT < 1d * 60d * 60d){
				hdwy_distrib_1h.addVal(averageHeadway() / 60d, 1d);
			}
			else if(actualTT < 2d * 60d * 60d){
				hdwy_distrib_2h.addVal(averageHeadway() / 60d, 1d);
			}
			else if(actualTT >= 2d * 60d * 60d){
				hdwy_distrib_24h.addVal(averageHeadway() / 60d, 1d);
			}
		}
		
	}
	
	private double getHeadway(TransitRoute route, boolean takeNxtHdwy) {
		
		if(route.getDepartures().size() <= 1) return 0d;
		
		double headway = 0d;
		
		TreeMap<Double, Departure> departuresTimes = new TreeMap<Double, Departure>();
		
		for(Departure departure : route.getDepartures().values()){
			departuresTimes.put(departure.getDepartureTime(), departure);
		}
		
		Double previousDepTime = Double.NaN;
		for(Double depTime : departuresTimes.keySet()){
			if(previousDepTime.equals(Double.NaN)) {
				previousDepTime = depTime;
			}
			else{
				headway = headway  + (depTime - previousDepTime);
					
				if(takeNxtHdwy && depTime > latestActivityEndTime) {
					return (depTime - previousDepTime);
				}
				previousDepTime = depTime;
			}
		}
		
		return (headway / (departuresTimes.size() - 1));
	}
	
	private double getTravelTime() {
		
		return this.totalTravelTime;
	}

	private void addTraveltime(double newtraveltime) {
		
		totalTravelTime = totalTravelTime + newtraveltime;
	}

	private boolean isElementOfLine(String line) {
		
		Id<TransitLine> id = Id.create(line, TransitLine.class);
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		return schedule.getTransitLines().containsKey(id);
	}

	private Double endRecord() {
		return averageHeadway();
	}

	private Double averageHeadway() {
		
		double sum = 0d;
		
		for(Double hdwy : headways) sum = sum + hdwy;
		
		double average = sum / headways.size();
			
		return average;
	}

	private void startRecord() {
		totalTravelTime = 0d;
	}
	
	private void printResults() {
		log.info("printResults ... ");
		
		System.out.println("TotalNumberOfAgents: "+ totalNrOfAgents);
		
		hdwy_distrib_1h.plotBinnedDistribution(outpath + scenarioName + "HeadwayDistribution lower 1h", "Headway", "#", "Number of departures with same headway");
		
		hdwy_distrib_2h.plotBinnedDistribution(outpath + scenarioName + "HeadwayDistribution between 1h and 2h", "Headway", "#", "Number of departures with same headway");
		
		hdwy_distrib_24h.plotBinnedDistribution(outpath + scenarioName + "HeadwayDistribution between 2h and 24h", "Headway", "#", "Number of departures with same headway");
		
		transportMode.plotBinnedDistribution(outpath + scenarioName + "TransportMode", "Bus  Tram  Train", "#", "Number of Trips");
		
		System.out.println("NumberOfAgentsDecreasedTT: " + numberOfAgentsDecreasedTT + " averageTTDecreased [h]: " + (totalTTDecreased / (numberOfAgentsDecreasedTT * 3600d)));
		System.out.println("NumberOfAgentsIncreasedTT: " + numberOfAgentsIncreasedTT + " averageTTIncreased [h]: " + (totalTTIncreased / (numberOfAgentsIncreasedTT * 3600d)));
		
		System.out.println("totalTTsaved [h]: " + (totalTTsaved / 3600d));
		System.out.println();
		
		System.out.println("NumberOfAgentsReducedWalkTime: " + numberOfAgentsReducedWalkTime + 
				" averageWalkTimeReduced [h]: " + (totalReducedWalkTime / (numberOfAgentsReducedWalkTime * 3600d)));
		System.out.println("NumberOfAgentsIncreasedWalkTime: " + numberOfAgentsIncreasedWalkTime + 
				" averageWalkTimeIncreased [h]: " + (totalIncreasedWalkTime / (numberOfAgentsIncreasedWalkTime * 3600d)));
		System.out.println();
		
		System.out.println("NumberOfAgentsDecreasedTotalAccessWalkTime: " + numberOfAgentsDecreasedTotalAccessTime + 
				" averageAccessWalkTimeReduced [h]: " + (totalDecreasedAccessTime / (numberOfAgentsDecreasedTotalAccessTime * 3600d)));
		System.out.println("NumberOfAgentsIncreasedTotalAccessWalkTime: " + numberOfAgentsIncreasedTotalAccessTime + 
				" averageFirstAccessWalkTimeIncreased [h]: " + (totalIncreasedAccessTime / (numberOfAgentsIncreasedTotalAccessTime * 3600d)));
		System.out.println();
		
		System.out.println("NumberOfAgentsReducedFirstAccessWalkTime: " + numberOfAgentsReducedFirstAccessWalkTime + 
				" averageFirstAccessWalkTimeReduced [h]: " + (totalReducedFirstAccessWalkTime / (numberOfAgentsReducedFirstAccessWalkTime * 3600d)));
		System.out.println("NumberOfAgentsIncreasedFirstAccessWalkTime: " + numberOfAgentsIncreasedFirstAccessWalkTime + 
				" averageFirstAccessWalkTimeIncreased [h]: " + (totalIncreasedFirstAccessWalkTime / (numberOfAgentsIncreasedFirstAccessWalkTime * 3600d)));
		System.out.println();
		
		System.out.println("numberOfAgentsDecreasedAccessWalkTimeWithSameRoute: " + numberOfAgentsDecreasedAccessWalkTimeWithSameRoute + 
				" averageDecreasedAccessWalkTimeWithSameRoute [h]: " + (totalDecreasedWalkTimeWithSameRoute / (numberOfAgentsDecreasedAccessWalkTimeWithSameRoute * 3600d)));
		System.out.println("numberOfAgentsIncreasedAccessWalkTimeWithSameRoute: " + numberOfAgentsIncreasedAccessWalkTimeWithSameRoute + 
				" averageIncreasedAccessWalkTimeWithSameRoute [h]: " + (totalIncreasedWalkTimeWithSameRoute / (numberOfAgentsIncreasedAccessWalkTimeWithSameRoute * 3600d)));
		System.out.println();
		
		System.out.println("NumberOfAgentsReducedEgressWalkTime: " + numberOfAgentsReducedEgressWalkTime + 
				" averageEgressWalkTimeReduced [h]: " + (totalReducedEgressWalkTime / (numberOfAgentsReducedEgressWalkTime * 3600d)));
		System.out.println("NumberOfAgentsIncreasedEgressWalkTime: " + numberOfAgentsIncreasedEgressWalkTime + 
				" averageEgressWalkTimeIncreased [h]: " + (totalIncreasedEgressWalkTime / (numberOfAgentsIncreasedEgressWalkTime * 3600d)));
		System.out.println();
		
		System.out.println("NumberOfAgentsGainedActTime: " + numberOfAgentsGainedActTime + 
				" averageActTimeGained [h]: " + (totalGainOfActTime / (numberOfAgentsGainedActTime * 3600d)));
		System.out.println("NumberOfAgentsLostActTime: " + numberOfAgentsLostActTime + 
				" averageActTimeLost [h]: " + (totalLostOfActTime / (numberOfAgentsLostActTime * 3600d)));
		System.out.println();
		
		System.out.println("NumberOfAgentsGainedScore: " + numberOfAgentsGainedScore + " averageScoreGain []: " + totalGain / numberOfAgentsGainedScore);
		System.out.println("NumberOfAgentsLostScore: " + numberOfAgentsLostScore + " averageScoreLost []: " + totalLost / numberOfAgentsLostScore);
		
		System.out.println("totalMoreScore: " + (totalMoreScore));
		
		log.info("printResults ... done");
	}
}

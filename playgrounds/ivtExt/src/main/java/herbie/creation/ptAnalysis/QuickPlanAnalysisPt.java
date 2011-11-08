
package herbie.creation.ptAnalysis;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.Vehicles;

import EDU.oswego.cs.dl.util.concurrent.Heap;

import utils.Bins;

public class QuickPlanAnalysisPt {
	
	private String PLANSFILE = "P:/Projekte/herbie/output/20111031/calibrun1/ITERS/it.0/herbie.0.plans.xml.gz";
	private String NETWORKFILE = "P:/Projekte/matsim/data/switzerland/networks/ivtch-multimodal/zh/network.multimodal-wu.xml.gz";
	private String transitScheduleFile = "P:/Projekte/matsim/data/switzerland/pt/zh/transitSchedule.networkOevModellZH.xml";
	private String transitVehicleFile = "P:/Projekte/matsim/data/switzerland/pt/zh/vehicles.oevModellZH.xml";
	private String outpath = "P:/Projekte/herbie/output/ptScenarioCreation/";
	
	private static double[] relevantBinInterval_1h = new double[]{0.0, 70.0}; // relevant interval graphical output
	private static double[] relevantBinInterval1_2h = new double[]{0.0, 2d  *60.0}; // relevant interval graphical output
	private static double[] relevantBinInterval2_24h = new double[]{0.0, 3d  *60.0}; // relevant interval graphical output
	private final static String SEPARATOR = "===";
	private String configFile;
	private ScenarioImpl scenario;
	private TransitScheduleFactory transitFactory = null;
	private ArrayList<Double> headways = new ArrayList<Double>();
	
	double[] distanceClasses = new double[]{
		Double.MAX_VALUE, 100000
		, 50000, 40000, 30000, 20000, 
		10000, 5000, 4000, 3000, 2000, 
		1000, 0.0};
	private PopulationImpl pop;
	private Bins hdwy_distrib_1h;
	private Bins hdwy_distrib_2h;
	private Bins hdwy_distrib_24h;
	private Bins transportMode;
	private double totalTravelTime = 0d;
	private double latestActivityEndTime;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		QuickPlanAnalysisPt quickAnalysis =  new QuickPlanAnalysisPt();
		quickAnalysis.run(args);
	}
	
	private void run(String[] args) {
		readParams(args);
		inizialize();
		analysePlanFile();
		printResults();
	}

	private void readParams(String[] args) {
		configFile = args[0] ;
	}
	
	public void inizialize(){
		
		final Logger log = Logger.getLogger(QuickPlanAnalysisPt.class);
		
		Config config = ConfigUtils.loadConfig(configFile);
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		new MatsimNetworkReader(scenario).readFile(NETWORKFILE);
		
		pop = (PopulationImpl) scenario.getPopulation();
		
		new MatsimPopulationReader(scenario).readFile(PLANSFILE);
		
		((PopulationFactoryImpl) pop.getFactory()).setRouteFactory("pt", new ExperimentalTransitRouteFactory());
		
		
		scenario.getConfig().scenario().setUseVehicles(true);
		scenario.getConfig().scenario().setUseTransit(true);
		
		NetworkImpl network = scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		this.transitFactory = schedule.getFactory();
		new TransitScheduleReaderV1(schedule, network, scenario).readFile(transitScheduleFile);
		
		Vehicles vehicles = scenario.getVehicles();
		new VehicleReaderV1(vehicles).readFile(transitVehicleFile);
		
		
		this.hdwy_distrib_1h = new Bins(1, relevantBinInterval_1h[1], "Headway Distrib TravelTime lower than 1h");
		this.hdwy_distrib_2h = new Bins(1, relevantBinInterval1_2h[1], "Headway Distrib 1h TravelTime 2h");
		this.hdwy_distrib_24h = new Bins(1, relevantBinInterval2_24h[1], "Headway Distrib 2h TravelTime 24h");
		this.transportMode = new Bins(1, 3, "Transport Mode");
	}
	
	private void analysePlanFile() {
//		System.out.println();
		for(Person p : pop.getPersons().values()){
			Plan plan = p.getSelectedPlan();
			
			QuickPlanAnalysisPt recordPt = new QuickPlanAnalysisPt();
			
			
			List<PlanElement> planElements = plan.getPlanElements();

			for (PlanElement pE : planElements) {
//				System.out.println();
				
				
				if(pE instanceof Activity){
					Activity act = (Activity) pE;
					if(act.equals("pt interaction")){
						recordPt.startRecord();
					}
					else {
						latestActivityEndTime = act.getEndTime();
						
						if(recordPt.endRecord() != Double.NaN){
							
							addHeadwayToBins(recordPt);
						}
					}
				}
				
				if (pE instanceof Leg) {
					LegImpl leg = (LegImpl) pE;
					if(leg.getMode().equals("pt") && leg.getRoute() instanceof GenericRoute){
//					if(leg.getMode().equals("pt")){
						
						String routeDescription = ((GenericRoute)leg.getRoute()).getRouteDescription();
						
						
						String[] description =  routeDescription.split(SEPARATOR);
						
						if(isElementOfLine(description[2])){
							IdImpl idLine = new IdImpl(description[2]);
							IdImpl idRoute = new IdImpl(description[3]);
							
							
							recordPt.addTraveltime(getTravelTime(leg));
							
							String mode = scenario.getTransitSchedule().getTransitLines().get(idLine).getRoutes().get(idRoute).getTransportMode();
							
							addToTransportModeBin(mode);
							
							TransitRoute route = scenario.getTransitSchedule().getTransitLines().get(idLine).getRoutes().get(idRoute);
							
							headways.add(getHeadway(route, true));
						}
						
//						System.out.println();
					}
					else{
						if(recordPt.endRecord() != Double.NaN){
							
							addHeadwayToBins(recordPt);
						}
					}
				}
			}
		}
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

	private void addHeadwayToBins(QuickPlanAnalysisPt recordPt) {
		if(recordPt.getTravelTime() < 1d * 60d * 60d){
			hdwy_distrib_1h.addVal(averageHeadway() / 60d, 1d);
		}
		else if(recordPt.getTravelTime() < 2d * 60d * 60d){
			hdwy_distrib_2h.addVal(averageHeadway() / 60d, 1d);
		}
		else if(recordPt.getTravelTime() >= 2d * 60d * 60d){
			hdwy_distrib_24h.addVal(averageHeadway() / 60d, 1d);
		}
	}

	private double getTravelTime() {
		return this.totalTravelTime;
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


	private void addTraveltime(double newtraveltime) {
		
		totalTravelTime = totalTravelTime + newtraveltime;
	}

	private boolean isElementOfLine(String line) {
		
		IdImpl id = new IdImpl(line);
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		return schedule.getTransitLines().containsKey(id);
	}

	private double endRecord() {
		return averageHeadway();
	}

	private double averageHeadway() {
		
		double sum = 0d;
		
		for(Double hdwy : headways) sum = sum + hdwy;
			
		return (sum / headways.size());
	}

	private void startRecord() {
	}
	
	private void printResults() {
		hdwy_distrib_1h.plotBinnedDistribution(outpath + "HeadwayDistribution lower 1h", "Headway", "#", "Number of departures with same headway");
		
		hdwy_distrib_2h.plotBinnedDistribution(outpath + "HeadwayDistribution between 1h and 2h", "Headway", "#", "Number of departures with same headway");
		
		hdwy_distrib_24h.plotBinnedDistribution(outpath + "HeadwayDistribution between 2h and 24h", "Headway", "#", "Number of departures with same headway");
		
		transportMode.plotBinnedDistribution(outpath + "TransportMode", "Bus  Tram  Train", "#", "Number of Trips");
	}
}


package herbie.creation.ptAnalysis;


import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.Vehicles;
import utils.Bins;

public class QuickPlanAnalysisPt {
	
	private String PLANSFILE = "P:/Projekte/herbie/output/20111031/calibrun1/ITERS/it.150/herbie.150.plans.xml.gz";
	private String NETWORKFILE = "P:/Projekte/matsim/data/switzerland/networks/ivtch-multimodal/zh/network.multimodal-wu.xml.gz";
	private String transitScheduleFile = "P:/Projekte/matsim/data/switzerland/pt/zh/transitSchedule.networkOevModellZH.xml";
	private String transitVehicleFile = "P:/Projekte/matsim/data/switzerland/pt/zh/vehicles.oevModellZH.xml";
	private String outpath = "P:/Projekte/herbie/output/ptScenarioCreation/ptPlanAnalysis/";
	
	private final static Logger log = Logger.getLogger(PtScenarioAdaption.class);
	private static double[] relevantBinInterval_1h = new double[]{0.0, 70.0}; // relevant interval graphical output
	private static double[] relevantBinInterval1_2h = new double[]{0.0, 2d  *60.0}; // relevant interval graphical output
	private static double[] relevantBinInterval2_24h = new double[]{0.0, 3d  *60.0}; // relevant interval graphical output
	private final static String SEPARATOR = "===";
	private ScenarioImpl scenario;
	private PopulationImpl pop;
	private TransitScheduleFactory transitFactory = null;
	private ArrayList<Double> headways = new ArrayList<Double>();
	
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
	}
	
	public void inizialize(){
		
		log.info("inizialize ... ");
		
    	this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
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
		
		log.info("inizialize ... done");
	}
	
	private void analysePlanFile() {
		
		log.info("analysePlanFile ... ");
		
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
		log.info("printResults ... ");
		
		hdwy_distrib_1h.plotBinnedDistribution(outpath + "HeadwayDistribution lower 1h", "Headway", "#", "Number of departures with same headway");
		
		hdwy_distrib_2h.plotBinnedDistribution(outpath + "HeadwayDistribution between 1h and 2h", "Headway", "#", "Number of departures with same headway");
		
		hdwy_distrib_24h.plotBinnedDistribution(outpath + "HeadwayDistribution between 2h and 24h", "Headway", "#", "Number of departures with same headway");
		
		transportMode.plotBinnedDistribution(outpath + "TransportMode", "Bus  Tram  Train", "#", "Number of Trips");
		
		log.info("printResults ... done");
	}
}

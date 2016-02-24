
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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class QuickLegAnalysisPtPlanbased {
	
	private String NETWORKFILE = "P:/Projekte/matsim/data/switzerland/networks/ivtch-multimodal/zh/network.multimodal-wu.xml.gz";
	private String outpath = "P:/Projekte/herbie/output/ptScenarioCreation/ptPlanAnalysis/";
	
	private String PLANSFILE;
	private String transitScheduleFile;
	private String transitVehicleFile;
	
	private String PLANSFILE0 = "P:/Projekte/herbie/output/20111213/calibrun4/ITERS/it.150/herbie.150.plans.xml.gz";
	private String transitScheduleFile0 = "P:/Projekte/matsim/data/switzerland/pt/zh/transitSchedule.networkOevModellZH.xml";
	private String transitVehicleFile0 = "P:/Projekte/matsim/data/switzerland/pt/zh/vehicles.oevModellZH.xml";
	
	private String PLANSFILE1 = "P:/Projekte/herbie/output/20111213/calibrun5/ITERS/it.150/herbie.150.plans.xml.gz";
	private String transitScheduleFile1 = "P:/Projekte/herbie/output/ptScenarioCreation/newTransitSchedule.xml.gz";
	private String transitVehicleFile1 = "P:/Projekte/herbie/output/ptScenarioCreation/newTransitVehicles.xml.gz";
	
	private final static Logger log = Logger.getLogger(PtScenarioAdaption.class);
	private final static String SEPARATOR = "===";
	private MutableScenario scenario;
	private Population pop;
	private TransitScheduleFactory transitFactory = null;
	private ArrayList<Double> headways = new ArrayList<Double>();
	private String scenarioName;
	


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		QuickLegAnalysisPtPlanbased quickAnalysis =  new QuickLegAnalysisPtPlanbased();
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
		
		scenario.getConfig().transit().setUseTransit(true);
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		this.transitFactory = schedule.getFactory();
		new TransitScheduleReaderV1(scenario).readFile(transitScheduleFile);
		
		Vehicles vehicles = scenario.getTransitVehicles();
		new VehicleReaderV1(vehicles).readFile(transitVehicleFile);
		
			
		log.info("inizialize ... done");
	}
	
	private void analysePlanFile() {
		
		log.info("analysePlanFile ... ");
		
		for(Person p : pop.getPersons().values()){
			
			Id persId = p.getId();
			
			Plan plan = p.getSelectedPlan();
			
			QuickLegAnalysisPtPlanbased recordPt = new QuickLegAnalysisPtPlanbased();
			
			recordPt.startRecord();
			
			headways = new ArrayList<Double>();
			
			
			List<PlanElement> planElements = plan.getPlanElements();

			for (PlanElement pE : planElements) {
				
				if(pE instanceof Activity){
					
					ActivityImpl act = (ActivityImpl) pE;
					if(!act.getType().toString().equals("pt interaction")){
						
						Double actDuration = act.getEndTime() - act.getStartTime();
						
						if(actDuration != Time.UNDEFINED_TIME){
						}
						
						if(!averageHeadway().equals(Double.NaN)){
							
						}
						
					}
					else {
						
					}
				}
				
				if (pE instanceof Leg) {
					LegImpl leg = (LegImpl) pE;
					if(leg.getMode().equals("pt")){
						
						String routeDescription = leg.getRoute().getRouteDescription();
						
						
						String[] description =  routeDescription.split(SEPARATOR);
						
						if(description.length > 2 && isElementOfLine(description[2])){
							Id<TransitLine> idLine = Id.create(description[2], TransitLine.class);
							Id<TransitRoute> idRoute = Id.create(description[3], TransitRoute.class);
														
							recordPt.addTraveltime(getTravelTime(leg));
							
							String mode = scenario.getTransitSchedule().getTransitLines().get(idLine).getRoutes().get(idRoute).getTransportMode();
							
							addToTransportModeBin(mode);
							
							TransitRoute route = scenario.getTransitSchedule().getTransitLines().get(idLine).getRoutes().get(idRoute);
							
							double newHdwy = getHeadway(route, true);
							
							headways.add(newHdwy);
						}
						
					}
					else if(leg.getMode().equals("transit_walk")){
						
						
					}
					else{
						if(!averageHeadway().equals(Double.NaN)){
							
						}
					}
				}
			}
		}
		log.info("analysePlanFile ... done");
	}
	
	private void addToTransportModeBin(String mode) {
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
				
				previousDepTime = depTime;
			}
		}
		
		return (headway / (departuresTimes.size() - 1));
	}
	

	private void addTraveltime(double newtraveltime) {
		
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
	}
	
	private void printResults() {
		log.info("printResults ... ");
		
		
		
		log.info("printResults ... done");
	}
}

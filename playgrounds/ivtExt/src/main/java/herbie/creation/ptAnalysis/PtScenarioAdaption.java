package herbie.creation.ptAnalysis;


import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

public class PtScenarioAdaption {
	
	public static final double[] headwayClasses = new double[]{
	    4, 7, 10, 15, 30, 60};  // in [minutes]!!
	
	private final static Logger log = Logger.getLogger(PtScenarioAdaption.class);
	private String networkfilePath;
	private String outpath;
	private String transitScheduleFile;
	private String vehiclesFile;
	private ScenarioImpl scenario;
	
	public static void main(String[] args) {
		if (args.length != 1) {
			log.info("Specify config path."); 
			return;
		}
		
		PtScenarioAdaption ptScenarioAdaption = new PtScenarioAdaption();
		ptScenarioAdaption.init(args[0]);
		ptScenarioAdaption.doubleHeadway();
		ptScenarioAdaption.writeScenario();
	}

	private void init(String file) {
		log.info("Initialization ...");
		
		Config config = new Config();
    	MatsimConfigReader matsimConfigReader = new MatsimConfigReader(config);
    	matsimConfigReader.readFile(file);
    	
		this.networkfilePath = config.findParam("ptScenarioAdaption", "networkfilePath");
		this.outpath = config.findParam("ptScenarioAdaption", "output");
		this.transitScheduleFile = config.findParam("ptScenarioAdaption", "transitScheduleFile");
		this.vehiclesFile = config.findParam("ptScenarioAdaption", "vehiclesFile");
		
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(this.scenario).readFile(this.networkfilePath);
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		
		TransitSchedule schedule = ((ScenarioImpl) scenario).getTransitSchedule();
		new TransitScheduleReaderV1(schedule, network, scenario).parse(transitScheduleFile);
		
		
		
		log.info("Initialization ... done");
	}
	
	/**
	 * increase headway according to the values in headwayClasses!
	 */
	private void doubleHeadway() {
		log.info("Double headway ...");
		
		TransitSchedule schedule = ((ScenarioImpl) this.scenario).getTransitSchedule();
		
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				
				Map<Id, Departure> departures = route.getDepartures();
				
				if(departures == null || departures.size() < 2) continue;
				
				TreeMap<Id, Departure> sortedDepartures = new TreeMap<Id, Departure>(departures);
				
				// test:
				for(Id id : sortedDepartures.keySet()) System.out.println("Key "+ id.toString());
				System.out.println("End test.");
				
				
				for(Id depId : sortedDepartures.keySet()){
					
					Departure prevDepId = (Departure) sortedDepartures.lowerEntry(depId);
					if(prevDepId == null) continue;
					
					double depTime1 = sortedDepartures.get(depId).getDepartureTime();
					double depTime0 = prevDepId.getDepartureTime();
					System.out.println("Dep. time: "+depTime1);
					System.out.println();
					
					double headway = depTime1 - depTime0;
					
					if(headway > headwayClasses[0] && headway <= headwayClasses[headwayClasses.length - 1]){
//  missing code
						double addDepartureTime = 0.0;
						addDeparture(addDepartureTime);
						
						if(depId.equals(sortedDepartures.lastKey())){
//  missing code
							double lastDepartureTime = 0.0;
							addDeparture(lastDepartureTime);
						}
					}
				}
			}
		}
		
		
		log.info("Double headway ... done");
	}
	
	private void addDeparture(double addDepartureTime) {

//  missing code
		
	}

	private void writeScenario() {
		log.info("Writing new network file ...");
		
		new NetworkWriter(this.scenario.getNetwork()).write(this.outpath + "network.xml.gz");
		new TransitScheduleWriter(this.scenario.getTransitSchedule()).writeFile(this.outpath + "transitSchedule.xml.gz");
		
		log.info("Writing new network file ... done");
	}
}

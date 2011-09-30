package herbie.creation.ptAnalysis;


import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

public class PtScenarioAdaption {
	
	private final static Logger log = Logger.getLogger(PtScenarioAdaption.class);
	private String networkfilePath;
	private ScenarioImpl scenario;
	private String outpath;
	
	public static void main(String[] args) 
	{
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
		
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(this.scenario).readFile(this.networkfilePath);
		
		log.info("Initialization ... done");
	}
	
	/**
	 * increase headway according to the following steps:
	 * 4, 7, 10, 15, 30, 60
	 */
	private void doubleHeadway() {
		log.info("Double headway ...");
		
		
		
		log.info("Double headway ... done");
	}
	
	private void writeScenario() {
		log.info("Writing new network file ...");
		
		new NetworkWriter(this.scenario.getNetwork()).write(this.outpath + "network.xml.gz");
		new TransitScheduleWriter(this.scenario.getTransitSchedule()).writeFile(this.outpath + "transitSchedule.xml.gz");
		
		log.info("Writing new network file ... done");
	}
}

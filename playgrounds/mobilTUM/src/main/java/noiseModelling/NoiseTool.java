package noiseModelling;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;


public class NoiseTool {
	private static String runDirectory = "/mobilTUMrunsKuhmo/output/output_baseCase_ctd/";
	private static String eventsFile = "../../mobilTUMrunsKuhmo/output/output_baseCase_ctd/ITERS/it.1500/1500.events.xml.gz";
	private static String netfile = "../../mobilTUMrunsKuhmo/output/output_baseCase_ctd/output_network.xml.gz";
	private final Scenario scenario;
	
	String outputfile = "../mobilTUM/OutputTests/noiseEvents.xml";

	
	public NoiseTool() {
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		NoiseTool noise = new NoiseTool();
		noise.run(args);
	}

	
	private void run(String[] args) {

		loadScenario();
		Network network = scenario.getNetwork();
	
		/* start events processing*/
		EventsManager eventsManager = EventsUtils.createEventsManager();
		NoiseHandler handler = new NoiseHandler(network);
		eventsManager.addHandler(handler);

		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);

		/* get information from class NoiseHandler  and save it in the following maps: linkId, time, traffic info*/
		//this method returns linkId2hourd2vehicles in NoiseHandler:
		Map<Id, Map<Double, double[]>> linkInfos = handler.getlinkId2timePeriod2TrafficInfo();
		//this method returns linkId2hour2vehicles in NoiseHandler:
		Map <Id,double [][]> linkInfosProStunde = handler.getlinkId2hour2vehicles(); 
	
		/* new instance of the class Calculation*/
		NoiseEmissionCalculation calculation = new NoiseEmissionCalculation();
		Map <Id,Map<Double,Double>> calLmeCarHdvHour = NoiseEmissionCalculation.calLmeCarHdvHour(linkInfos);
		Map <Id,Double> linkId2Lden = calculation.cal_lden(calLmeCarHdvHour);
		
		/* get information from class Calculation using the respective maps (linkInfos and res1) as input*/
		//Map <Id,Map<String,Double>> linkId2timePeriod2lme = calculation.calculate_lme(linkInfos);
		//Map <Id,Double> linkId2Lden = calculation.cal_lden(linkId2timePeriod2lme);
		
		/*new instance of the class NoiseWriter */
		//NoiseWriter writer = new NoiseWriter (linkId2timePeriod2lme, linkId2Lden);
		NoiseWriter writer = new NoiseWriter (linkInfos,linkId2Lden);
		
		//writer.writeEvents();
		
		
		try { // write the TrafficInfos per hour for every link 
			writer.writeVehiclesFreespeedProStunde(linkInfosProStunde);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try { // write the TrafficInfos per hour for every link 
			writer.writeVehiclesFreespeedProStundeDouble(linkInfos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netfile);
		// config.plans().setInputFile(plansfile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.loadScenario();

	}

}
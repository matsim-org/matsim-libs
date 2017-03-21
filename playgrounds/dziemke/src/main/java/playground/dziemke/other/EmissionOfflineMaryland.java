package playground.dziemke.other;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * 
 * Use the config file as created by the 
 * {@link org.matsim.contrib.emissions.example.CreateEmissionConfig CreateEmissionConfig} to calculate 
 * emissions based on the link leave events of an events file. Results are written into an emission event file. 
 *
 * @author benjamin, julia
 */
public class EmissionOfflineMaryland {
	
	private final static String runDirectory = "../../../runs-svn/silo/maryland/run_09/matsim/year_2001/";
	private final static String filePrefix = "year_2001."; 
	private static final String configFile = runDirectory + filePrefix + "output_config_updated.xml";
	private final static Integer lastIteration = getLastIteration();
	
	private static final String eventsPath = runDirectory + "ITERS/it." + lastIteration + "/" + filePrefix + lastIteration;
	private static final String eventsFile = eventsPath + ".events.xml.gz";
	private static final String emissionEventOutputFile = eventsPath + ".emission.events.offline.xml.gz";
	
	private static final String emissionsContribTestData = "../../contribs/emissions/test/input/org/matsim/contrib/emissions/";
	private static final String roadTypeMappingFile = emissionsContribTestData + "sample_roadTypeMapping.txt";
	private static final String emissionVehicleFile = emissionsContribTestData + "sample_emissionVehicles.xml";
	
	private static final String averageFleetWarmEmissionFactorsFile = emissionsContribTestData + "sample_EFA_HOT_vehcat_2005average.txt";
	private static final String averageFleetColdEmissionFactorsFile = emissionsContribTestData + "sample_EFA_ColdStart_vehcat_2005average.txt";
	
	private static final boolean isUsingDetailedEmissionCalculation = true;
	private static final String detailedWarmEmissionFactorsFile = emissionsContribTestData + "sample_EFA_HOT_SubSegm_2005detailed.txt";
	private static final String detailedColdEmissionFactorsFile = emissionsContribTestData + "sample_EFA_ColdStart_SubSegm_2005detailed.txt";
	
	
	// =======================================================================================================		
	
	public static void main (String[] args) throws Exception{
		
		Config config = ConfigUtils.loadConfig(configFile, new EmissionsConfigGroup());
		config.network().setInputFile("../../../shared-svn/projects/maryland/siloMatsim/network/04/network.xml");
		
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg = (EmissionsConfigGroup) config.getModule(ecg.getName());
        ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
		config.vehicles().setVehiclesFile(emissionVehicleFile);
        
        ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
        ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
        
        ecg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
        ecg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
        ecg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);
		
		System.out.println(config.network().getName());
		System.out.println(config.network().getInputFile());
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionModule emissionModule = new EmissionModule(scenario, eventsManager);

		eventsManager.addHandler(emissionModule.getWarmEmissionHandler());
		eventsManager.addHandler(emissionModule.getColdEmissionHandler());
		
		EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
		
		emissionEventWriter.closeFile();

		emissionModule.writeEmissionInformation();
	}

	private static int getLastIteration() {		
		Config config = new Config();
		config.addCoreModules();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(configFile);
        return config.controler().getLastIteration();
	}
}
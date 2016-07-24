package playground.kturner.freightKt.analyse;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class FreightAnalyseKT {

	/**
	 * @param args
	 */
	
	//TODO: VEhicleTypes in Simulation als ebenfalls in OutputDir schreiben lassen, damit Zuordnugn zru Simulation erkennbar ist.
//	private static final String RUN_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/JSprit/Berlin/aldi/base_rushhourTrue/Run_1/" ;
//	private static final String RUN_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/JSprit/Berlin/aldi/CordonTollOnHeavy/Run_1/" ;
	private static final String RUN_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/JSprit/Berlin/aldi/ElectroWithoutUCC/Run_1/" ;
//	private static final String RUN_DIR = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/output/JSprit/Berlin/aldi/UCC/Run_1/" ;

	//	private static final String VEHTYPEFILE = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Grid_Szenario/grid-vehTypes_kt.xml";
	private static final String VEHTYPEFILE = "F:/OneDrive/Dokumente/Masterarbeit/MATSIM/input/Berlin_Szenario/vehicleTypes.xml"; 
	private static final String OUTPUT_DIR = RUN_DIR + "Statistik/" ;
		
	private static final Logger log = Logger.getLogger(FreightAnalyseKT.class);
	
	public static void main(String[] args) {
		
		FreightAnalyseKT analysis = new FreightAnalyseKT();
		analysis.run();
	}
	
		private void run() {
			
			String configFile = RUN_DIR + "output_config.xml"; //Datei wurde um Parameter (Scenario) "useVehicles" erleichtert.
//			String configFile = RUN_DIR + "output_config.xml.gz";
			String populationFile = null;
			String networkFile = RUN_DIR+ "output_network.xml.gz";
			String carrierFile = RUN_DIR+ "output_carriers.xml.gz";
			
			Config config = ConfigUtils.loadConfig(configFile);		
			config.plans().setInputFile(populationFile);
			config.network().setInputFile(networkFile);
			
			MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
			EventsManager events = EventsUtils.createEventsManager();
			
			CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
			new CarrierVehicleTypeReader(vehicleTypes).readFile(VEHTYPEFILE) ;
			
			Carriers carriers = new Carriers() ;
			new CarrierPlanXmlReaderV2(carriers).readFile(carrierFile) ;

			TripEventHandler tripHandler = new TripEventHandler(scenario, vehicleTypes);
			events.addHandler(tripHandler);
					
			int iteration = config.controler().getLastIteration();
			String eventsFile = RUN_DIR + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
			
			log.info("Reading the event file...");
			MatsimEventsReader reader = new MatsimEventsReader(events);
			reader.readFile(eventsFile);
			log.info("Reading the event file... Done.");
			
			TripWriter tripWriter = new TripWriter(tripHandler, OUTPUT_DIR);
			for (Carrier carrier : carriers.getCarriers().values()){
				tripWriter.writeDetailedResultsSingleCarrier(carrier.getId().toString());
				tripWriter.writeVehicleResultsSingleCarrier(carrier.getId().toString());
			}
			
			tripWriter.writeResultsPerVehicleTypes();
			
			System.out.println("### ENDE ####");
			
	}

}

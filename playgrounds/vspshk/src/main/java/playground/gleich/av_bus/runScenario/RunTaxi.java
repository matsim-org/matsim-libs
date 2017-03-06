package playground.gleich.av_bus.runScenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.vehicles.VehicleReaderV1;

import playground.gleich.av_bus.FilePaths;

public class RunTaxi {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(FilePaths.PATH_CONFIG_BERLIN__10PCT_NULLFALL);
//		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(FilePaths.PATH_NETWORK_BERLIN__10PCT);
		new TransitScheduleReaderV1(scenario).readFile(FilePaths.PATH_TRANSIT_SCHEDULE_BERLIN__10PCT_WITHOUT_BUSES_IN_STUDY_AREA);
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(FilePaths.PATH_TRANSIT_VEHICLES_BERLIN__10PCT);
		new PopulationReader(scenario).readFile(FilePaths.PATH_POPULATION_BERLIN__10PCT_FILTERED);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		config.controler().setOutputDirectory(FilePaths.PATH_OUTPUT_BERLIN__10PCT_TAXI_100);
		config.controler().setWritePlansInterval(10);
		config.qsim().setEndTime(60*60*60); // [geloest durch maximum speed in transit_vehicles-datei: bei Stunde 50:00:00 immer noch 492 Veh unterwegs (nur pt veh., keine Agenten), alle pt-fahrten stark verspätet, da pünktlicher start, aber niedrigere Geschwindigkeit als im Fahrplan geplant]
		config.controler().setWriteEventsInterval(10);	
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		Controler controler = new Controler(scenario);
		controler.run();
	}

}

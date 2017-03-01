package playground.gleich.av_bus.prepareScenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult.ValidationIssue;
import org.matsim.vehicles.VehicleReaderV1;

import playground.gleich.av_bus.FilePaths;

public class CheckModifiedInputScenarioData {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		config.transit().setTransitScheduleFile(FilePaths.PATH_TRANSIT_SCHEDULE_BERLIN_100PCT_WITHOUT_BUSES_IN_STUDY_AREA);
//		config.transit().setTransitScheduleFile(FilePaths.PATH_TRANSIT_SCHEDULE_BERLIN_100PCT);
		config.transit().setVehiclesFile(FilePaths.PATH_TRANSIT_VEHICLES_BERLIN_100PCT);
		config.network().setInputFile(FilePaths.PATH_NETWORK_BERLIN_100PCT);
		config.plans().setInputFile(FilePaths.PATH_POPULATION_BERLIN_100PCT_FILTERED);
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(FilePaths.PATH_NETWORK_BERLIN_100PCT);
		new TransitScheduleReaderV1(scenario).readFile(FilePaths.PATH_TRANSIT_SCHEDULE_BERLIN_100PCT_WITHOUT_BUSES_IN_STUDY_AREA);
//		new TransitScheduleReaderV1(scenario).readFile(FilePaths.PATH_TRANSIT_SCHEDULE_BERLIN_100PCT);
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(FilePaths.PATH_TRANSIT_VEHICLES_BERLIN_100PCT_45MPS);
		TransitScheduleValidator.ValidationResult validationResult = TransitScheduleValidator.validateAll(scenario.getTransitSchedule(), scenario.getNetwork());
		for(String s: validationResult.getErrors()){
			System.out.println(s);
		}
		System.out.println();
		for(ValidationIssue i: validationResult.getIssues()){
			System.out.println(i.getMessage());
		}
		System.out.println();
		for(String s: validationResult.getWarnings()){
			System.out.println(s);
		}
		
		new PopulationReader(scenario).readFile(FilePaths.PATH_POPULATION_BERLIN_100PCT_FILTERED);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		config.controler().setOutputDirectory(FilePaths.PATH_OUTPUT_BERLIN_100PCT_MODIFIED_TRANSIT_SCHEDULE_TEST);
		config.controler().setWritePlansInterval(1);
		config.qsim().setEndTime(50*60*60); // [geloest durch maximum speed in transit_vehicles-datei: bei Stunde 50:00:00 immer noch 492 Veh unterwegs (nur pt veh., keine Agenten), alle pt-fahrten stark verspätet, da pünktlicher start, aber niedrigere Geschwindigkeit als im Fahrplan geplant]
		config.controler().setWriteEventsInterval(1);	
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		Controler controler = new Controler(scenario);
		controler.run();
	}

}

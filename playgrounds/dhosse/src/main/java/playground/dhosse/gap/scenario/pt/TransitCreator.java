package playground.dhosse.gap.scenario.pt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

import playground.dhosse.gap.Global;

public class TransitCreator {
	
	public static void createTransit(Scenario scenario) {
		
		new TransitScheduleReader(scenario).readFile(Global.matsimInputDir + "transit/schedule_stopsOnly.xml");
		
		TransitScheduleCSVReader reader = new TransitScheduleCSVReader(scenario);
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/9606.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/9606_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/9608.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/9608_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_1_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_1_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_2_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_2_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_3.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_3_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_4.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne_zum_Einlesen/Linie_5.csv", "bus");
		
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(Global.matsimInputDir + "transit/scheduleComplete.xml");
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(Global.matsimInputDir + "transit/transitVehicles.xml");
		
	}

}

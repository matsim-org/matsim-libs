package playground.dhosse.gap.scenario.pt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

import playground.dhosse.gap.Global;

public class TransitCreator {
	
	public static void createTransit(Scenario scenario) {
		
		new TransitScheduleReader(scenario).readFile(Global.matsimInputDir + "transit/schedule_20150914.xml");
		
		TransitScheduleCSVReader reader = new TransitScheduleCSVReader(scenario);
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/1_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/1_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/2_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/2_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/3_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/3_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/4_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/5_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/4186_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/4186_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9606_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9606_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9606_Rest1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9606_Rest2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9608_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9608_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9611_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9611_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9612_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9612_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9620_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9620_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9621.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9622_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9622_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9623_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9623_2.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/9631_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/eibsee_1.csv", "bus");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/eibsee_2.csv", "bus");
		
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/960_1.csv", "train");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/960_2.csv", "train");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/961_1.csv", "train");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/961_2.csv", "train");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/963_1.csv", "train");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/963_2.csv", "train");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/965_1.csv", "train");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/965_2.csv", "train");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/zsb_1.csv", "train");
		reader.readFileAndAddLines("/home/dhosse/workspace/MAS/input_data/Fahrpläne/inputForMatsim/zsb_2.csv", "train");
		
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(Global.matsimInputDir + "transit/scheduleComplete.xml");
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(Global.matsimInputDir + "transit/transitVehicles.xml");
		
	}

}

package ConfigCreator;

import NetworkCreator.SyntheticNetworkCreator;
import PlansCreator.PlansXMLSynthesizer;
import PlansCreator.RandomCoordinatesGenerator;
import TransitCreator.RailLinkCreator;
import TransitCreator.RailScheduleCreator;
import TransitCreator.VehicleCreator;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;


public class ScenarioCreator {

	private static final String SCENARIO_PATH = "examples/scenarios/UrbanLine/3x1km";

	public static void main(String[] args) throws Exception {

		// 1. Create network.xml
		createNetwork();

		// 2. Create households.xml and commercial.xml
		createHouseholdsAndCommercialCoordinates();

		// 3. Create plans.xml
		createPlans();

		// 4. Create transitschedule.xml
		createRailSchedule();

		// 5. Create transitVehicles.xml
		createVehicles();


		System.out.println("Scenario files successfully created.");
	}

	private static void createNetwork() throws Exception {
		List<Integer> slicesList = List.of(
//			30, 27, 26, //main core
//			21, 20, 19, 18, 17, //urban area
//			16, 15, 14, 13, 12, 11, 10, 9, //suburban transition
//			8, 8, 8, 7, 7, 7,
			27, 27, 27 //suburb
		);
		SyntheticNetworkCreator.main(new String[]{SCENARIO_PATH}, slicesList);
	}

	private static void createHouseholdsAndCommercialCoordinates() throws Exception {
		List<Integer> households = List.of(
//			8000, 10000, 10000, //main core
//			6000, 7000, 5000, 4000, 4200, //urban area
//			2000, 1800, 1500, 1300, 1200, 1000, 900, 800, //suburban transition
//			900, 800, 800, 700, 700, 800,
			800, 500, 700 //suburb
		);
		List<Double> decayRates = List.of(
//			0.1, 0.6, 0.7 ,// main core
//			0.1, 0.7, 0.5, 0.3, 0.2, 0.2, // urban area
//			0.2, 0.2, 0.5, 0.1, 0.8, 0.9, 0.9, 0.8, // suburban transition
//			0.9, 0.8, 0.7, 0.5, 0.5, 0.4,
			0.3, 0.4, 0.9 //suburb
		);
		List<Integer> commercial = List.of(
//			1500, 1500, 1500, //main core
//			1000, 1000, 1000, 1000, 1000, //urban area
//			250, 250, 250, 250, 250, 250, 250, 250, //suburban transition
//			220, 220, 220, 70, 70, 50,
			50, 70, 70 //suburb
		);

		RandomCoordinatesGenerator generator = new RandomCoordinatesGenerator(households, decayRates);
		generator.writeToXML(SCENARIO_PATH, "households.xml");

		generator = new RandomCoordinatesGenerator(commercial, decayRates);
		generator.writeToXML(SCENARIO_PATH, "commercial.xml");
	}

	private static void createPlans() throws ParserConfigurationException, IOException, SAXException {
		PlansXMLSynthesizer synthesizer = new PlansXMLSynthesizer(SCENARIO_PATH);
		int numberOfPlansToGenerate = 500; // specify the desired number of plans here
		synthesizer.synthesize(numberOfPlansToGenerate);
	}

	private static void createRailSchedule() throws IOException {

		double[] distances = {
			// First half: links from 700 to 1500
			700.0, 742.0, 868.0, 910.0,
//			 952.0, 1036.0, 1246.0, 1414.0, 1456.0,
//			// Next quarter: links from 1500 to 3000
//			1666.0, 1998.0, 2330.0, 2994.0,
//			//  last bit: 10 links from 2500 to 3500
//			2765.0, 2895.0
		};

		String[] times = {
			// 5 AM to 6 AM: Early morning, every 15 minutes
			"05:00:00", "05:15:00", "05:30:00", "05:45:00",
			// 6 AM to 7:30 AM: Morning rush, every 5 minutes
			"06:00:00", "06:05:00", "06:10:00", "06:15:00", "06:20:00", "06:25:00", "06:30:00",
			"06:35:00", "06:40:00", "06:45:00", "06:50:00", "06:55:00", "07:00:00", "07:05:00",
			"07:10:00", "07:15:00", "07:20:00", "07:25:00", "07:30:00",
			// 7:30 AM to 9 AM: Peak rush, every 3-4 minutes
			"07:33:00", "07:37:00", "07:40:00", "07:44:00", "07:48:00", "07:52:00", "07:56:00",
			"08:00:00", "08:04:00", "08:08:00", "08:12:00", "08:16:00", "08:20:00", "08:24:00",
			"08:28:00", "08:32:00", "08:36:00", "08:40:00", "08:44:00", "08:48:00", "08:52:00",
			"08:56:00",
			// 9 AM to 4 PM: Off-peak, every 10 minutes
			"09:00:00", "09:10:00", "09:20:00", "09:30:00", "09:40:00", "09:50:00",
			"10:00:00", "10:10:00", "10:20:00", "10:30:00", "10:40:00", "10:50:00",
			"11:00:00", "11:10:00", "11:20:00", "11:30:00", "11:40:00", "11:50:00",
			"12:00:00", "12:10:00", "12:20:00", "12:30:00", "12:40:00", "12:50:00",
			"13:00:00", "13:10:00", "13:20:00", "13:30:00", "13:40:00", "13:50:00",
			"14:00:00", "14:10:00", "14:20:00", "14:30:00", "14:40:00", "14:50:00",
			"15:00:00", "15:10:00", "15:20:00", "15:30:00", "15:40:00", "15:50:00",
			// 4 PM to 7 PM: Evening rush, every 5 minutes
			"16:00:00", "16:05:00", "16:10:00", "16:15:00", "16:20:00", "16:25:00",
			"16:30:00", "16:35:00", "16:40:00", "16:45:00", "16:50:00", "16:55:00",
			"17:00:00", "17:05:00", "17:10:00", "17:15:00", "17:20:00", "17:25:00",
			"17:30:00", "17:35:00", "17:40:00", "17:45:00", "17:50:00", "17:55:00",
			// 7 PM to 10 PM: Evening, every 10 minutes
			"18:00:00", "18:10:00", "18:20:00", "18:30:00", "18:40:00", "18:50:00",
			"19:00:00", "19:10:00", "19:20:00", "19:30:00", "19:40:00", "19:50:00",
			"20:00:00", "20:10:00", "20:20:00", "20:30:00", "20:40:00", "20:50:00",
			"21:00:00", "21:10:00", "21:20:00", "21:30:00", "21:40:00", "21:50:00",
			// 10 PM to 12 AM: Late evening, every 15 minutes
			"22:00:00", "22:15:00", "22:30:00", "22:45:00", "23:00:00", "23:15:00", "23:30:00", "23:45:00",
		};

		String[] vehicleRefIds = new String[times.length];
		for (int i = 0; i < times.length; i++) {
			vehicleRefIds[i] = "tr_" + ((i % 25) + 1);  // rotates between tr_1, tr_2, .... tr_25
		}

		RailScheduleCreator scheduleCreator = new RailScheduleCreator();

		scheduleCreator.generateSchedule(SCENARIO_PATH, times, vehicleRefIds, distances);

	}

	private static void createVehicles(){
		VehicleCreator vehicleCreator = new VehicleCreator();
		int veh_no = 25; // specify the desired number of vehicles here
		vehicleCreator.createTrain(SCENARIO_PATH, veh_no);
	}

}



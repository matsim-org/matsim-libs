package playground.santiago.colectivos;


	

	import java.io.BufferedWriter;
	import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
	import java.util.Map;

	import org.matsim.api.core.v01.Id;
	import org.matsim.api.core.v01.Scenario;

	import org.matsim.api.core.v01.network.Network;
	import org.matsim.api.core.v01.population.Person;
	import org.matsim.core.api.experimental.events.EventsManager;
	import org.matsim.core.config.Config;
	import org.matsim.core.config.ConfigUtils;
	import org.matsim.core.events.EventsUtils;
	import org.matsim.core.events.MatsimEventsReader;
	import org.matsim.core.network.NetworkUtils;
	import org.matsim.core.network.io.MatsimNetworkReader;
	import org.matsim.core.population.io.PopulationReader;
	import org.matsim.core.scenario.ScenarioUtils;
	import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;



	public class PostProcessModalShare {
		
		public static void main(String[] args) {
			String outputFolder =  "C:/Users/Felix/Documents/Bachelor/Santiago de Chile/v3/output_final_newConstants2/";
			String eventsFile = outputFolder + "50.events.xml.gz";
//			String eventsFile = outputFolder + "output_events_test.xml";
			String networkFile = outputFolder + "output_network.xml.gz";
//			String plansFile = outputFolder + "output_plans.xml.gz";
						
//			Network network = NetworkUtils.createNetwork();
//			new MatsimNetworkReader(network).readFile(networkFile);
			EventsManager events = EventsUtils.createEventsManager();
						
//			Config config = ConfigUtils.createConfig();
//			Scenario scenario = ScenarioUtils.createScenario(config);
//			new PopulationReader(scenario).readFile(plansFile);
			
			ColectivoModalShareEvaluator colectivoModalShareEvaluator = new ColectivoModalShareEvaluator();
			events.addHandler(colectivoModalShareEvaluator);
			
			new MatsimEventsReader(events).readFile(eventsFile);
			Map<String,Integer> rides = new HashMap<>();
			rides.put("colectivos", colectivoModalShareEvaluator.getColectivo());
			
			ArrayList<Id<Vehicle>> vehicles = new ArrayList<>();
			vehicles = colectivoModalShareEvaluator.getVehicles();
			writeHistogramData(outputFolder+"ColectivoRides.txt", rides);
//			writeVehicles(outputFolder+"CoVehicles.txt", vehicles);
		}
		public static void writeVehicles(String filename, ArrayList<Id<Vehicle>> vehicles){
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			try {
				
				for (Id<Vehicle> veh : vehicles) {

					bw.newLine();
					bw.write(veh.toString());
				}
				bw.flush();
				bw.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public static void writeHistogramData(String filename, Map<String, Integer> rides) {
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			try {
				bw.write("mode ; trips");
				for (HashMap.Entry<String,Integer> average : rides.entrySet()) {

					bw.newLine();
					bw.write(average.getKey()+" ; "+average.getValue().intValue());
				}
				bw.flush();
				bw.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}


	}

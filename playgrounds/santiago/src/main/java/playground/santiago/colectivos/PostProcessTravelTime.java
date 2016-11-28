package playground.santiago.colectivos;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
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


public class PostProcessTravelTime {
	
	public static void main(String[] args) {
		String outputFolder = "C:/Users/Felix/Documents/Uni/Santiago de Chile/v1/santiago/outputWithCollectivo(FC_SC1)/";
		String eventsFile = outputFolder + "output_events.xml.gz";
		String networkFile = outputFolder + "output_network.xml.gz";
		String plansFile = outputFolder + "output_plans.xml.gz";
		
		
		
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);
		EventsManager events = EventsUtils.createEventsManager();
		
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(plansFile);
				
		
		double beelineFactorBike = config.plansCalcRoute().getBeelineDistanceFactors().get(TransportMode.bike);
		System.out.println(beelineFactorBike);
	

		double beelineFactorWalk = config.plansCalcRoute().getBeelineDistanceFactors().get(TransportMode.walk);
		System.out.println(beelineFactorWalk);
		

	
		
		double beelineFactorPt = config.plansCalcRoute().getBeelineDistanceFactors().get(TransportMode.pt);
		
		


		new MatsimEventsReader(events).readFile(eventsFile);
//		Id<Person> personId = vehicleId2PersonId(event.getVehicleId());

	
	}
	
	
	
	

	public static void writeHistogramData(String filename, int[] distanceClasses) {
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("distance;rides");
			for (int i = 0; i < distanceClasses.length; i++) {
				bw.newLine();
				bw.write(i + ";" + distanceClasses[i]);
			}
			bw.flush();
			bw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


}

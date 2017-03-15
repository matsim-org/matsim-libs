package playground.santiago.colectivos;

import java.io.BufferedWriter;
import java.io.IOException;
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



public class PostProcessTravelTime {
	
	public static void main(String[] args) {
		String outputFolder =  "C:/Users/Felix/Documents/Uni/Santiago de Chile/v1/santiago/outputWithCollectivo(FC_SC1)/";
		String eventsFile = outputFolder + "output_events.xml.gz";
		String networkFile = outputFolder + "output_network.xml.gz";
		String plansFile = outputFolder + "output_plans.xml.gz";
		
		
		
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);
		EventsManager events = EventsUtils.createEventsManager();
		
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(plansFile);
			
		ColectivoTraveltimesEvaluator colectivoTraveltimesEvaluator = new ColectivoTraveltimesEvaluator();
		
		events.addHandler(colectivoTraveltimesEvaluator);
		new MatsimEventsReader(events).readFile(eventsFile);
		Map<Id<Person>,Double> travel = new HashMap<>();
		HashMap<Integer,Double> averageTraveltimes = new HashMap<>();
		travel = colectivoTraveltimesEvaluator.getTravelTimes();
		int numberOfDepartures = 0;
		double sumOfTimes=0.0;
		double average=0.0;
//		travel.values();
//		System.out.println(travel);
//		Id<Person> leute = travel.get(key);
		for ( int a=1; a <382; a++ ){
			numberOfDepartures=0;
			sumOfTimes=0.0;
			
			for (Map.Entry<Id<Person>,Double> person : travel.entrySet()){
				if (person.getKey().toString().contains("pt_co"+a+"_")){
//					System.out.println(person.getKey() +"  "+ person.getValue());
					numberOfDepartures++;
					sumOfTimes = sumOfTimes+person.getValue();					
				}
			}
			average= sumOfTimes/numberOfDepartures;
			averageTraveltimes.put(a,average);
		}
		for (HashMap.Entry<Integer,Double> averages : averageTraveltimes.entrySet())
			{
			System.out.println("Linenumber:"+averages.getKey()+"  Traveltime: "+averages.getValue());
			}

		writeHistogramData(outputFolder+"Traveltimes3.csv", averageTraveltimes);
	}
		

	public static void writeHistogramData(String filename, HashMap<Integer,Double> averages) {
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("Linenumber ; Traveltime");
			for (HashMap.Entry<Integer,Double> average : averages.entrySet()) {

				bw.newLine();
				bw.write(average.getKey()+";"+average.getValue().intValue());
			}
			bw.flush();
			bw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


}

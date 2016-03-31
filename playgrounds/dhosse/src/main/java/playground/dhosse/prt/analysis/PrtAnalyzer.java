package playground.dhosse.prt.analysis;

import java.io.*;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.michalm.taxi.data.file.ETaxiReader;

public class PrtAnalyzer {
	
	public static void main(String args[]){
	
		double scenarioEndTime = 30*3600;
//		String sc = "NOS";
		String sc = "4persons";
//		String sc = "15persons";
		
		double[] sum_ttime_prt_pax = new double[300];
		double[] sum_tdis_prt_pax = new double[300];
		double[] sum_wtime_prt_pax = new double[300];
		double[] sum_ttime_prt = new double[300];
		double[] sum_tdis_prt = new double[300];
		double[] sum_pax_prt = new double[300];
		
		for(int i = 1; i < 301; i++){
		
		int nVeh = i;
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/network_prt.xml");
		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/population_prt_final2.xml");
		VrpData data = new VrpDataImpl();
		ETaxiReader vehReader = new ETaxiReader(scenario.getNetwork(), data);
		vehReader.parse("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/vehicles/" + nVeh + "_vehicles.xml");
		
		EventsManager events = EventsUtils.createEventsManager();
		PrtEventsHandler handler = new PrtEventsHandler(scenario);
		events.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		
		handler.reset(0);
		String file = nVeh > 1 ? "C:/Users/Daniel/Desktop/dvrp/old results/" + sc + "/" + nVeh + "_vehicles/events_dvrp.xml" :
			"C:/Users/Daniel/Desktop/dvrp/old results/" + sc + "/" + nVeh + "_vehicle/events_dvrp.xml";
		reader.readFile(file);
		
		BufferedWriter writer = IOUtils.getBufferedWriter("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/results/" +
		sc + "/pax/stats_pax_" + nVeh + "_vehicles.csv");
		
		try {

			writer.write("person id;ttime_walk;tdis_walk;ttime_prt;tdis_prt;wtime");
			writer.newLine();

			for(Person person : scenario.getPopulation().getPersons().values()){
				
				String personIdString = person.getId().toString();
				String ttimeWalkString = Double.toString(handler.travelTimesPerPersonWalk.get(person.getId()));
				String tdisWalkString = Double.toString(handler.travelDistancesWalkPerPerson.get(person.getId()));
				String ttimePrtString = Double.toString(handler.travelTimesPerPersonPrt.get(person.getId()));
				String tdisPrtString = Double.toString(handler.travelDistancesPrtPerPerson.get(person.getId()));
				
				double wtime = 0.;
				//the person might not have been picked up by a vehicle
				if(handler.waitingTimesPerPerson.get(person.getId()) == 0){
					
					if(handler.personId2BeginWaitingTime.get(person.getId()) > 0){
						
						wtime = scenarioEndTime - handler.personId2BeginWaitingTime.get(person.getId());
						
					}
					
				} else{
					
					if(handler.personId2BeginWaitingTime.get(person.getId()) > 0){
						wtime += scenarioEndTime - handler.personId2BeginWaitingTime.get(person.getId());
					}
					
					wtime += handler.waitingTimesPerPerson.get(person.getId());
					
				}
				
				sum_ttime_prt_pax[i-1] += handler.travelTimesPerPersonPrt.get(person.getId());
				sum_tdis_prt_pax[i-1] += handler.travelDistancesPrtPerPerson.get(person.getId());
				sum_wtime_prt_pax[i-1] += wtime;
				
				String wtimeString = Double.toString(wtime);
				
				writer.write(personIdString + ";" + ttimeWalkString + ";" + tdisWalkString +
						";" + ttimePrtString + ";" + tdisPrtString + ";" + wtimeString);
				writer.newLine();
				
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		writer = IOUtils.getBufferedWriter("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/results/" +
				sc + "/vehicles/stats_veh" + nVeh + "_vehicles.csv");
		
		try {

			writer.write("vehicle id;ttime;tdis;n passengers");
			writer.newLine();
			
			for(Vehicle vehicle : data.getVehicles().values()){
				
				String vehIdString = vehicle.getId().toString();
				String ttimeString = Double.toString(handler.travelTimesPerVehicle.get(vehicle.getId()));
				String tdisString = Double.toString(handler.travelDistancesPerVehicle.get(vehicle.getId()));
				String nPassengersString = Integer.toString(handler.vehicleIds2PassengerCounts.get(vehicle.getId()));
				
				sum_ttime_prt[i-1] += handler.travelTimesPerVehicle.get(vehicle.getId());
				sum_tdis_prt[i-1] += handler.travelDistancesPerVehicle.get(vehicle.getId());
				sum_pax_prt[i-1] += handler.vehicleIds2PassengerCounts.get(vehicle.getId());
				
				writer.write(vehIdString + ";" + ttimeString + ";" + tdisString + ";" + nPassengersString);
				writer.newLine();
				
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		}
		
		BufferedWriter writer = IOUtils.getBufferedWriter("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/results/" +
				sc + "results.csv");
		
			try {
				
				writer.write("n_veh;ttime_pax;tdis_pax;wtime_pax;ttime_veh;tdis_veh;n_pax");
				writer.newLine();
				
				for(int i = 0; i < 300; i++){
					
					writer.write((i+1) + ";" + sum_ttime_prt_pax[i] + ";" + sum_tdis_prt_pax[i] + ";" + sum_wtime_prt_pax[i] + ";"
							+ sum_ttime_prt[i] + ";" + sum_tdis_prt[i] + ";" + sum_pax_prt[i]);
					writer.newLine();
				
				}
				
				writer.flush();
				writer.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
	}

}

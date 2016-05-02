package playground.dhosse.gap.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.minibus.genericUtils.RecursiveStatsContainer;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

public class GapAnalysis {

	private static final int idxPersonId = 0;
	private static final int idxStartTime = 1;
	private static final int idxEndTime = 2;
	private static final int idxStartLinkId = 3;
	private static final int idxEndLinkId = 4;
	private static final int idxDistance = 5;
	private static final int idxAccessTime = 6;
	private static final int idxEgressTime = 7;
	private static final int idxVehicleId = 8;
	
//	private static final int nVehicles = 204;
	
	private static String inputPath = "/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/"
			+ "2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/";
	
	private static Network network;
	
	public static void main(String args[]){
	
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		network = sc.getNetwork();
		new MatsimNetworkReader(network).readFile("/home/dhosse/run12/input/networkMultimodal.xml");
		
		String scenario = "base";
		String outputDirectory = "/home/dhosse/stats/" + scenario + "/";
		
		GapAnalysis.run(inputPath + "run12/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run17/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run21/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run25/output/ITERS/it.100/100.ow_cs");
		
		new File(outputDirectory).mkdirs();
		writeVehicleStats(outputDirectory, vehicleStats);
		writeStationStats(outputDirectory, stationStats);
		
		personStats = new HashMap<>();
		vehicleStats = new HashMap<>();
		stationStats = new HashMap<>();
		vehicleTravelTime = new RecursiveStatsContainer();
		vehicleTravelDistance = new RecursiveStatsContainer();
		vehicleOccupancy = new RecursiveStatsContainer();
		
		scenario = "base_reducedCosts";
		outputDirectory = "/home/dhosse/stats/" + scenario + "/";
		
		GapAnalysis.run(inputPath + "run13/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run18/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run22/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run26/output/ITERS/it.100/100.ow_cs");
		
		new File(outputDirectory).mkdirs();
		writeVehicleStats(outputDirectory, vehicleStats);
		writeStationStats(outputDirectory, stationStats);
		
		personStats = new HashMap<>();
		vehicleStats = new HashMap<>();
		stationStats = new HashMap<>();
		vehicleTravelTime = new RecursiveStatsContainer();
		vehicleTravelDistance = new RecursiveStatsContainer();
		vehicleOccupancy = new RecursiveStatsContainer();
		
		scenario = "ext";
		outputDirectory = "/home/dhosse/stats/" + scenario + "/";
		
		GapAnalysis.run(inputPath + "run15/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run19/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run23/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run27/output/ITERS/it.100/100.ow_cs");
		
		new File(outputDirectory).mkdirs();
		writeVehicleStats(outputDirectory, vehicleStats);
		writeStationStats(outputDirectory, stationStats);
		
		personStats = new HashMap<>();
		vehicleStats = new HashMap<>();
		stationStats = new HashMap<>();
		vehicleTravelTime = new RecursiveStatsContainer();
		vehicleTravelDistance = new RecursiveStatsContainer();
		vehicleOccupancy = new RecursiveStatsContainer();
		
		scenario = "ext_reducedCosts";
		outputDirectory = "/home/dhosse/stats/" + scenario + "/";
		
		GapAnalysis.run(inputPath + "run16/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run20/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run24/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run28/output/ITERS/it.100/100.ow_cs");
		
		new File(outputDirectory).mkdirs();
		writeVehicleStats(outputDirectory, vehicleStats);
		writeStationStats(outputDirectory, stationStats);
		
		personStats = new HashMap<>();
		vehicleStats = new HashMap<>();
		stationStats = new HashMap<>();
		vehicleTravelTime = new RecursiveStatsContainer();
		vehicleTravelDistance = new RecursiveStatsContainer();
		vehicleOccupancy = new RecursiveStatsContainer();
		
		scenario = "infrastructure";
		outputDirectory = "/home/dhosse/stats/" + scenario + "/";
		
		GapAnalysis.run(inputPath + "run29/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run31/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run33/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run35/output/ITERS/it.100/100.ow_cs");
		
		new File(outputDirectory).mkdirs();
		writeVehicleStats(outputDirectory, vehicleStats);
		writeStationStats(outputDirectory, stationStats);
		
		personStats = new HashMap<>();
		vehicleStats = new HashMap<>();
		stationStats = new HashMap<>();
		vehicleTravelTime = new RecursiveStatsContainer();
		vehicleTravelDistance = new RecursiveStatsContainer();
		vehicleOccupancy = new RecursiveStatsContainer();
		
		scenario = "infrastructure_reducedCosts";
		outputDirectory = "/home/dhosse/stats/" + scenario + "/";
		
		GapAnalysis.run(inputPath + "run30/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run32/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run34/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run36/output/ITERS/it.100/100.ow_cs");
		
		new File(outputDirectory).mkdirs();
		writeVehicleStats(outputDirectory, vehicleStats);
		writeStationStats(outputDirectory, stationStats);
		
		personStats = new HashMap<>();
		vehicleStats = new HashMap<>();
		stationStats = new HashMap<>();
		vehicleTravelTime = new RecursiveStatsContainer();
		vehicleTravelDistance = new RecursiveStatsContainer();
		vehicleOccupancy = new RecursiveStatsContainer();
		
		scenario = "30kmZone";
		outputDirectory = "/home/dhosse/stats/" + scenario + "/";
		
		GapAnalysis.run(inputPath + "run37/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run39/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run41/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run43/output/ITERS/it.100/100.ow_cs");
		
		new File(outputDirectory).mkdirs();
		writeVehicleStats(outputDirectory, vehicleStats);
		writeStationStats(outputDirectory, stationStats);
		
		personStats = new HashMap<>();
		vehicleStats = new HashMap<>();
		stationStats = new HashMap<>();
		vehicleTravelTime = new RecursiveStatsContainer();
		vehicleTravelDistance = new RecursiveStatsContainer();
		vehicleOccupancy = new RecursiveStatsContainer();
		
		scenario = "30kmZone_reducedCosts";
		outputDirectory = "/home/dhosse/stats/" + scenario + "/";
		
		GapAnalysis.run(inputPath + "run38/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run40/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run42/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run44/output/ITERS/it.100/100.ow_cs");
		
		new File(outputDirectory).mkdirs();
		writeVehicleStats(outputDirectory, vehicleStats);
		writeStationStats(outputDirectory, stationStats);
		
		personStats = new HashMap<>();
		vehicleStats = new HashMap<>();
		stationStats = new HashMap<>();
		vehicleTravelTime = new RecursiveStatsContainer();
		vehicleTravelDistance = new RecursiveStatsContainer();
		vehicleOccupancy = new RecursiveStatsContainer();
		
		scenario = "mutable";
		outputDirectory = "/home/dhosse/stats/" + scenario + "/";
		
		GapAnalysis.run(inputPath + "run45/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run47/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run49/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run51/output/ITERS/it.100/100.ow_cs");
		
		new File(outputDirectory).mkdirs();
		writeVehicleStats(outputDirectory, vehicleStats);
		writeStationStats(outputDirectory, stationStats);
		
		personStats = new HashMap<>();
		vehicleStats = new HashMap<>();
		stationStats = new HashMap<>();
		vehicleTravelTime = new RecursiveStatsContainer();
		vehicleTravelDistance = new RecursiveStatsContainer();
		vehicleOccupancy = new RecursiveStatsContainer();
		
		scenario = "mutable_reducedCosts";
		outputDirectory = "/home/dhosse/stats/" + scenario + "/";
		
		GapAnalysis.run(inputPath + "run46/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run48/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run50/output/ITERS/it.100/100.ow_cs");
		GapAnalysis.run(inputPath + "run52/output/ITERS/it.100/100.ow_cs");
		
		new File(outputDirectory).mkdirs();
		writeVehicleStats(outputDirectory, vehicleStats);
		writeStationStats(outputDirectory, stationStats);
		
	}
	
	static Map<Id<Person>, PersonStats> personStats = new HashMap<>();
	static Map<Id<Link>, StationStats> stationStats = new HashMap<>();
	static Map<Id<Vehicle>, VehicleStats> vehicleStats = new HashMap<>();
	
	//TODO used and unused vehicles separately
	static RecursiveStatsContainer vehicleTravelTime = new RecursiveStatsContainer();
	static RecursiveStatsContainer vehicleTravelDistance = new RecursiveStatsContainer();
	static RecursiveStatsContainer vehicleOccupancy = new RecursiveStatsContainer();
	
	public static void run(String csStatsFile){
		
		BufferedReader reader = IOUtils.getBufferedReader(csStatsFile);
		
		try {
			
			String line = reader.readLine();
			
			while((line = reader.readLine()) != null){
				
				String[] lineParts = line.split(" ");
				
				Id<Person> personId = Id.createPersonId(lineParts[idxPersonId]);
				double startTime = Double.parseDouble(lineParts[idxStartTime]);
				double endTime = Double.parseDouble(lineParts[idxEndTime]);
				Id<Link> startLinkId = Id.createLinkId(lineParts[idxStartLinkId]);
				Id<Link> endLinkId = Id.createLinkId(lineParts[idxEndLinkId]);
				double distance = Double.parseDouble(lineParts[idxDistance]);
				double accessTime = Double.parseDouble(lineParts[idxAccessTime]);
				double egressTime = Double.parseDouble(lineParts[idxEgressTime]);
				Id<Vehicle> vehicleId = Id.create(lineParts[idxVehicleId], Vehicle.class);

				double duration = endTime - startTime;
				
				if(distance > 0){
					
					if(!personStats.containsKey(personId)){
						
						personStats.put(personId, new PersonStats());
						
					}
					
					if(!stationStats.containsKey(startLinkId)){
						
						stationStats.put(startLinkId, new StationStats());
						
					}
					
					if(!stationStats.containsKey(endLinkId)){
						
						stationStats.put(endLinkId, new StationStats());
						
					}
					
					if(!vehicleStats.containsKey(vehicleId)){
						
						vehicleStats.put(vehicleId, new VehicleStats());
						
					}
					
					personStats.get(personId).handleNewInformation(vehicleId, accessTime, egressTime, duration, distance);
					stationStats.get(startLinkId).handleNewInformation(1, 0);
					stationStats.get(endLinkId).handleNewInformation(0, 1);
					vehicleStats.get(vehicleId).handleNewInformation(personId, duration, distance);
					vehicleTravelTime.handleNewEntry(duration);
					vehicleTravelDistance.handleNewEntry(distance);
					
				}
				
			}
			
			reader.close();
			
		} catch (IOException e) {

			e.printStackTrace();
			
		}
		
		for(VehicleStats stats : vehicleStats.values()){
			
			double wholeDay = 24 * 3600;
			
			double occupancy = stats.getTotalTravelTime() / wholeDay;
			vehicleOccupancy.handleNewEntry(occupancy);
			
		}
		
	}
	
	private static void writeVehicleStats(String outputDirectory, Map<Id<Vehicle>, VehicleStats> stats){
		
		BufferedWriter statsWriter = IOUtils.getBufferedWriter(outputDirectory + "vehicleStats.csv");
		
		try{
			
			statsWriter.write("id;n_rides;n_drivers;mileage;ttime");
			
			for(Entry<Id<Vehicle>, VehicleStats> entry : stats.entrySet()){
				
				statsWriter.newLine();
				statsWriter.write(entry.getKey().toString() + ";" + (double)entry.getValue().numberOfRides / 4 + ";" +
						(double)entry.getValue().driverIds.size() / 4 + ";" + (double)entry.getValue().totalMileage / 4 + ";" +
						(double)entry.getValue().totalTravelTime / 4);
				
			}
			
			statsWriter.flush();
			statsWriter.close();
			
		} catch(IOException e){
			e.printStackTrace();
		}
		
	}

	private static void writeStationStats(String outputDirectory,
			Map<Id<Link>, StationStats> stationStats) {
		BufferedWriter stationStatsWriter = IOUtils.getBufferedWriter(outputDirectory + "stationStats.csv");

		try {
			
			stationStatsWriter.write("id;n_boardings;n_alightings;x;y");
		
			for(Entry<Id<Link>,StationStats> entry : stationStats.entrySet()){
				
				stationStatsWriter.newLine();
				stationStatsWriter.write(entry.getKey().toString() + ";" + (double)entry.getValue().numberOfBoardings / 4 +
						";" + (double)entry.getValue().numberOfAlightings / 4 + ";" +
						network.getLinks().get(entry.getKey()).getCoord().getX() +
						";" + network.getLinks().get(entry.getKey()).getCoord().getY());
				
			}
			
			stationStatsWriter.flush();
			stationStatsWriter.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static class PersonStats{
		
		private Set<Id<Vehicle>> vehicleIds;
		private int numberOfCsLegs;
		private double accessTime;
		private double egressTime;
		private double totalTravelTime;
		private double totalTravelDistance;
		
		public PersonStats(){
			this.vehicleIds = new HashSet<>();
		}
		
		public Set<Id<Vehicle>> getVehicleIds() {
			return vehicleIds;
		}

		public int getNumberOfCsLegs() {
			return numberOfCsLegs;
		}

		public double getAccessTime() {
			return accessTime;
		}

		public double getEgressTime() {
			return egressTime;
		}

		public double getTotalTravelTime() {
			return totalTravelTime;
		}

		public double getTotalTravelDistance() {
			return totalTravelDistance;
		}

		public void handleNewInformation(Id<Vehicle> vehicleId, double accessTime, double egressTime, double travelTime, double travelDistance){
		
			this.vehicleIds.add(vehicleId);
			this.accessTime += accessTime;
			this.egressTime += egressTime;
			this.totalTravelTime += travelTime;
			this.totalTravelDistance += travelDistance;
			
		}
		
	}
	
	static class StationStats{
		
		private int numberOfBoardings;
		private int numberOfAlightings;
		
		public int getNumberOfBoardings(){
			return numberOfBoardings;
		}
		
		public int getNumberOfAlightings(){
			return numberOfAlightings;
		}
		
		public void handleNewInformation(int boardings, int alightings){
			
			this.numberOfBoardings += boardings;
			this.numberOfAlightings += alightings;
			
		}
		
	}
	
	static class VehicleStats{
		
		private Set<Id<Person>> driverIds;
		private int numberOfRides;
		private double totalTravelTime;
		private double totalMileage;
		
		public VehicleStats(){
			this.driverIds = new HashSet<>();
		}
		
		public int getNumberOfRides() {
			return numberOfRides;
		}
		
		public double getTotalTravelTime() {
			return totalTravelTime;
		}
		
		public double getTotalMileage() {
			return totalMileage;
		}
		
		public void handleNewInformation(Id<Person> driverId, double travelTime, double distance){
			
			if(!this.driverIds.contains(driverId)){
				
				this.driverIds.add(driverId);
				
			}
			
			this.numberOfRides++;
			this.totalTravelTime += travelTime;
			this.totalMileage += distance;
			
		}
		
	}
	
}

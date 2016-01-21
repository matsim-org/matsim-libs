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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.minibus.genericUtils.RecursiveStatsContainer;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

public class GapAnalysis {

	/*
	 * Auswertung:
	 * Modal split
	 * -V'aufkommen
	 * -V'leistung
	 * standzeit
	 * fahrleistung
	 * auslastung (t_occupied / day)
	 */
	
	private static final int idxPersonId = 0;
	private static final int idxStartTime = 1;
	private static final int idxEndTime = 2;
	private static final int idxStartLinkId = 3;
	private static final int idxEndLinkId = 4;
	private static final int idxDistance = 5;
	private static final int idxAccessTime = 6;
	private static final int idxEgressTime = 7;
	private static final int idxVehicleId = 8;
	
	private static final int nVehicles = 204;
	
	public static void main(String args[]){
		
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run12/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/base/12/");
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run17/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/base/17/");
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run21/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/base/21/");
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run25/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/base/25/");
		
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run13/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/base_reducedCosts/13/");
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run18/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/base_reducedCosts/18/");
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run22/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/base_reducedCosts/22/");
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run26/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/base_reducedCosts/26/");
		
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run15/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/ext/15/");
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run19/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/ext/19/");
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run23/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/ext/23/");
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run27/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/ext/27/");
		
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run16/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/ext_reducedCosts/16/");
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run20/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/ext_reducedCosts/20/");
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run24/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/ext_reducedCosts/24/");
		GapAnalysis.run("/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/"
				+ "run28/output/ITERS/it.100/100.ow_cs", "/home/dhosse/stats/ext_reducedCosts/28/");
		
	}
	
	public static void run(String csStatsFile, String outputDirectory){
		
		BufferedReader reader = IOUtils.getBufferedReader(csStatsFile);
		
		Map<Id<Person>, PersonStats> personStats = new HashMap<>();
		Map<Id<Link>, StationStats> stationStats = new HashMap<>();
		Map<Id<Vehicle>, VehicleStats> vehicleStats = new HashMap<>();
		
		//TODO used and unused vehicles separately
		RecursiveStatsContainer vehicleTravelTime = new RecursiveStatsContainer();
		RecursiveStatsContainer vehicleTravelDistance = new RecursiveStatsContainer();
		RecursiveStatsContainer vehicleOccupancy = new RecursiveStatsContainer();
		
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
		
		new File(outputDirectory).mkdirs();
		
		writeVehicleStats(outputDirectory, vehicleStats);
		writeStationStats(outputDirectory, stationStats);
		
	}
	
	private static void writeVehicleStats(String outputDirectory, Map<Id<Vehicle>, VehicleStats> stats){
		
		BufferedWriter statsWriter = IOUtils.getBufferedWriter(outputDirectory + "vehicleStats.csv");
		
		try{
			
			statsWriter.write("id;n_rides;n_drivers;mileage;ttime");
			
			for(Entry<Id<Vehicle>, VehicleStats> entry : stats.entrySet()){
				
				statsWriter.newLine();
				statsWriter.write(entry.getKey().toString() + ";" + entry.getValue().numberOfRides + ";" + entry.getValue().driverIds.size() + ";" + entry.getValue().totalMileage + ";" + entry.getValue().totalTravelTime);
				
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
			
			stationStatsWriter.write("id;n_boardings;n_alightings");
		
			for(Entry<Id<Link>,StationStats> entry : stationStats.entrySet()){
				
				stationStatsWriter.newLine();
				stationStatsWriter.write(entry.getKey().toString() + ";" + entry.getValue().numberOfBoardings + ";" + entry.getValue().numberOfAlightings);
				
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

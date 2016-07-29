package playground.dhosse.gap.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import playground.dhosse.utils.io.AbstractCsvReader;


public class CsOccupancyStatsAnalyzer {

	private static final Logger log = Logger.getLogger(CsOccupancyStatsAnalyzer.class);
	
	private static double[] busyVehiclesAtTimeSlice = new double[31];
	
	private static String scenario = "mutable-reducedCosts";
//	private static int[] runIds = new int[]{45,47,49,51};
	private static int[] runIds = new int[]{46};
	private static double nVehicles = 0;
	
//	private static String inputPath = "/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/"
//			+ "2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/";
	private static String inputPath = "/home/dhosse/stats/";
	
	private static Map<Integer, Set<Id<Vehicle>>> timeStep2UsedVehicleIds = 
			new HashMap<Integer, Set<Id<Vehicle>>>();
	
	public static void main(String args[]){

		if(scenario.contains("mutable")){
			
			AbstractCsvReader stationsReader = new AbstractCsvReader("\t", true) {
				
				@Override
				public void handleRow(String[] line) {
					
					nVehicles += Double.parseDouble(line[6]);
					
				}
				
			};
			
			for(int runId : runIds){
				stationsReader.read(inputPath + "run" + Integer.toString(runId) + "/output/csStations/"
						+ "100_stations.txt");
			}
			
		} else if(scenario.contains("base")){
			
			nVehicles = 4 * 6;
			
		} else {
			
			nVehicles = 4 * 204;
			
		}
		
		log.info("There were " + (int)nVehicles + " car sharing vehicles found...");
		
		AbstractCsvReader reader = new AbstractCsvReader(" ", true) {
			
			@Override
			public void handleRow(String[] line) {
				
				Id<Vehicle> vehicleId = Id.createVehicleId(line[8]);
				if(!vehicleId.toString().equals("null")){
					
					double startTime = Double.parseDouble(line[1]);
					double endTime = Double.parseDouble(line[2]);
					
					int startingHour = (int)(startTime / 3600);
					int endingHour = (int)(endTime / 3600);
					
					if(!timeStep2UsedVehicleIds.get(startingHour).contains(vehicleId)){
						
						timeStep2UsedVehicleIds.get(startingHour).add(vehicleId);
						busyVehiclesAtTimeSlice[startingHour] += 1;
						
					}
					
					if(!timeStep2UsedVehicleIds.get(endingHour).contains(vehicleId) &&
							endingHour != startingHour){
						
						timeStep2UsedVehicleIds.get(endingHour).add(vehicleId);
						busyVehiclesAtTimeSlice[endingHour] += 1;
						
					}
					
				}
				
			}
			
		};
		
		for(int runId : runIds){
			
			for(int i = 0; i < 31; i++){
				timeStep2UsedVehicleIds.put(i, new HashSet<>());
			}
			
			reader.read(inputPath + "run" + Integer.toString(runId) +
					"/output/ITERS/it.100/100.OW_CS");
			
		}
		
		BufferedWriter writer = IOUtils.getBufferedWriter("/home/dhosse/stats/occupiedVehicles_" +
				scenario + "2.csv");
		
		try {
		
			writer.write("hour\tpOccupiedVehices");
			
			for(int i = 0; i < 31; i++){

				writer.newLine();
				writer.write(i + "\t" + busyVehiclesAtTimeSlice[i]/nVehicles);
				
			}
			
			writer.flush();
			writer.close();
		
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}

}

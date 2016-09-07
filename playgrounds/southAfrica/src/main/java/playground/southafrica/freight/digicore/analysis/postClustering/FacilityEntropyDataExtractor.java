/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractFacilityEntropyData.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
/**
 * 
 */
package playground.southafrica.freight.digicore.analysis.postClustering;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader;
import playground.southafrica.utilities.Header;

/**
 * This class reads Digicore Vehicles's activity chains (once clustered and 
 * cleaned), and check the number of facility visits each vehicle has had at
 * each of the identified facilities. This class has been written to handle
 * a single-file population of {@link DigicoreVehicles}.
 * 
 * @author jwjoubert
 */
public class FacilityEntropyDataExtractor {
	final private static Logger LOG = Logger.getLogger(FacilityEntropyDataExtractor.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(FacilityEntropyDataExtractor.class.toGenericString(), args);
		
		String inputFile = args[0];
		String outputFile = args[1];
		
		/* Parse population. */
		DigicoreVehicles dv = new DigicoreVehicles("WGS_SA_Albers");
		new DigicoreVehiclesReader(dv).readFile(inputFile);
		
		/* Executing the extraction. */
		FacilityEntropyDataExtractor.extract(dv, outputFile);
		
		Header.printFooter();
	}
	
	private FacilityEntropyDataExtractor() {
		/* Hide constructor. */
	}
	
	/**
	 * Extract for each vehicle the number of visits to each facility. Only 
	 * {@link DigicoreActivity}s with an {@link Id}<{@link ActivityFacility}> 
	 * are considered. The output is written in comma-separated format with
	 * three columns:<br><br>
	 * <code>truckId,facilityId,visits</code>
	 * @param vehicles
	 * @param output
	 */
	public static void extract(DigicoreVehicles vehicles, String output){
		LOG.info("Extracting facility entropy data...");
		Map<Id<Vehicle>, Map<Id<ActivityFacility>, Integer>> map = new TreeMap<Id<Vehicle>, Map<Id<ActivityFacility>, Integer>>();
		Map<Id<ActivityFacility>, Coord> coordMap = new HashMap<Id<ActivityFacility>, Coord>();
		Counter counter = new Counter("  vehicles # ");
		for(Id<Vehicle> vid : vehicles.getVehicles().keySet()) {
//			map.put(vid, new TreeMap<>());
			Map<Id<ActivityFacility>, Integer> vehicleMap = new TreeMap<>();
			
			DigicoreVehicle dv = vehicles.getVehicles().get(vid);
			for(DigicoreChain chain : dv.getChains()){
				for(DigicoreActivity activity : chain.getMinorActivities()){
					Id<ActivityFacility> fid = activity.getFacilityId();
					if(fid != null){
						if(vehicleMap.containsKey(fid)){
							int oldValue = vehicleMap.get(fid);
							vehicleMap.put(fid, oldValue+1);
						} else{
							vehicleMap.put(fid, 1);
							coordMap.put(fid, activity.getCoord());
						}
					}
				}
			}
			counter.incCounter();
			
			/* Only add the vehicle if it has any facility visits. */
			if(vehicleMap.size() > 0){
				map.put(vid, vehicleMap);
			}
		}
		counter.printCounter();
		LOG.info("Done extracting.");
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		
		LOG.info("Writing outpout to file...");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			bw.write("truckId,facilityId,visits,lon,lat");
			bw.newLine();
			for(Id<Vehicle> vid : map.keySet()){
				for(Id<ActivityFacility> fid : map.get(vid).keySet()){
					Coord c = ct.transform(coordMap.get(fid));
					String entry = String.format("%s,%s,%d,%.6f,%.6f\n", 
							vid.toString(), 
							fid.toString(), 
							map.get(vid).get(fid),
							c.getX(), c.getY());
					bw.write(entry);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		LOG.info("Done writing.");
	}
}

package playground.smeintjes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.freight.digicore.algorithms.complexNetwork.DigicoreNetworkParser;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreNetwork;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

	/** This class calculates the completeness of each clustering parameter
	 *  configuration in an area (defined by a shapefile). Completeness is defined
	 *  as the number of activities that has a facilityID, AND that is included
	 *  in the network as a node. 
	 * @param args
	 * @author sumarie
	 */
public class CompletenessPerConfiguration {
	final private static Logger LOG = Logger.getLogger(CompletenessPerConfiguration.class);
	
	public static void main(String[] args) {
		Header.printHeader(CompletenessPerConfiguration.class.toString(), args);
		String sourceFolder = args[0];
		String shapefile = args[1];
		String outputFolder = args[2];
		
		MultiPolygon areaShape = readInShapefile(shapefile);
		
		ArrayList<Tuple<Tuple<Double, Integer>, Double>> results = calculateCompleteness(areaShape, sourceFolder, outputFolder);
		writeCompletenessOutput(results, outputFolder);
		
		Header.printFooter();
	}
	
	public static MultiPolygon readInShapefile(String shapefilePath) {


		MyMultiFeatureReader mmfr = new MyMultiFeatureReader();
		try {
			mmfr.readMultizoneShapefile(shapefilePath, 1);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read shapefile from " + shapefilePath);

		}
		List<MyZone> listZones = mmfr.getAllZones();
		
		if(listZones.size() > 1){
			LOG.warn("There are multiple shapefiles in " + shapefilePath);
			LOG.warn("Only the first will be used as the geographic area.");
		}
		MultiPolygon area = listZones.get(0);
		
		return area;
	}
	
	public static boolean checkActivityInArea(Geometry area, DigicoreActivity activity) {
		
		boolean inArea = false;
		Geometry boundingBox = area.getEnvelope();
		Coord activityCoord = activity.getCoord();
		
		//Convert the matsim coord to a vividsolutions point
		double x = activityCoord.getX();
		double y = activityCoord.getY();
		Coordinate activityCoordinate = new Coordinate(x, y);
		GeometryFactory gf = new GeometryFactory();
		Point activityPoint = gf.createPoint(activityCoordinate);
		
		//Check if the activity lies in the envelope of NMBM
		//If it does, check if it lies in NMBM
		//Change the boolean inArea accordingly
		if (boundingBox.covers(activityPoint)) {
//			LOG.info("Activity lies in the envelope of NMBM.");
			if (area.covers(activityPoint)) {
//				LOG.info("Activity lies in NMBM and should be included in the analysis.");
				inArea = true;
			} else {
//				LOG.info("Activity lies in envelope but outside of NMBM.");
			} 
		} 
//		else LOG.info("Activity not in envelope.");
		
			
		return inArea;
	}
	
	public static Collection<Id<ActivityFacility>> getFacilityIds(String networkPath) {
		DigicoreNetworkParser dnr = new DigicoreNetworkParser();
		DigicoreNetwork dn;
		Collection<Id<ActivityFacility>> facilityIdList = null;
		try {
			dn = dnr.parseNetwork(networkPath);
			facilityIdList = dn.getVertices();
		} catch (IOException e) {
			LOG.info("Cannot parse network!");
			e.printStackTrace();
		}
		
		return facilityIdList;
	}
	
	public static ArrayList<Tuple<Tuple<Double, Integer>, Double>> calculateCompleteness(Geometry area, String sourceFolder, String outputFolder) {
		
		ArrayList<Tuple<Tuple<Double,Integer>, Double>> completenessResults = new ArrayList<Tuple<Tuple<Double,Integer>, Double>>();
		double completeness = 1000.0; //initialise to a large value 
//		boolean activityInList = false;
//		Run for all combinations once code has been tested and is confirmed to work
		double[] radii = {1, 5, 10, 15, 20, 25, 30, 35, 40};
		int[] pmins = {1, 5, 10, 15, 20, 25};
		
		//For testing purposes
//		double[] radii = {1};
//		int[] pmins = {1, 5, 10, 15, 20, 25};
//		
		DigicoreVehicleReader dvr = new DigicoreVehicleReader();
		for(double thisRadius : radii){
			for(int thisPmin : pmins){
				LOG.info("===============================================");
				LOG.info("Starting analysis for radius " + thisRadius + " and pmin " + thisPmin);
				LOG.info("===============================================");
				int in = 0;
				int total = 0;

				//Get the network and list of facilityIds
				String networkPath = String.format("%s/%.0f_%d/%.0f_%d_network.txt", sourceFolder, thisRadius, thisPmin, thisRadius, thisPmin);
				Collection<Id<ActivityFacility>> facilityIdList = getFacilityIds(networkPath);
				//Get the vehicles
				String vehicleFolder = String.format("%s/%.0f_%d/xml2/", sourceFolder, thisRadius, thisPmin);
				List<File> listOfFiles = FileUtils.sampleFiles(new File(vehicleFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));

				for (File file : listOfFiles) {
					String vehicle = file.toString();
					dvr.parse(vehicle);
					DigicoreVehicle dv = dvr.getVehicle();
					List<DigicoreChain> digicoreChains = dv.getChains();
					for (DigicoreChain digicoreChain : digicoreChains) {
						List<DigicoreActivity> digicoreActivities = digicoreChain.getAllActivities();
//						LOG.info("There are " + digicoreActivities.size() + " activities in this chain number" + digicoreChains.indexOf(digicoreChain));
						for (DigicoreActivity digicoreActivity : digicoreActivities) {
							boolean inArea = checkActivityInArea(area, digicoreActivity);
							if (inArea == true) {
								if (facilityIdList.contains(digicoreActivity.getFacilityId())) {
									in++;
									LOG.info("IN counter: " + in);
								} 
								total++;
								LOG.info("TOTAL counter: " + total);
							} 
						} 
						
					}
				
				}
				LOG.info("IN is " + in);
				LOG.info("TOTAL is " + total);
				completeness = (double) in/total;
				LOG.info("Completeness for radius " + thisRadius + " and " + thisPmin + " is " + completeness);
				Tuple<Double, Integer> combinationTuple = new Tuple<Double, Integer>(thisRadius, thisPmin);
				Tuple<Tuple<Double, Integer>, Double> completenessTuple = new Tuple<Tuple<Double, Integer>, Double>(combinationTuple, completeness);
				completenessResults.add(completenessTuple);
			}
			
		}
	
		return completenessResults;
		
	}
	
	public static void  writeCompletenessOutput(ArrayList<Tuple<Tuple<Double, Integer>, Double>> results, String outputFolder) {
		
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFolder);
		try{
			bw.write("radius,pmin,completeness");
			bw.newLine();
			for (Tuple<Tuple<Double, Integer>, Double> entry : results) {
				bw.write(entry.getFirst().getFirst().toString());
				bw.write(",");
				bw.write(entry.getFirst().getSecond().toString());
				bw.write(",");
				bw.write(entry.getSecond().toString());
				bw.newLine();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFolder);
		}  finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFolder);
			}
		}
	}
	

}

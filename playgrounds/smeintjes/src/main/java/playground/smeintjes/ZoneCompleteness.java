package playground.smeintjes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.projects.complexNetworks.analysis.CheckActivityPercentages;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * This class calculates the percentage of Digicore activities associated with 
 * facilities for all clustering configurations in the 10 hexagonal study areas in NMBM.
 * The completeness for each zone will be used for Serina de Smedt's final year
 * project.
 * 
 * @author sumarie
 *
 */
public class ZoneCompleteness {
	final private static Logger LOG = Logger.getLogger(CheckActivityPercentages.class); 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ZoneCompleteness.class.toString(), args);
		
		String sourceFolder = args[0];
		int numberOfThreads = Integer.parseInt(args[1]);
		Double hexWidth = Double.parseDouble(args[2]);
		
		Id zone1 = new IdImpl(1);
		Id zone2 = new IdImpl(2);
		Id zone3 = new IdImpl(3);
		Id zone4 = new IdImpl(4);
		Id zone5 = new IdImpl(5);
		Id zone6 = new IdImpl(6);
		Id zone7 = new IdImpl(7);
		Id zone8 = new IdImpl(8);
		Id zone9 = new IdImpl(9);
		Id zone10 = new IdImpl(10);
		Id[] idList = {zone1,zone2,zone3,zone4,zone5,zone6,zone7,zone8,zone9,zone10};
		
		
		QuadTree<Tuple<Coord, Tuple<Id, Polygon>>> qt = buildZoneQuadTree(idList);
		getZoneCompleteness(numberOfThreads, sourceFolder, qt, hexWidth, idList);
				
		Header.printFooter();

	}
	
	/*
	 * This method creates a QuadTree that includes the hexagons of the 10 study areas (in NMBM)  
	 * as Polygons.
	 */
	private static QuadTree<Tuple<Coord,Tuple<Id, Polygon>>> buildZoneQuadTree(Id[] idList) {
		
		double minX = 130000.0;
		double minY = -3707000.0;
		double maxX = 152000.0;
		double maxY = -3684000.0;
				
		QuadTree<Tuple<Coord,Tuple<Id, Polygon>>> zoneQT = new QuadTree<Tuple<Coord,Tuple<Id, Polygon>>>(minX, minY, maxX, maxY);
		
		Coord centroid1 = new CoordImpl(130048.2549,-3685018.8482);
		Coord centroid2 = new CoordImpl(148048.2549,-3702339.3562);
		Coord centroid3 = new CoordImpl(148798.2549,-3704504.4197);
		Coord centroid4 = new CoordImpl(149548.2549,-3706669.4833);
		Coord centroid5 = new CoordImpl(151048.2549,-3706669.4833);
		Coord centroid6 = new CoordImpl(148048.2549,-3701473.3308);
		Coord centroid7 = new CoordImpl(146548.2549,-3697143.2038);
		Coord centroid8 = new CoordImpl(146548.2549,-3704937.4325);
		Coord centroid9 = new CoordImpl(148048.2549,-3705803.4579);
		Coord centroid10 = new CoordImpl(130048.2549,-3684152.8228);
		Coord[] centroidList = {centroid1,centroid2,centroid3,centroid4,centroid5,centroid6,centroid7,centroid8,centroid9,centroid10};
						
		/* Set up distances for hexagon and create each zone's hexagon */
		GeometryFactory gf = new GeometryFactory();
		double conversion_x = 0.01074886; //Make sure these conversions are correct (taken from plotGtiStudyAreas.R)
		double conversion_y = 0.00905810;
		double width = 0.5*conversion_x;
		double height = Math.sqrt(3)/2 * width * (conversion_y/conversion_x);
		int counter = 0;
		for (Coord coord : centroidList) {
			
			double x = coord.getX(); //does it matter that I work with MATSim Coords and 
			double y = coord.getY(); //vividsolution's Coordinates? Probably...
			/*Create Coordinate[] that contains the Coordinates of the hexagon's vertices*/
			Coordinate[] coordinates = new Coordinate[]{new Coordinate(x-width, y), 
					new Coordinate(x-0.5*width, y+height), 
					new Coordinate(x+0.5*width, y+height),
					new Coordinate(x+width, y), 
					new Coordinate(x+0.5*width, y-height), 
					new Coordinate(x-0.5*width, y-height), 
					new Coordinate(x-width, y)};
			CoordinateSequence coordinateSeq = gf.getCoordinateSequenceFactory().create(coordinates);
			LinearRing hexRing = new LinearRing(coordinateSeq, gf);
			Polygon hex = gf.createPolygon(hexRing, null); 
//			zoneQT.put(x, y, new Tuple<Coord, Polygon>(coord,hex)); //Probably better to have a QuadTree<Tuple<Id,Polygon>>
			Tuple<Id, Polygon> zoneTuple = new Tuple<Id, Polygon>(idList[counter],hex);
			Coord zoneCoord = new CoordImpl(x,y);
			zoneQT.put(x, y, new Tuple<Coord,Tuple<Id, Polygon>>(zoneCoord,zoneTuple));
			counter++;
		}
		
		return zoneQT;
	}
	
	public static void getZoneCompleteness(int numberOfThreads, String sourceFolder, QuadTree<Tuple<Coord,Tuple<Id, Polygon>>> qt, double hexWidth, Id[] idList) {
		
		
//		double[] radii = {1, 5, 10, 15, 20, 25, 30, 35, 40};
//		int[] pmins = {1, 5, 10, 15, 20, 25};
		
		//For testing purposes
		double[] radii = {10};
		int[] pmins = {15};
		
		/*
		 * For each zone, get a list of DigicoreActivities and put all in a 
		 * Map<Id,List<DigicoreActivity>>
		 */
		
			for(double thisRadius : radii){
				for(int thisPmin : pmins){
					/* Set configuration-specific filenames */
					String vehicleFolder = String.format("%s/%.0f_%d/xml2/", sourceFolder, thisRadius, thisPmin);
					
					String outputFile = String.format("%s%.0f_%d/%.0f_%d_zonePercentageActivities.csv", sourceFolder, thisRadius, thisPmin, thisRadius, thisPmin);
					for(Id thisZone : idList){
					LOG.info("================================================================================");
					LOG.info("Performing percentage-facility-id analysis for radius " + thisRadius + ", and pmin of " + thisPmin + ", in zone " + thisZone);
					LOG.info("================================================================================");
										
					/* Get all the files */
					List<File> listOfFiles = FileUtils.sampleFiles(new File(vehicleFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
					
					Counter counter = new Counter("   vehicles # ");
					
					/* Set up the multi-threaded infrastructure. */
					LOG.info("Setting up multi-threaded infrastructure");
					ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
					List<Future<Map<Id,List<DigicoreActivity>>>> jobs = new ArrayList<Future<Map<Id,List<DigicoreActivity>>>>();
					
					LOG.info("Processing the vehicle files...");
					for(File file : listOfFiles){
						Callable<Map<Id,List<DigicoreActivity>>> job = new ExtractorCallable(qt, file, counter, hexWidth);
						Future<Map<Id,List<DigicoreActivity>>> result = threadExecutor.submit(job);
						jobs.add(result);
					}
					counter.printCounter();
					LOG.info("Done processing vehicle files...");
					
					threadExecutor.shutdown();
					while(!threadExecutor.isTerminated()){//}
					}

					/* Consolidate the output */
					LOG.info("Consolidating output...");
					
					Map<Id,Double> zoneCompleteness = new TreeMap<Id,Double>();
					BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
					LOG.info("Calculating zone completeness...");
						
						try {
							bw.write("radius,pmin,zone,completeness");
							bw.newLine();
							for (Future<Map<Id, List<DigicoreActivity>>> job : jobs) {
								Map<Id, List<DigicoreActivity>> result = job.get();
								if(result != null){
									LOG.info("Got job. Size of map: " + result.size());
								}
								for (Id zone : result.keySet()) {
									int activitiesWithId = 0;
									int totalActivities = 0;
									LOG.info("Getting activities in zone " + zone );
										for (List<DigicoreActivity> activity : result.values()) {
											if(activity.get(totalActivities).getFacilityId() != null){
												activitiesWithId++;
												LOG.info("Number of activities with facilityIDs: " + activitiesWithId);
											}
											totalActivities++;
											LOG.info("Total number of activities: " +totalActivities);
										}
								double completeness = ((double) activitiesWithId)/((double) totalActivities);
								zoneCompleteness.put(zone, completeness);
								LOG.info("Writing zone completeness...");
								bw.write(String.format("%.2f,%d,%d,%.2f\n", thisRadius, thisPmin, zone, zoneCompleteness.get(zone)));
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
							throw new RuntimeException("Cannot write to " + outputFile);
						} catch (InterruptedException e) {
							e.printStackTrace();
							throw new RuntimeException("Couldn't get thread job result.");
						} catch (ExecutionException e) {
							e.printStackTrace();
							throw new RuntimeException("Couldn't get thread job result.");
						}  finally{
							try {
								bw.close();
							} catch (IOException e) {
								e.printStackTrace();
								throw new RuntimeException("Cannot close " + outputFile);
							}
						}
						counter.printCounter();
					}
			}
		}
		
	}
	
	/* 
	 * Multi-threaded analysis for extracting Digicore activities from activity chains. Each thread is
	 * passed a vehicle file, which is parsed and analysed to determine whether the activities fall within 
	 * one of the ten study areas.
	 */
	private static class ExtractorCallable implements Callable<Map<Id,List<DigicoreActivity>>>{
		private final QuadTree<Tuple<Coord,Tuple<Id, Polygon>>> qt;
		private final File file;
		public Counter counter;
		private final double width;
		
		
		public ExtractorCallable(QuadTree<Tuple<Coord,Tuple<Id, Polygon>>> qt, File file, Counter counter, double width) {
			this.qt = qt;
			this.file = file;
			this.counter = counter;
			this.width = width;
			
		}

		@Override
		public Map<Id,List<DigicoreActivity>> call() throws Exception {
			
			Map<Id,List<DigicoreActivity>> activityMap = new TreeMap<Id,List<DigicoreActivity>>();
//			List<DigicoreActivity> list = new ArrayList<DigicoreActivity>(); //Don't think I need this list
			GeometryFactory gf = new GeometryFactory();
			/* Parse the vehicle from file. */
			DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
			dvr.parse(file.getAbsolutePath());
			DigicoreVehicle vehicle = dvr.getVehicle();

			/* Check how far EACH activity is. If it is within the threshold,
			 * then put it in the activityMap for further completeness analysis. */
			for(DigicoreChain chain : vehicle.getChains()){
				for(DigicoreActivity act : chain.getAllActivities()){
					Coord activityCoord = act.getCoord();
					double x = activityCoord.getX();
					double y = activityCoord.getY();
					/*Need to create a CoordinateSequence with a Coordinate, since it is required to
					 * create a Point feature with */
					Coordinate coordinate = new Coordinate(x,y);
					CoordinateSequence activityCoordinateSeq = gf.getCoordinateSequenceFactory().create(new Coordinate[]{coordinate});
					Tuple<Coord,Tuple<Id, Polygon>> closest = qt.get(activityCoord.getX(), activityCoord.getY());
					double dist = CoordUtils.calcDistance(activityCoord, closest.getFirst());
					/*First do a a quick check with a distance*/
					if(dist <= this.width){
						/*Do a proper check to see whether activity lies within hexagon boundary*/
						if(closest.getSecond().getSecond().covers(new Point(activityCoordinateSeq,gf))){
//						list.add(act);
							if(!activityMap.containsKey(closest.getSecond().getFirst())){
								activityMap.put(closest.getSecond().getFirst(), new ArrayList<DigicoreActivity>());
							} 
								List<DigicoreActivity> list = activityMap.get(closest.getSecond().getFirst());
								list.add(act);
								activityMap.put(closest.getSecond().getFirst(), list);	
								LOG.info("Added activity to map. Size of map: " + activityMap.size());
								
						}
					}
					
				}
			}

			counter.incCounter();
			return activityMap;
		}
	}

}

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
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
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
	final private static Logger LOG = Logger.getLogger(ZoneCompleteness.class); 
	/**
	 * @param sourceFolder the path to the folder containing the network files 
	 * @param outputFolder the path to where the output should be written
	 * @param numberOfThreads the number of threads to use for the multi-threading part
	 * @param hexWidth the width of the hexagonal study areas
	 */
	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {
		Header.printHeader(ZoneCompleteness.class.toString(), args);

		String sourceFolder = args[0];
		String outputFolder =args[1];
		int numberOfThreads = Integer.parseInt(args[2]);
		Double hexWidth = Double.parseDouble(args[3]);

		Id<Polygon> zone1 = Id.create(1, Polygon.class);
		Id<Polygon> zone2 = Id.create(2, Polygon.class);
		Id<Polygon> zone3 = Id.create(3, Polygon.class);
		Id<Polygon> zone4 = Id.create(4, Polygon.class);
		Id<Polygon> zone5 = Id.create(5, Polygon.class);
		Id<Polygon> zone6 = Id.create(6, Polygon.class);
		Id<Polygon> zone7 = Id.create(7, Polygon.class);
		Id<Polygon> zone8 = Id.create(8, Polygon.class);
		Id<Polygon> zone9 = Id.create(9, Polygon.class);
		Id<Polygon> zone10 = Id.create(10, Polygon.class);
		Id<Polygon>[] idList = new Id[] {zone1,zone2,zone3,zone4,zone5,zone6,zone7,zone8,zone9,zone10};


		QuadTree<Tuple<Id<Polygon>, Polygon>> qt = buildZoneQuadTree(idList, hexWidth);
		getZoneCompleteness(numberOfThreads, sourceFolder, outputFolder, qt, hexWidth, idList);

		Header.printFooter();

	}

	/*
	 * This method creates a QuadTree that includes the hexagons of the 10 study areas (in NMBM)  
	 * as Polygons.
	 */
	private static QuadTree<Tuple<Id<Polygon>, Polygon>> buildZoneQuadTree(Id<Polygon>[] idList, double totalWidth) {

		double minX = 130000.0;
		double minY = -3707000.0;
		double maxX = 152000.0;
		double maxY = -3684000.0;

		QuadTree<Tuple<Id<Polygon>, Polygon>> zoneQT = new QuadTree<Tuple<Id<Polygon>, Polygon>>(minX, minY, maxX, maxY);

		final double y10 = -3685018.8482;
		Coord centroid1 = new Coord(130048.2549, y10);
		final double y9 = -3702339.3562;
		Coord centroid2 = new Coord(148048.2549, y9);
		final double y8 = -3704504.4197;
		Coord centroid3 = new Coord(148798.2549, y8);
		final double y7 = -3706669.4833;
		Coord centroid4 = new Coord(149548.2549, y7);
		final double y6 = -3706669.4833;
		Coord centroid5 = new Coord(151048.2549, y6);
		final double y5 = -3701473.3308;
		Coord centroid6 = new Coord(148048.2549, y5);
		final double y4 = -3697143.2038;
		Coord centroid7 = new Coord(146548.2549, y4);
		final double y3 = -3704937.4325;
		Coord centroid8 = new Coord(146548.2549, y3);
		final double y2 = -3705803.4579;
		Coord centroid9 = new Coord(148048.2549, y2);
		final double y1 = -3684152.8228;
		Coord centroid10 = new Coord(130048.2549, y1);
		Coord[] centroidList = {centroid1,centroid2,centroid3,centroid4,centroid5,centroid6,centroid7,centroid8,centroid9,centroid10};

		/* Set up distances for hexagon and create each zone's hexagon */
		GeometryFactory gf = new GeometryFactory();
		double width = 0.5*totalWidth;
		double height = Math.sqrt(3)/2 * width;
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

			Tuple<Id<Polygon>, Polygon> zoneTuple = new Tuple<Id<Polygon>, Polygon>(idList[counter],hex);
			zoneQT.put(x, y, zoneTuple);
			counter++;
		}

		return zoneQT;
	}

	public static void getZoneCompleteness(int numberOfThreads, String sourceFolder, String outputFolder, QuadTree<Tuple<Id<Polygon>, Polygon>> qt, double hexWidth, Id<Polygon>[] idList) {


				double[] radii = {20};
				int[] pmins = {25};

		//For testing purposes
//		double[] radii = {15};
//		int[] pmins = {15};

		/*
		 * For each zone, get a list of DigicoreActivities and put all in a 
		 * Map<Id,List<DigicoreActivity>>
		 */

		for(double thisRadius : radii){
			for(int thisPmin : pmins){
				/* Set configuration-specific filenames */
				String vehicleFolder = String.format("%s/%.0f_%d/xml2/", sourceFolder, thisRadius, thisPmin);
				String outputFile = String.format("%s/%.0f_%d_zonePercentageActivities.csv", outputFolder, thisRadius, thisPmin);

				LOG.info("================================================================================");
				LOG.info("Performing percentage-facility-id analysis for radius " + thisRadius + ", and pmin of " + thisPmin);
				LOG.info("================================================================================");

				/* Get all the files */
				List<File> listOfFiles = FileUtils.sampleFiles(new File(vehicleFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));

				Counter counter = new Counter("   vehicles # ");

				/* Set up the multi-threaded infrastructure. */
				LOG.info("Setting up multi-threaded infrastructure");
				ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
				List<Future<Map<Id<Polygon>, Tuple<Integer, Integer>>>> jobs = new ArrayList<Future<Map<Id<Polygon>, Tuple<Integer, Integer>>>>();

				LOG.info("Processing the vehicle files...");
				for(File file : listOfFiles){
					Callable<Map<Id<Polygon>, Tuple<Integer, Integer>>> job = new ExtractorCallable(qt, file, counter);
					Future<Map<Id<Polygon>, Tuple<Integer, Integer>>> result = threadExecutor.submit(job);
					jobs.add(result);
				}
				counter.printCounter();
				LOG.info("Done processing vehicle files...");

				threadExecutor.shutdown();
				while(!threadExecutor.isTerminated()){
				}
				counter.printCounter();

				/* Consolidate the output */
				LOG.info("Consolidating output...");
				Map<Id<Polygon>, Tuple<Integer, Integer>> consolidatedMap = new TreeMap<Id<Polygon>, Tuple<Integer,Integer>>();

				try{
					for(Future<Map<Id<Polygon>, Tuple<Integer, Integer>>> job : jobs){
						Map<Id<Polygon>, Tuple<Integer, Integer>> thisJob = job.get();
						for(Id<Polygon> id : thisJob.keySet()){
							if(!consolidatedMap.containsKey(id)){
								consolidatedMap.put(id, thisJob.get(id));
							} else{
								int oldTotal = consolidatedMap.get(id).getFirst();
								int oldHasId = consolidatedMap.get(id).getSecond();

								int thisTotal = thisJob.get(id).getFirst();
								int thisHasId = thisJob.get(id).getSecond();

								Tuple<Integer, Integer> newTuple = new Tuple<Integer, Integer>(oldTotal + thisTotal, oldHasId + thisHasId);
								consolidatedMap.put(id, newTuple);
							}
						}
					}
				} catch (ExecutionException e) {
					e.printStackTrace();
					throw new RuntimeException("Couldn't get thread job result.");
				} catch (InterruptedException e) {
					e.printStackTrace();
					throw new RuntimeException("Couldn't get thread job result.");
				}

				/* Write the output to file. */
				BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
				try{
					bw.write("ZoneId,Total,NumberWithFacilityId");
					bw.newLine();

					for(Id<Polygon> id: consolidatedMap.keySet()){
						bw.write(id.toString());
						bw.write(",");
						bw.write(String.valueOf(consolidatedMap.get(id).getFirst()));
						bw.write(",");
						bw.write(String.valueOf(consolidatedMap.get(id).getSecond()));
						bw.newLine();
					}
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot write to " + outputFile);
				}  finally{
					try {
						bw.close();
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("Cannot close " + outputFile);
					}
				}

			}
		}
	}


	/* 
	 * Multi-threaded analysis for extracting Digicore activities from activity chains. Each thread is
	 * passed a vehicle file, which is parsed and analysed to determine whether the activities fall within 
	 * one of the ten study areas.
	 */
	@SuppressWarnings("rawtypes")
	private static class ExtractorCallable implements Callable<Map<Id<Polygon>, Tuple<Integer, Integer>>>{
		private final QuadTree<Tuple<Id<Polygon>, Polygon>> qt;
		private final File file;
		public Counter counter;


		public ExtractorCallable(QuadTree<Tuple<Id<Polygon>, Polygon>> qt, File file, Counter counter) {
			this.qt = qt;
			this.file = file;
			this.counter = counter;
		}


		@Override
		public Map<Id<Polygon>, Tuple<Integer, Integer>> call() throws Exception {
			Map<Id<Polygon>, Tuple<Integer, Integer>> zoneMap = new TreeMap<Id<Polygon>, Tuple<Integer, Integer>>();
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
					/* Need to create a CoordinateSequence with a Coordinate, since it is required to
					 * create a Point feature with */
					Coordinate coordinate = new Coordinate(x,y);
					Point point = gf.createPoint(coordinate);

					Id closestPolygonId = qt.get(x, y).getFirst();
					Polygon closestPolygon = qt.get(x, y).getSecond();

					/* Check if activity is in closest polygon. */
					if(closestPolygon.covers(point)){
						if(!zoneMap.containsKey(closestPolygonId)){
							Tuple<Integer, Integer> tuple = new Tuple<Integer, Integer>(0, 0);
							zoneMap.put(closestPolygonId, tuple);
						}

						int oldTotal = zoneMap.get(closestPolygonId).getFirst();
						int oldHasId = zoneMap.get(closestPolygonId).getSecond();
						int newTotal = oldTotal + 1;
						int newHasId = oldHasId;


						/* Check if the activity has a facility Id. */
						if(act.getFacilityId() != null){
							newHasId = oldHasId + 1;
						}

						/* Put the updated tuple. */
						Tuple<Integer, Integer> newTuple = new Tuple<Integer, Integer>(newTotal, newHasId);
						zoneMap.put(closestPolygonId, newTuple);
					}
				}
			}

			counter.incCounter();
			return zoneMap;
		}
	}

}

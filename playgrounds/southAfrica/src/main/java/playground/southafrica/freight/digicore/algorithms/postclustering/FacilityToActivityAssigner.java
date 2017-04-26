package playground.southafrica.freight.digicore.algorithms.postclustering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.southafrica.freight.digicore.algorithms.djcluster.DigicoreClusterRunner;
import playground.southafrica.freight.digicore.algorithms.djcluster.HullConverter;
import playground.southafrica.freight.digicore.containers.DigicoreFacility;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesWriter;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

public class FacilityToActivityAssigner {
	private static Logger log = Logger.getLogger(FacilityToActivityAssigner.class);
	private static long reconstructDuration;
	private static long treeBuildDuration;
	private static long writeToFileDuration;

	/**
	 * This class will read in a set of facilities, along with their attributes, 
	 * and then adapt given vehicles' activity chains. If any activity in the
	 * chain occurs at a read facility - that is, it falls within the facility's 
	 * bounding polygon (concave hull), the activity is associated with that 
	 * facility. Consecutive activities belonging to the same cluster/facility 
	 * will, in this revised version, <b><i>NOT</i></b> be merged. The new 
	 * chains will be written out to new XML files.
	 * 
	 * <h4>Note:</h4>
	 * This class supersedes the original {@link ClusteredChainGenerator}.
	 *
	 * @param args
	 * <ul>
	 * <li> args[0] = the absolute path of the input {@link DigicoreVehicles} 
	 * 				  container;
	 * <li> args[1] = the absolute path of the facilities file that was created
	 * 				  by the {@link DigicoreClusterRunner} class;
	 * <li> args[2] = the absolute path of the facility attributes file that was 
	 * 				  created by the {@link DigicoreClusterRunner} class;
	 * <li> args[3] = the number of threads to use in the multithreaded parts
	 * <li> args[4] = the absolute path of the shapefile of the study area. 
	 * 				  only vehicles with at least one activity inside the area
	 * 				  will be written out to the xml folder. NOTE: It is, to my
	 * 				  current knowledge (JWJ, Aug 2013), NECESSARY to use the 
	 * 				  shapefile of the entire area, and <i><b>not</b></i> a 
	 * 				  smaller demarcation shapefile, for example the GAP zones.
	 * <li> args[5] = the ID field for the shapefile.
	 * <li> args[6] = the absolute path of the output {@link DigicoreVehicles} 
	 * 				  container;
	 * </ul>
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Header.printHeader(FacilityToActivityAssigner.class.toString(), args);
		long startTime = System.currentTimeMillis();

		String inputVehicles = args[0];
		String inputFacilityFile = args[1];
		String inputFacilityAttributeFile = args[2];
		int nThreads = Integer.parseInt(args[3]);
		String shapefile = args[4];
		int idField = Integer.parseInt(args[5]);
		String outputVehicles = args[6];
		
		

		/* Read the study area from shapefile. This is necessary as we
		 * only want to retain xml files of vehicles that performed at
		 * least one activity in the study area. */
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		mfr.readMultizoneShapefile(shapefile, idField);
		List<MyZone> zones = mfr.getAllZones();
		if(zones.size() > 1){
			log.warn("The read shapefile contains multiple zones. Only the first will be used as study area.");
		}
		Geometry studyArea = zones.get(0);


		/* Read facility attributes. */
		ObjectAttributes oa = new ObjectAttributes();
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(oa);
		oar.putAttributeConverter(Point.class, new HullConverter());
		oar.putAttributeConverter(LineString.class, new HullConverter());
		oar.putAttributeConverter(Polygon.class, new HullConverter());
		oar.readFile(inputFacilityAttributeFile);

		FacilityToActivityAssigner ccg = new FacilityToActivityAssigner();

		/* Build facility QuadTree. */
		QuadTree<DigicoreFacility> facilityTree = ccg.buildFacilityQuadTree(inputFacilityFile, inputFacilityAttributeFile);

		/* Run through vehicle files to reconstruct the chains */
		DigicoreVehicles newVehicles = ccg.reconstructChains(facilityTree, oa, inputVehicles, nThreads, studyArea);
		new DigicoreVehiclesWriter(newVehicles).write(outputVehicles);

		long duration = System.currentTimeMillis() - startTime;
		log.info("	 Tree build time (s): " + treeBuildDuration/1000);
		log.info("	Reconstruct time (s): " + reconstructDuration/1000);
		log.info("Write to file time (s): " + writeToFileDuration/1000);
		log.info("	  Total run time (s): " + duration/1000);

		Header.printFooter();
	}

	/**
	 * This method takes each vehicle file and reconstructs the chains.
	 * 
	 * @param facilityTree {@link QuadTree} of {@link DigicoreFacility}s built 
	 * 		  with the {@link #buildFacilityQuadTree(String, String)} method. 
	 * @param inputFolder original vehicles file location;
	 * @param outputFolder adapted vehicle file location;
	 * @param nThreads number of threads to use.
	 * @return {@link ConcurrentHashMap}
	 * @throws IOException
	 */

	public DigicoreVehicles reconstructChains(
			QuadTree<DigicoreFacility> facilityTree, ObjectAttributes facilityAttributes, 
			String inputVehicles, int nThreads, Geometry studyArea) throws IOException {
		long startTime = System.currentTimeMillis();

		/* Read the input vehicles container. */
		DigicoreVehicles dvs = new DigicoreVehicles();
		new DigicoreVehiclesReader(dvs).readFile(inputVehicles);

		/* Execute the multi-threaded jobs. */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(nThreads);
		Counter threadCounter = new Counter("   vehicles completed: ");
		List<Future<DigicoreVehicle>> listOfJobs = new ArrayList<>(dvs.getVehicles().size());
		
		for(DigicoreVehicle vehicle : dvs.getVehicles().values()){
			Callable<DigicoreVehicle> job = new CallableChainReconstructor(vehicle, facilityTree, facilityAttributes, threadCounter, studyArea);
			Future<DigicoreVehicle> submit = threadExecutor.submit(job);
			listOfJobs.add(submit);
		}

		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		threadCounter.printCounter();
		log.info("  chains reconstructed.");

		reconstructDuration = System.currentTimeMillis() - startTime;
		
		/* Add all vehicles to the new vehicles container. */
		log.info("Adding all vehicles from multi-threaded run...");
		DigicoreVehicles newVehicles = new DigicoreVehicles(dvs.getCoordinateReferenceSystem());
		String oldDescription = dvs.getDescription();
		oldDescription += oldDescription.endsWith(".") ? " " : ". ";
		oldDescription += "Facility Ids added.";
		
		for(Future<DigicoreVehicle> future : listOfJobs){
			try {
				DigicoreVehicle vehicle = future.get();
				if(vehicle != null){
					newVehicles.addDigicoreVehicle(vehicle);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot add vehicle during multithreaded consolidation.");
			} catch (ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot add vehicle during multithreaded consolidation.");
			}
		}
		log.info("Done adding all the vehicles.");
		return newVehicles;
	}
	

	/**
	 * This method reads a MATSim facilities file, as well as the facilities'
	 * associated {@link ObjectAttributes} and builds and returns a 
	 * {@link QuadTree} of {@link DigicoreFacility}s.
	 * 
	 * @param facilityFile
	 * @throws IOException
	 */
	public QuadTree<DigicoreFacility> buildFacilityQuadTree(String facilityFile, String facilityAttributeFile) throws IOException {
		long startTime = System.currentTimeMillis();
		log.info("Building QuadTree of facilities...");

		/* Read facilities. */
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(sc);
		fr.readFile(facilityFile);

		/* Convert each MATSim facility to a specific DigicoreFacility. */
		List<DigicoreFacility> facilityList = new ArrayList<DigicoreFacility>();
		for(Id<ActivityFacility> id : sc.getActivityFacilities().getFacilities().keySet()){
			ActivityFacility af = sc.getActivityFacilities().getFacilities().get(id); 

			DigicoreFacility df = new DigicoreFacility(id);
			df.setCoord(af.getCoord());
			facilityList.add(df);
		}
		log.info("  " + facilityList.size() + " facilities were identified");

		/* Determine QuadTree extent. */
		double xMin = Double.MAX_VALUE;
		double yMin = Double.MAX_VALUE;
		double xMax = Double.MIN_VALUE;
		double yMax = Double.MIN_VALUE;

		for(DigicoreFacility df : facilityList){
			xMin = Math.min(xMin, df.getCoord().getX());
			xMax = Math.max(xMax, df.getCoord().getX());
			yMin = Math.min(yMin, df.getCoord().getY());
			yMax = Math.max(yMax, df.getCoord().getY());
		}

		QuadTree<DigicoreFacility> facilityTree = new QuadTree<DigicoreFacility>(xMin,yMin,xMax,yMax);

		/* Populate the QuadTree with the Digicore facilities. */
		for(DigicoreFacility df : facilityList){
			facilityTree.put(df.getCoord().getX(), df.getCoord().getY(), df);
		}

		treeBuildDuration = System.currentTimeMillis() - startTime;
		log.info(" QuadTree built with " + facilityTree.size() + " entries.");

		return facilityTree;
	}

	/* Default constructor */
	public FacilityToActivityAssigner() {

	}

}

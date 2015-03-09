package playground.southafrica.freight.digicore.analysis.postClustering;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.freight.digicore.algorithms.djcluster.HullConverter;
import playground.southafrica.freight.digicore.containers.DigicoreFacility;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ClusteredChainGenerator {
	private static Logger log = Logger.getLogger(ClusteredChainGenerator.class);
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
	 * @param args
	 * <ul>
	 * <li> args[0] = the absolute path of the folder containing the original 
	 *                .xml.gz vehicle files;
	 * <li> args[1] = the absolute path of the folder containing the different 
	 * 				  clustering configuration output folders;
	 * <li> args[2] = the number of threads to use in the multithreaded parts
	 * <li> args[3] = the absolute path of the shapefile of the study area. 
	 * 				  only vehicles with at least one activity inside the area
	 * 				  will be written out to the xml folder. NOTE: It is, to my
	 * 				  current knowledge (JWJ, Aug 2013), NECESSARY to use the 
	 * 				  shapefile of the entire area, and <i><b>not</b></i> a 
	 * 				  smaller demarcation shapefile, for example the GAP sones.
	 * <li> args[4] = the ID field for the shapefile.
	 * </ul>
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Header.printHeader(ClusteredChainGenerator.class.toString(), args);
		long startTime = System.currentTimeMillis();
		
		String inputVehicleFolder = args[0];
		String inputFacilityFolder = args[1];
		int nThreads = Integer.parseInt(args[2]);
		String shapefile = args[3];
		int idField = Integer.parseInt(args[4]);
		
		/* These values should be set following Quintin's Design-of-Experiment inputs. */
		double[] radii = {15}; //, 10, 15, 20, 25, 30, 35, 40};
		int[] pmins = {15}; //, 10, 15, 20, 25};

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
		
		for(double thisRadius : radii){
			for(int thisPmin : pmins){
				/* Just write some indication to the log file as to what we're 
				 * busy with at this point in time. */
				log.info("================================================================================");
				log.info("Executing chain modification for radius " + thisRadius + ", and pmin of " + thisPmin);
				log.info("================================================================================");

				/* Set configuration-specific filenames, */
				String facilityFile = String.format("%s%.0f_%d/%.0f_%d_facilities.xml.gz", inputFacilityFolder, thisRadius, thisPmin, thisRadius, thisPmin);
				String facilityAttributeFile = String.format("%s%.0f_%d/%.0f_%d_facilityAttributes.xml.gz", inputFacilityFolder, thisRadius, thisPmin, thisRadius, thisPmin);
				String xml2Folder = String.format("%s%.0f_%d/xml2/", inputFacilityFolder, thisRadius, thisPmin);
				
				ClusteredChainGenerator ccg = new ClusteredChainGenerator();
				
				/* Read facility attributes. */
				ObjectAttributes oa = new ObjectAttributes();
				ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(oa);
				oar.putAttributeConverter(Point.class, new HullConverter());
				oar.putAttributeConverter(LineString.class, new HullConverter());
				oar.putAttributeConverter(Polygon.class, new HullConverter());
				oar.parse(facilityAttributeFile);
				
				/* Build facility QuadTree. */
				QuadTree<DigicoreFacility> facilityTree = ccg.buildFacilityQuadTree(facilityFile, facilityAttributeFile);
				
				/* Run through vehicle files to reconstruct the chains */
				ccg.reconstructChains(facilityTree, oa, inputVehicleFolder, xml2Folder, nThreads, studyArea);
			}
		}
		
		
		

		
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
	
	public void reconstructChains(
			QuadTree<DigicoreFacility> facilityTree, ObjectAttributes facilityAttributes, 
			String inputFolder, String outputFolder, int nThreads, Geometry studyArea) throws IOException {
		long startTime = System.currentTimeMillis();
		
		/* Check if the output folder exists, and delete if it does. */
		File folder = new File(outputFolder);
		if(folder.exists()){
			log.warn("The output folder exists and will be deleted.");
			log.warn("  --> " + folder.getAbsolutePath());
			FileUtils.delete(folder);
		}
		boolean created = folder.mkdirs();
		if(!created){
			log.error("Could not create the output folder " + folder.getAbsolutePath());
		}
		
		/* Read vehicle files. */
		List<File> vehicleList = FileUtils.sampleFiles(new File(inputFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));

		/* Execute the multi-threaded jobs. */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(nThreads);
		Counter threadCounter = new Counter("   vehicles completed: ");
		
		for(File vehicleFile : vehicleList){
			RunnableChainReconstructor rcr = new RunnableChainReconstructor(vehicleFile, facilityTree, facilityAttributes, threadCounter, outputFolder, studyArea);
			threadExecutor.execute(rcr);
		}
		
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		threadCounter.printCounter();
		log.info("  chains reconstructed.");
				
		reconstructDuration = System.currentTimeMillis() - startTime;
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
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(sc);
		fr.parse(facilityFile);
		
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
	public ClusteredChainGenerator() {
	
	}
	
}

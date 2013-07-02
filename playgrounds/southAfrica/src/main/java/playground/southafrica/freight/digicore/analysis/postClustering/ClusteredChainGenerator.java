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
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.freight.digicore.algorithms.djcluster.HullConverter;
import playground.southafrica.freight.digicore.containers.DigicoreFacility;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ClusteredChainGenerator {
	private static Logger log = Logger.getLogger(ClusteredChainGenerator.class);
	private static int nThreads;
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
	 * <li> args[1] = the absolute path of the file containing facilities;
	 * <li> args[2] = the absolute path of the file containing facility attributes.
	 *                This file is necessary as it contains the points of the 
	 *                concave hull describing the facility;
	 * <li> args[3] = the absolute path of the output folder where the new files 
	 *                should be written to.
	 * <li> args[4] = the number of threads to use in the multithreaded parts
	 * </ul>
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Header.printHeader(ClusteredChainGenerator.class.toString(), args);
		long startTime = System.currentTimeMillis();
		
		ClusteredChainGenerator ccg = new ClusteredChainGenerator();
		
		String inputFolder = args[0];
		String facilityFile = args[1];
		String facilityAttributeFile = args[2];
		String outputFolder = args[3];
		nThreads = Integer.parseInt(args[4]);
		
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
		ccg.reconstructChains(facilityTree, oa, inputFolder, outputFolder, nThreads);
		
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
			String inputFolder, String outputFolder, int nThreads) throws IOException {
		long startTime = System.currentTimeMillis();
		
		/* Read vehicle files. */
		List<File> vehicleList = FileUtils.sampleFiles(new File(inputFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));

		/* Execute the multi-threaded jobs. */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(nThreads);
		Counter threadCounter = new Counter("   Vehicles completed: ");
		
		for(File vehicleFile : vehicleList){
			RunnableChainReconstructor rcr = new RunnableChainReconstructor(vehicleFile, facilityTree, facilityAttributes, threadCounter, outputFolder);
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
		for(Id id : sc.getActivityFacilities().getFacilities().keySet()){
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

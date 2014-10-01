package playground.southafrica.freight.digicore.analysis.geographicClassifier;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * 
 * This class contains methods to do any of the following:
 * <ul>
 * <li> filter the *.xml.gz vehicle files and create a {@link List} of {@link File}s - this will read in all vehicle files in the specified folder.
 * <li> split the vehicles into INTER-, INTRA- and EXTRA-Provincial vehicles.
 * <li> read a file containing the IDs of vehicles and store it in a {@link List} of {@link File}s. E.g. you only want to read intra-provincial vehicles from the vehicle files provided in a directory. 
 * <li> write out the vehicle IDs in a {@link List} of {@link File}s to a .txt file.
 * </ul>
 * 
 * The main method will split vehicles into Intra-,Inter-, and Extra-provincial vehicles and write the IDs to a .txt file.
 * 
 * @author qvanheerden, jwjoubert
 *
 */

public class GeographicClassifier {
	private final static Logger LOG = Logger.getLogger(GeographicClassifier.class);
	private final File folder;
	private final MultiPolygon area;
	private Map<String,List<Id<Vehicle>>> lists;
	
	/**
	 * 
	 * @param args
	 * <ul>
	 * <li> args[0] : the absolute path to the folder containing XML vehicle files
	 * <li> args[1] : the absolute path to the shapefile for the specific area
	 * <li> args[2] : the percentage threshold to distinguish between intra- and inter-provincial vehicles (currently 0.6)
	 * <li> args[3] : number of threads to use
	 * <li> args[4] : the absolute path to the output folder - it must exist!
	 * </ul>
	 * 
	 * @throws IOException
	 */
	
	public static void main(String[] args) throws IOException{
		Header.printHeader(GeographicClassifier.class.toString(), args);
		
		String inputFolder = args[0];
		String shapefile = args[1];
		int idField = Integer.parseInt(args[2]);
		double threshold = Double.parseDouble(args[3]);
		int nThreads = Integer.parseInt(args[4]);
		String outputFolder = args[5];
		String descriptor = args[6];

		GeographicClassifier classifier = new GeographicClassifier(inputFolder, shapefile, idField);
				
		classifier.splitIntraInterExtra(threshold, nThreads);
		
		classifier.writeLists(outputFolder, descriptor);
		
		Header.printFooter();
	}


	/**
	 * Constructor
	 */
	public GeographicClassifier(String inputFolder, String shapefile, int idField){
		/* Check that the input folder exists, and is readable. */
		File f = new File(inputFolder);
		if(!f.exists() || !f.isDirectory() || !f.canRead()){
			throw new RuntimeException("Cannot read from input folder " + inputFolder);
		}
		this.folder = f;
		
		/* Read the geographic area. */
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		try {
			mfr.readMultizoneShapefile(shapefile, idField);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read shapefile from " + shapefile);
		}
		List<MyZone> list = mfr.getAllZones();
		if(list.size() > 1){
			LOG.warn("There are multiple shapefiles in " + shapefile);
			LOG.warn("Only the first will be used as the geographic area.");
		}
		this.area = list.get(0);
		
		/* Set up the result map. */
		lists = new HashMap<String, List<Id<Vehicle>>>();
		lists.put("intra", new ArrayList<Id<Vehicle>>());
		lists.put("inter", new ArrayList<Id<Vehicle>>());
		lists.put("extra", new ArrayList<Id<Vehicle>>());
	}
	
		
	/**
	 * Multi-threaded implementation of splitting {@link DigicoreVehicle}s 
	 * based on their activity locations relative a given geographic area. We 
	 * distinguish between three types:
	 * <ul>
	 * 	<li><b>intra-area</b>: those vehicles that spend at least a given 
	 * 		percentage of their activities inside the area;
	 * 	<li><b>inter-area</b>: those vehicles that have at least one, but no 
	 * 		more than the given percentage of their activities inside the area;
	 * 		and
	 * 	<li><b>extra-area</b>: those vehicles that do not have a single activity 
	 * 		inside the area.
	 * </ul>
	 * 
	 * @param files the {@link List} of {@link DigicoreVehicle} files;
	 * @param threshold a value, <code>0.0 &#8804 t &#8804 1.0</code>, used to
	 * 		  distinguish between intra- and inter-area traffic. Based on 
	 * 		  <a href="http://dx.doi.org/10.1016/j.jtrangeo.2009.11.005">Joubert 
	 * 		  & Axhausen (2011)</a>, a value of 0.6 is appropriate, at least 
	 * 		  for Gauteng, South Africa.  
	 * @param nThreads the number of threads required.
	 */
	public void splitIntraInterExtra(double threshold, int nThreads){
		LOG.info("Identifying intra-, inter-, and external vehicles...");
		
		/* Sample all vehicle files. */
		List<File> files = FileUtils.sampleFiles(this.folder, Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		
		/* Set up multi-threaded executor. */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(nThreads);
		List<Future<Tuple<Id<Vehicle>, Integer>>> listOfJobs = new ArrayList<Future<Tuple<Id<Vehicle>, Integer>>>();
		Counter counter = new Counter("   vehicles # ");		
		
		/* Execute the multi-threaded classification. */
		for(File file : files){
			Callable<Tuple<Id<Vehicle>, Integer>> job = new GeographicClassifierCallable(file, area, threshold, counter);
			Future<Tuple<Id<Vehicle>, Integer>> result = threadExecutor.submit(job);
			listOfJobs.add(result);
		}
		
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();
		
		/* Consolidate the output. */
		for(Future<Tuple<Id<Vehicle>, Integer>> job : listOfJobs){
			/* Get the job's result. */
			Tuple<Id<Vehicle>, Integer> tuple = null;
			try {
				tuple = job.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			
			int type = tuple.getSecond();
			switch (type) {
			case 0:
				lists.get("intra").add(tuple.getFirst());
				break;
			case 1:
				lists.get("inter").add(tuple.getFirst());
				break;
			case 2:
				lists.get("extra").add(tuple.getFirst());
				break;
			default:
				break;
			}
		}
		
		/* Write some summary statistics to the console. */
		LOG.info("Number of intra-provincial vehicles: " + lists.get("intra").size());
		LOG.info("Number of inter-provincial vehicles: " + lists.get("inter").size());
		LOG.info("Number of extra-provincial vehicles: " + lists.get("extra").size());
	}
	
	
	/**
	 * Writes the intra-, inter- and extra-area vehicle Ids to file. Each file
	 * only contains a list of vehicle Ids, one per line, without a header.
	 * @param folder where the three output files will be written to.
	 * @param descriptor a unique identifier, typically the year and province, 
	 * 		  that describes the contents better.
	 */
	public void writeLists(String folder, String descriptor){
		File f = new File(folder);
		if(!f.exists() || !f.isDirectory() || !f.canWrite()){
			throw new RuntimeException("Cannot write output to " + folder);
		}
		
		LOG.info("Writing vehicle splits to " + folder);
		for(String s : this.lists.keySet()){
			LOG.info("   " + s + " vehicles.");
			String filename = f.getAbsolutePath() + "/" + s + "_" + descriptor + ".txt";
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			try{
				for(Id<Vehicle> id : this.lists.get(s)){
					bw.write(id.toString());
					bw.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write to " + filename);
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close " + filename);
				}
			}
		}
		LOG.info("Done writing vehicle splits.");
	}
	
	
	public Map<String, List<Id<Vehicle>>> getLists(){
		return this.lists;
	}
	
}

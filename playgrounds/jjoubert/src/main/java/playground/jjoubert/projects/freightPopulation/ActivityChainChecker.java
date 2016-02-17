package playground.jjoubert.projects.freightPopulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader;
import playground.southafrica.freight.digicore.utils.DigicoreUtils;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/**
 * Class to analyse a Digicore activity chains for the purpose of evaluating 
 * the plans generation procedure. The results are used in the Joubert & 
 * Meintjes working paper #049 on activity chain generation. This class is 
 * meant to be executed for two subsets: the training and the test set of
 * vehicles. 
 *
 * @author jwjoubert
 */
public class ActivityChainChecker {
	final private static Logger LOG = Logger.getLogger(ActivityChainChecker.class);
	private static List<String> ABNORMAL_LIST = null;
	private static Geometry SA = null;
	private static Geometry SA_ENVELOPE = null;
	private static Geometry GAUTENG = null;
	private static Geometry GAUTENG_ENVELOPE = null;
	private static Geometry ETHEKWINI = null;
	private static Geometry ETHEKWINI_ENVELOPE = null;
	private static Geometry CAPETOWN = null;
	private static Geometry CAPETOWN_ENVELOPE = null;

	public static void main(String[] args) {
		Header.printHeader(ActivityChainChecker.class.toString(), args);
		
		String vehicleFolder = args[0];
		String vehicleIds = args[1];
		String abnormalDaysFile = args[2];
		String shapefileFolder = args[3];
		String outputFile = args[4];
		int numberOfThreads = Integer.parseInt(args[5]);
		
		/* Delete output file if it exists. */
		FileUtils.delete(new File(outputFile));
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write("Id,day,dayType,hour,activities,extent,vkt");
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
		
		List<File> vehicles = null;
		try {
			vehicles = DigicoreUtils.readDigicoreVehicleIds(vehicleIds, vehicleFolder);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not read vehicle ids in test set.");
		}
		LOG.info("Total number of vehicles identified: " + vehicles.size());
		
		ABNORMAL_LIST = DigicoreUtils.readAbnormalDays(abnormalDaysFile);
		setUpAreaGeometries(shapefileFolder);
		
		ActivityChainChecker acc = new ActivityChainChecker();
		acc.processVehicles(vehicles, outputFile, numberOfThreads);
		
		Header.printFooter();
	}
	
	public ActivityChainChecker() {

	}
	
	private void processVehicles(List<File> files, String outputFile, int numberOfThreads){
		LOG.info("Processing " + files.size() + " vehicles...");
		Counter counter = new Counter("   vehicles # ");
		
		/* Set up multi-threaded objective function evaluator. */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		List<Future<List<String>>> listOfJobs = new ArrayList<Future<List<String>>>();
		
		/* Assign each vehicle to a thread. */
		for(File file : files){
			Callable<List<String>> job = new ProcessorCallable(file, counter);
			Future<List<String>> result = threadExecutor.submit(job);
			listOfJobs.add(result);
		}
		
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}

		/* Run through all the output and print to file. */
		for(Future<List<String>> job : listOfJobs){

			/* Write this vehicle's output. */
			BufferedWriter bw = IOUtils.getAppendingBufferedWriter(outputFile);
			
			try{
				for(String s : job.get()){
					bw.write(s);
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write to " + outputFile);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException("Something went wrong with getting the multi-thread output!");
			} catch (ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException("Something went wrong with getting the multi-thread output!");
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close " + outputFile);
				}
			}
		}
	
		LOG.info("Done processing vehicles.");
	}
	
	private static void setUpAreaGeometries(String shapefileFolder){
		LOG.info("Parsing geographic areas.");
		String folder = shapefileFolder + (shapefileFolder.endsWith("/") ? "" : "/");

		/* Read South Africa */
		ShapeFileReader saReader = new ShapeFileReader();
		saReader.readFileAndInitialize(folder + "SouthAfrica/zones/SouthAfrica_SA-Albers.shp");
		SimpleFeature saFeature = saReader.getFeatureSet().iterator().next(); /* Just get the first one. */
		if(saFeature.getDefaultGeometry() instanceof MultiPolygon){
			SA = (MultiPolygon)saFeature.getDefaultGeometry();
			SA_ENVELOPE = SA.getEnvelope();
		}
		
		/* Read Gauteng */
		ShapeFileReader gReader = new ShapeFileReader();
		gReader.readFileAndInitialize(folder + "Gauteng/zones/Gauteng_PR2011_SA-Albers.shp");
		SimpleFeature gFeature = gReader.getFeatureSet().iterator().next(); /* Just get the first one. */
		if(gFeature.getDefaultGeometry() instanceof MultiPolygon){
			GAUTENG = (MultiPolygon)gFeature.getDefaultGeometry();
			GAUTENG_ENVELOPE = GAUTENG.getEnvelope();
		}
		
		/* Read Cape Town */
		ShapeFileReader ctReader = new ShapeFileReader();
		ctReader.readFileAndInitialize(folder + "CapeTown/zones/CapeTown_MN2011_SA-Albers.shp");
		SimpleFeature ctFeature = ctReader.getFeatureSet().iterator().next(); /* Just get the first one. */
		if(ctFeature.getDefaultGeometry() instanceof MultiPolygon){
			CAPETOWN = (MultiPolygon)ctFeature.getDefaultGeometry();
			CAPETOWN_ENVELOPE = CAPETOWN.getEnvelope();
		}
		
		/* Read eThekwini */
		ShapeFileReader etReader = new ShapeFileReader();
		etReader.readFileAndInitialize(folder + "eThekwini/zones/eThekwini_MN2011_SA-Albers.shp");
		SimpleFeature etFeature = etReader.getFeatureSet().iterator().next(); /* Just get the first one. */
		if(etFeature.getDefaultGeometry() instanceof MultiPolygon){
			ETHEKWINI = (MultiPolygon)etFeature.getDefaultGeometry();
			ETHEKWINI_ENVELOPE = ETHEKWINI.getEnvelope();
		}
		LOG.info("Done parsing geometries.");
	}
	
	private String evaluateExtent(DigicoreChain chain) {
		String extent = null;
		
		/* Check in SA. */
		boolean inSA = checkArea(chain, SA_ENVELOPE, SA);
		boolean inGauteng = checkArea(chain, GAUTENG_ENVELOPE, GAUTENG);
		boolean inCapeTown = checkArea(chain, CAPETOWN_ENVELOPE, CAPETOWN);
		boolean inEthekwini = checkArea(chain, ETHEKWINI_ENVELOPE, ETHEKWINI);
		
		if(	inGauteng == false &&
			inCapeTown == false &&
			inEthekwini == false &&
			inSA == false){
			extent = "a1";
		} else if(	inGauteng == false &&
					inCapeTown == false &&
					inEthekwini == false &&
					inSA == true){
			extent = "a2";
		} else if(	inGauteng == false &&
					inCapeTown == false &&
					inEthekwini == true &&
					inSA == true){
			extent = "a3";
		} else if(	inGauteng == false &&
					inCapeTown == true &&
					inEthekwini == false &&
					inSA == true){
			extent = "a4";
		} else if(	inGauteng == true &&
					inCapeTown == false &&
					inEthekwini == false &&
					inSA == true){
			extent = "a5";
		} else if(	inGauteng == true &&
					inCapeTown == true &&
					inEthekwini == false &&
					inSA == true){
			extent = "a6";
		} else if(	inGauteng == false &&
					inCapeTown == true &&
					inEthekwini == true &&
					inSA == true){
			extent = "a7";
		} else if(	inGauteng == true &&
					inCapeTown == true &&
					inEthekwini == true &&
					inSA == true){
			extent = "a8";
		} else if(	inGauteng == true &&
					inCapeTown == false &&
					inEthekwini == true &&
					inSA == true){
			extent = "a9";
		} else{
			LOG.warn("Could not find the extent for [" + inGauteng + ";" + 
		inCapeTown + ";" + inEthekwini + ";" + inSA  + "]");
		}
		
		return extent;
	}
	
	private boolean checkArea(DigicoreChain chain, Geometry envelope, Geometry geometry){
		GeometryFactory gf = new GeometryFactory();

		boolean found = false;
		Iterator<DigicoreActivity> iterator = chain.getAllActivities().iterator();
		while(!found && iterator.hasNext()){
			Coord c = iterator.next().getCoord();
			Point p = gf.createPoint(new Coordinate(c.getX(), c.getY()));
			if(envelope.contains(p)){
				if(geometry.contains(p)){
					found = true;
				}
			}
		}
		return found;
	}

	
	private boolean isValidDay(GregorianCalendar cal){
		boolean valid = false;
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		
		/* Check that it is a weekday. */
		if(dayOfWeek > 1 && dayOfWeek < 7){
			/* Check that it is a normal day. */
			String s = DigicoreUtils.getShortDate(cal);
			if(!ABNORMAL_LIST.contains(s)){
				valid = true;
			}
		}
		return valid;
	}
	
	private int getDayType(GregorianCalendar cal){
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		String s = DigicoreUtils.getShortDate(cal);
		if(ABNORMAL_LIST.contains(s)){
			dayOfWeek = 8;
		}
		
		return dayOfWeek;
	}
	
	
	private class ProcessorCallable implements Callable<List<String>>{
		private List<String> outputList = new ArrayList<String>();
		private final double distanceMultiplier = 1.3;
		private File vehicle;
		private Counter counter;
		
		public ProcessorCallable(File vehicleFile, Counter counter) {
			this.vehicle = vehicleFile;
			this.counter = counter;
		}
		
		@Override
		public List<String> call() throws Exception {
			/* Read the vehicle */
			DigicoreVehicleReader dvr = new DigicoreVehicleReader();
			dvr.parse(this.vehicle.getAbsolutePath());
			DigicoreVehicle dv = dvr.getVehicle();
			
			/* Check each chain start date. */
			for(DigicoreChain chain : dv.getChains()){
				GregorianCalendar chainStart = chain.get(0).getEndTimeGregorianCalendar();
				
				/* Get the day of the week, with '8' denoting an abnormal day. */
				int dayOfWeek = getDayType(chainStart);

				/* Get start hour. */
				int hour = chainStart.get(Calendar.HOUR_OF_DAY);
				
				/* Get number of minor activities. */
				int numberOfActivities = chain.getNumberOfMinorActivities();
				
				/* Evaluate the chain's geographic extent. */
				String extent = evaluateExtent(chain);
				
				/* Get the estimated vehicle kilometers travelled. */
				double vkt = estimateVkt(chain)/1000.0;
				
				this.outputList.add(String.format("%s,%s,%d,%d,%d,%s,%.2f\n", 
						dv.getId().toString(), DigicoreUtils.getShortDate(chainStart), dayOfWeek, hour, numberOfActivities, extent, vkt));
			}
			
			counter.incCounter();
			return outputList;
		}

		private double estimateVkt(DigicoreChain chain){
			double distance = 0.0;
			Coord c1 = chain.get(0).getCoord();
			for(int i = 1; i < chain.size(); i++){
				Coord c2 =chain.get(i).getCoord();
				distance += CoordUtils.calcEuclideanDistance(c1, c2)*distanceMultiplier;
				c1 = c2;
			}
			
			return distance;
		}

	}

}

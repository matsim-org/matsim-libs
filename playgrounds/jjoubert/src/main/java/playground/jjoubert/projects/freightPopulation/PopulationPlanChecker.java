package playground.jjoubert.projects.freightPopulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/**
 * Class to analyse a given population for the purpose of evaluating the plans
 * generation procedure. The results are used in the Joubert & Meintjes working
 * paper #049 on activity chain generation. 
 *
 * @author jwjoubert
 */
public class PopulationPlanChecker {
	final private static Logger LOG = Logger.getLogger(PopulationPlanChecker.class);
	private static Geometry SA = null;
	private static Geometry SA_ENVELOPE = null;
	private static Geometry GAUTENG = null;
	private static Geometry GAUTENG_ENVELOPE = null;
	private static Geometry ETHEKWINI = null;
	private static Geometry ETHEKWINI_ENVELOPE = null;
	private static Geometry CAPETOWN = null;
	private static Geometry CAPETOWN_ENVELOPE = null;

	public static void main(String[] args) {
		Header.printHeader(PopulationPlanChecker.class.toString(), args);
		
		String population = args[0];
		String shapefileFolder = args[1];
		String outputFile = args[2];
		int numberOfThreads = Integer.parseInt(args[3]);
		
		/* Delete output file if it exists. */
		FileUtils.delete(new File(outputFile));
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		try{
			bw.write("Id,hour,activities,extent,vkt");
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
		
		Scenario sc= ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(sc).parse(population);
		
		setUpAreaGeometries(shapefileFolder);
		
		PopulationPlanChecker ctsa = new PopulationPlanChecker();
		ctsa.processVehicles(sc.getPopulation(), outputFile, numberOfThreads);
		
		Header.printFooter();
	}
	
	public PopulationPlanChecker() {

	}
	
	private void processVehicles(Population population, String outputFile, int numberOfThreads){
		LOG.info("Processing " + population.getPersons().size() + " persons...");
		Counter counter = new Counter("   persons # ");
		
		/* Set up multi-threaded objective function evaluator. */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		List<Future<List<String>>> listOfJobs = new ArrayList<Future<List<String>>>();
		
		/* Assign each vehicle to a thread. */
		for(Person person : population.getPersons().values()){
			Callable<List<String>> job = new ProcessorCallable(person, counter);
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
	
	private String evaluateExtent(Plan plan) {
		String extent = null;
		
		/* Check in SA. */
		boolean inSA = checkArea(plan, SA_ENVELOPE, SA);
		boolean inGauteng = checkArea(plan, GAUTENG_ENVELOPE, GAUTENG);
		boolean inCapeTown = checkArea(plan, CAPETOWN_ENVELOPE, CAPETOWN);
		boolean inEthekwini = checkArea(plan, ETHEKWINI_ENVELOPE, ETHEKWINI);
		
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
	
	private boolean checkArea(Plan plan, Geometry envelope, Geometry geometry){
		GeometryFactory gf = new GeometryFactory();

		boolean found = false;
		Iterator<PlanElement> iterator = plan.getPlanElements().iterator();
		while(!found && iterator.hasNext()){
			PlanElement pe = iterator.next();
			if(pe instanceof Activity){
				Activity act = (Activity)pe;
				Coord c = act.getCoord();
				Point p = gf.createPoint(new Coordinate(c.getX(), c.getY()));
				if(envelope.contains(p)){
					if(geometry.contains(p)){
						found = true;
					}
				}
			}
		}
		return found;
	}
	

	
	private class ProcessorCallable implements Callable<List<String>>{
		private final double distanceMultiplier = 1.3;
		private List<String> outputList = new ArrayList<String>();
		private Person person;
		private Counter counter;
		
		public ProcessorCallable(Person person, Counter counter) {
			this.person = person;
			this.counter = counter;
		}
		
		@Override
		public List<String> call() throws Exception {
			Plan plan = person.getSelectedPlan();
			/* Get start hour. */
			double endTime = ((Activity)plan.getPlanElements().get(0)).getEndTime();
			while(endTime > (24*60*60)){
				endTime -= (24.0*60.0*60.0);
			}
			int hour = (int) Math.round(Math.floor(endTime / 3600));
			
			/* Get number of minor activities. */
			int numberOfActivities = ((plan.getPlanElements().size() + 1) / 2) - 2;
			
			/* Evaluate the chain's geographic extent. */
			String extent = evaluateExtent(plan);
			
			/* Get the estimated vehicle kilometers travelled. */
			double vkt = estimateVkt(plan)/1000.0;

			this.outputList.add(String.format("%s,%d,%d,%s,%.2f\n", 
					person.getId().toString(), hour, numberOfActivities, extent, vkt));
			
			counter.incCounter();
			return outputList;
		}

		private double estimateVkt(Plan plan){
			double distance = 0.0;
			Coord c1 = ((Activity)plan.getPlanElements().get(0)).getCoord();
			for(int i = 2; i < plan.getPlanElements().size(); i+= 2){
				Coord c2 = ((Activity)plan.getPlanElements().get(i)).getCoord();
				distance += CoordUtils.calcEuclideanDistance(c1, c2)*distanceMultiplier;
				c1 = c2;
			}
			
			return distance;
		}
	}

}

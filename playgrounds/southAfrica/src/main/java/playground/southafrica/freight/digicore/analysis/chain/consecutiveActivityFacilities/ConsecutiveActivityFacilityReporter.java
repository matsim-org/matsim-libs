/**
 * 
 */
package playground.southafrica.freight.digicore.analysis.chain.consecutiveActivityFacilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader;
import playground.southafrica.freight.digicore.utils.DigicoreUtils;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Class to run through activity chain, and check for consecutive activities
 * that occur at the same {@link ActivityFacility}. Note that the assignment of
 * an {@link ActivityFacility} is dependent on the clustering parameters and 
 * may vary (significantly) for different runs with different parameter 
 * configurations.
 * 
 * @author jwjoubert
 */
public class ConsecutiveActivityFacilityReporter {
	private final Logger log = Logger.getLogger(ConsecutiveActivityFacilityReporter.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ConsecutiveActivityFacilityReporter.class.toString(), args);
		String xmlFolder = args[0];
		String outputFile = args[1];
		String abnormalDaysFile = args[2];
		int numberOfThreads = Integer.parseInt(args[3]);
		
		/* Parse the abnormal days. */
		List<Integer> abnormalDays = DigicoreUtils.readDayOfYear(abnormalDaysFile);
		
		
		ConsecutiveActivityFacilityReporter cafr = new ConsecutiveActivityFacilityReporter();
		cafr.run(xmlFolder, abnormalDays, outputFile, numberOfThreads);
		Header.printFooter();
	}
	
	public ConsecutiveActivityFacilityReporter() {
		log.info("Cleaning temporary folder (if it exists)");
		File tmpFolder = new File("tmp/");
		FileUtils.delete(tmpFolder);
		boolean createdTmpFolder = tmpFolder.mkdirs();
		if(!createdTmpFolder){
			throw new RuntimeException("Couldn't create the temporary folder " + tmpFolder.getAbsolutePath());
		}
	}
	
	
	public void run(String xmlFolder, List<Integer> abnormalDays, String outputfile, int numberOfThreads){
		log.info("Running the consecutive activity finder with " + numberOfThreads + " threads.");
		/* Run the multi-threaded activity finder. */
		List<File> files = FileUtils.sampleFiles(new File(xmlFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		List<ConsecutiveActivityFinderRunnable> listOfJobs = new ArrayList<>(files.size());
		
		/* Execute the multi-threaded analysis. */
		Counter counter = new Counter("  vehicles # ");
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		for(File file : files){
			ConsecutiveActivityFinderRunnable job = new ConsecutiveActivityFinderRunnable(file, counter, abnormalDays);
			threadExecutor.execute(job);
			listOfJobs.add(job);
		}
		
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();
		log.info("Done finding consecutive activities.");
		
		/* Consolidate output. */
		log.info("Consolidate output files...");
		List<File> tmpOutputs = FileUtils.sampleFiles(new File("tmp/"), Integer.MAX_VALUE, FileUtils.getFileFilter(".csv.gz"));
		BufferedWriter bw = IOUtils.getBufferedWriter(outputfile);
		try{
			bw.write("vehId,facilityId,chainLength,position,chainDay,x,y,lon,lat,activityDay,dateTime,timeBetween,startToStart,isDuplicate");
			bw.newLine();
			
			for(File file : tmpOutputs){
				BufferedReader br = IOUtils.getBufferedReader(file.getAbsolutePath());
				try{
					String line = null;
					while((line=br.readLine()) != null){
						bw.write(line);
						bw.newLine();
					}
				} finally{
					br.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputfile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputfile);
			}
		}
		
		/* Clean up. */
		log.info("Cleaning up temporary folder...");
		FileUtils.delete(new File("tmp/"));
	}
	
	
	private class ConsecutiveActivityFinderRunnable implements Runnable{
		private File vehicle;
		private Counter counter;
		private List<Integer> abnormalDays;
		
		public ConsecutiveActivityFinderRunnable(File file, Counter counter, List<Integer> abnormalDays) {
			this.vehicle = file;
			this.counter = counter;
			this.abnormalDays = abnormalDays;
		}
		
		@Override
		public void run() {
			/* Set up the output. */
			List<String> strings = new ArrayList<>();
			
			/* Read the vehicle. */
			DigicoreVehicleReader dvr = new DigicoreVehicleReader();
			dvr.parse(this.vehicle.getAbsolutePath());
			DigicoreVehicle dv = dvr.getVehicle();			
			
			/* Process each chain. */
			CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
			for(DigicoreChain chain : dv.getChains()){
				for(int i = 1; i < chain.size(); i++){
					DigicoreActivity previous = chain.get(i-1);
					DigicoreActivity current = chain.get(i);
					if(previous.getFacilityId() != null & current.getFacilityId() != null){
						/* Right, now get all the statistics together. */
						String vehicleId = dv.getId().toString();
						
						String facilityId = "NA";
						Id<ActivityFacility> id = current.getFacilityId();
						if(id != null){
							facilityId = id.toString();
						}
						int position = i+1;
						int chainLength = chain.size();
						int chainDay = chain.getChainStartDay(abnormalDays);
						Coord c = current.getCoord();
						Coord cWgs = ct.transform(c);
						String dateTime = DigicoreUtils.getShortDateAndTime(current.getStartTimeGregorianCalendar());
						int activityDay = current.getStartTimeGregorianCalendar().get(Calendar.DAY_OF_WEEK);
						int dayOfYear = current.getStartTimeGregorianCalendar().get(Calendar.DAY_OF_YEAR);
						if(abnormalDays.contains(dayOfYear)){
							activityDay = 8;
						}
						long currentStartTime = current.getStartTimeGregorianCalendar().getTimeInMillis();
						long previousEndTime = previous.getEndTimeGregorianCalendar().getTimeInMillis();
						long previousStartTime = previous.getStartTimeGregorianCalendar().getTimeInMillis();
						
						double timeBetweenDuplicates = (((double)currentStartTime) - ((double)previousEndTime))/(60.0*1000.0);
						double startToStart = (((double)currentStartTime) - ((double)previousStartTime))/(60.0*1000.0);
						
						/* Check if the facility Ids are the same. */
						String isDuplicate = "false";
						if(previous.getFacilityId().equals(current.getFacilityId())){
							isDuplicate = "true";
						}
						String line = vehicleId 
								+ "," + facilityId
								+ "," + String.valueOf(chainLength)
								+ "," + String.valueOf(position)
								+ "," + String.valueOf(chainDay)
								+ "," + String.format("%.0f,%.0f", c.getX(), c.getY())
								+ "," + String.format("%.6f,%.6f", cWgs.getX(), cWgs.getY())
								+ "," + activityDay
								+ "," + dateTime
								+ "," + String.format("%.0f", timeBetweenDuplicates)
								+ "," + String.format("%.0f", startToStart)
								+ "," + isDuplicate;
						
						strings.add(line);
						
					}
				}
			}
			
			/* Write the output to temporary file. */
			if(strings.size() > 0){
				File output = new File("tmp/" + dv.getId().toString() + ".csv.gz") ;
				BufferedWriter bw = IOUtils.getBufferedWriter(output.getAbsolutePath());
				try{
					for(String s : strings){
						bw.write(s);
						bw.newLine();
					}
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot write to " + output.getAbsolutePath());
				} finally{
					try {
						bw.close();
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("Cannot close " + output.getAbsolutePath());
					}
				}
			}
			
			counter.incCounter();
		}
		
	}

}

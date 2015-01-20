/**
 * 
 */
package playground.southafrica.freight.digicore.analysis.chain;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader;
import playground.southafrica.freight.digicore.utils.DigicoreUtils;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 *
 * @author jwjoubert
 */
public class BasicChainAnalyser {
	final private static Logger LOG = Logger.getLogger(BasicChainAnalyser.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(BasicChainAnalyser.class.toString(), args);
		
		String xmlFolder = args[0];
		String outputFile = args[1];
		String abnormalDaysFile = args[2];
		int numberOfThreads = Integer.parseInt(args[3]);

		/* Delete output file if it exists. */
		FileUtils.delete(new File(outputFile));
		BufferedWriter bwHeader = IOUtils.getBufferedWriter(outputFile);
		try{
			bwHeader.write("Id,day,dayType,hour,activities");
			bwHeader.newLine();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bwHeader.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
		
		/* Parse the abnormal days. */
		List<String> abnormalDays = DigicoreUtils.readAbnormalDays(abnormalDaysFile);

		/* Get all the vehicle files that must be analysed. */
		List<File> files;
		String vehicleIds;
		if(args.length > 4){
			vehicleIds = args[4];
			try {
				files = DigicoreUtils.readDigicoreVehicleIds(vehicleIds, xmlFolder);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not read vehicle Ids from " + vehicleIds);
			}
		} else{
			files = FileUtils.sampleFiles(new File(xmlFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		}
		
		/* Set up multi-threaded objective function evaluator. */
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		List<Future<List<String>>> listOfJobs = new ArrayList<Future<List<String>>>();
		
		/* Assign each vehicle to a thread. */
		LOG.info("Processing vehicles (" + files.size() + ")...");
		Counter counter = new Counter("   vehicles # ");
		for(File file : files){
			Callable<List<String>> job = new ProcessorCallable(file, abnormalDays, counter);
			Future<List<String>> result = threadExecutor.submit(job);
			listOfJobs.add(result);
		}
		
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();
		LOG.info("Done processing vehicles.");

		/* Run through all the output and print to file. */
		LOG.info("Aggregating multi-threaded output.");
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
		
		Header.printFooter();
	}
	

	private static class ProcessorCallable implements Callable<List<String>>{
		private List<String> outputList = new ArrayList<String>();
		private File vehicle;
		private List<String> abnormalDays;
		private Counter counter;
		
		public ProcessorCallable(File vehicleFile, List<String> abnormalDays, Counter counter) {
			this.vehicle = vehicleFile;
			this.abnormalDays = abnormalDays;
			this.counter = counter;
		}

		private int getDayType(GregorianCalendar cal){
			String date = DigicoreUtils.getShortDate(cal);
			
			int result = 0;
			
			/* First check for 'abnormal' day, otherwise get the day of the week. */
			if(this.abnormalDays.contains(date)){
				result = 8;
			} else {
				result = cal.get(Calendar.DAY_OF_WEEK);
			}
			return result;
		}
		
		@Override
		public List<String> call() throws Exception {
			/* Read the vehicle */
			DigicoreVehicleReader dvr = new DigicoreVehicleReader();
			dvr.parse(this.vehicle.getAbsolutePath());
			DigicoreVehicle dv = dvr.getVehicle();
			
			/* Check each chain. */
			for(DigicoreChain chain : dv.getChains()){
				GregorianCalendar chainStart = chain.get(0).getEndTimeGregorianCalendar();
				String day = DigicoreUtils.getShortDate(chainStart);
				int dayType = getDayType(chainStart);
				int hour = chainStart.get(Calendar.HOUR_OF_DAY);
				int numberOfActivities = chain.getNumberOfMinorActivities();
				
				this.outputList.add(String.format("%s,%s,%d,%d,%d\n", 
						dv.getId().toString(), day, dayType, hour, numberOfActivities));
			}
			
			this.counter.incCounter();
			return this.outputList;
		}
	}
	
}

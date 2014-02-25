/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.jjoubert.DigiCore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.DateString;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 *
 * @author jwjoubert
 */
public class ExtractOneDayTraces {
	private final static Logger LOG = Logger.getLogger(ExtractOneDayTraces.class);

	/**
	 * Extract all the GPS traces on a particular day.
	 * @param args arguments in the following order:
	 * <ol>
	 * 		<li> date, in the format "YYYYMMDD";
	 * 		<li> input folder containing the gzipped text files with gps traces;
	 * 		<li> output file; and
	 * 		<li> number of threads to use.
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(ExtractOneDayTraces.class.toString(), args);
		String date = args[0];
		String inputfolder = args[1];
		String outputFile = args[2];
		int numberOfThreads = Integer.parseInt(args[3]);
		
		ExtractOneDayTraces.Run(date, inputfolder, outputFile, numberOfThreads);
		
		Header.printFooter();
	}
	
	public static void Run(String date, String inputfolder, String outputfile, int numberOfThreads){
		List<File> files = FileUtils.sampleFiles(new File(inputfolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".txt.gz"));
		Counter counter = new Counter("  files # ");
		int year = Integer.parseInt(date.substring(0, 4));
		int month = Integer.parseInt(date.substring(4, 6));
		int day = Integer.parseInt(date.substring(6, 8));
		GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		calendar.set(year, month, day);
		LOG.info(String.format("Checking for date: %d/%02d/%02d",
				calendar.get(Calendar.YEAR), 
				calendar.get(Calendar.MONTH), 
				calendar.get(Calendar.DAY_OF_MONTH) ));
		/* Set up the multithreaded infrastructure. */ 
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		List<Future<List<String>>> listOfJobs = new ArrayList<Future<List<String>>>();
		
		/* Assign each vehicle's gps traces to a thread. */
		for(File f : files){
			Callable<List<String>> job = new RunVehicle(counter, f, calendar);
			Future<List<String>> result = threadExecutor.submit(job);
			listOfJobs.add(result);
		}
		
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){}
		
		/* Consolidate the output. */
		BufferedWriter bw = IOUtils.getBufferedWriter(outputfile);
		try{
			bw.write("VehicleID,Time,HourOfDay,Long,Lat");
			bw.newLine();
			
			for(Future<List<String>> job : listOfJobs){
				List<String> traces = job.get();
				for (String s : traces){
					bw.write(s);
					bw.newLine();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputfile);
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot get multithreaded result.");
		} catch (ExecutionException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot get multithreaded result.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputfile);
			}
		}
	}
	
	private static class RunVehicle implements Callable<List<String>>{
		private final Counter counter;
		private final File file;
		private final GregorianCalendar date;
		
		public RunVehicle(final Counter counter, final File file, final GregorianCalendar date) {
			this.counter = counter;
			this.file = file;
			this.date = date;
		}

		@Override
		public List<String> call() throws Exception {
			List<String> list = new ArrayList<String>();
			BufferedReader br = IOUtils.getBufferedReader(this.file.getAbsolutePath());
			boolean dateExceeded = false;
			try{
				String line = br.readLine(); /* Header. */
				while(!dateExceeded && (line = br.readLine()) != null){
					String[] sa = line.split(",");
					GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
					cal.setTimeInMillis(Long.parseLong(sa[1])*1000);
					if(cal.get(Calendar.DAY_OF_YEAR) == this.date.get(Calendar.DAY_OF_YEAR)){
						/* Convert the time into a more usable form. */
						String time = convertDate(cal);
						String newLine = sa[0] + "," + time + "," + Integer.parseInt(time.substring(9, 11)) + ","  + sa[2] + "," + sa[3];
						list.add(newLine);
					} else if(cal.get(Calendar.DAY_OF_YEAR) > this.date.get(Calendar.DAY_OF_YEAR)){
						dateExceeded = true;
					}
				}
			} finally{
				br.close();
			}
			counter.incCounter();
			return list;
		}
		
		private String convertDate(GregorianCalendar calendar){
			DateString ds = new DateString();
			ds.setTimeInMillis(calendar.getTimeInMillis());
			return ds.toString();
		}
	}

}

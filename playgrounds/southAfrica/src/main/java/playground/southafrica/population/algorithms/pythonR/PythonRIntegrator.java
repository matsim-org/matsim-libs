/* *********************************************************************** *
 * project: org.matsim.*
 * TheRandPythonIntegrator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.population.algorithms.pythonR;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.DateString;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

/**
 * Integrate the Iterative Proportional Fitting (IPF) procedures (python code 
 * of Kirill Mueller) with the population synthesis (R script adapted from
 * Pieter Fourie).
 * 
 * @author jwjoubert
 */
public class PythonRIntegrator {
	private final static Logger LOG = Logger.getLogger(PythonRIntegrator.class);
	private final String baseFolder;
	private final String outputFolder;
	private Map<String, String> controlTotalsMap;
	private List<String> zoneList;
	
	/**
	 * Implements the Python-R-integration. It is assumed the first argument,
	 * the base folder, is where the output will be written to. It should also,
	 * as input, contain a folder <code>template</code> with the following 
	 * contents:
	 * <ul>
	 * 		<li> <code>controlTotals.csv</code>, as the name suggests, the 
	 * 			control totals for the subplaces (zones) for which a population 
	 * 			is to be generated. 
	 * 		<li> <code>REFSAMPLE.dat</code>, a tab-separated file with the
	 * 			input data, i.e. reference sample, for the iterative proportional 
	 * 			fitting.
	 * 		<li> <code>INC_zero.csv</code>, a small file used during python run.
	 * 			It is provided.
	 * 		<li> <code>POP_GEN_zero.csv</code>, a small file used during python
	 * 			run. It, too, is provided.
	 * 		<li> <code>config.xml</code> the configuration file required for the 
	 * 			python run.
	 * 		<li> <code>src</code>, the source folder containing all the provided 
	 * 			python classes.
	 * 		<li> <code>R</code>, the source folder containing the single R script
	 * 			file, <code>sampling.R</code>, also provided.
	 * </ul>
	 * For each zone, the template files will be copied into a temporary folder
	 * @param args three arguments must be provided:
	 * 	<ol>
	 * 		<li> absolute path of the base folder;
	 * 		<li> number of threads to be used; and
	 * 		<li> the number of threads to be used concurrently, i.e. the 
	 * 			<i>block size</i> in which the zones will be fitted. This is
	 * 			to reduce memory. Since the current python code used requires
	 * 			that each zones be fitted separately, each zone must be provided 
	 * 			with the reference sample. The multi-threaded implementation then
	 * 			results in many duplicate reference samples be created. This 
	 * 			<i>block size</i> approach is an attempt to conserve memory so that
	 * 			only a certain number of threads are executed at a time, and then
	 * 			java wait until all are finished before executing the next block 
	 * 			of zones.
	 * </ol> 
	 */
	public static void main(String[] args) {
		Header.printHeader(PythonRIntegrator.class.toString(), args);
		String baseFolder = args[0];
		int numberOfThreads = Integer.parseInt(args[1]);
		int blockSize = Integer.parseInt(args[2]);
		
		PythonRIntegrator pji = new PythonRIntegrator(baseFolder);
		
//		List<String> dummyList = new ArrayList<String>();
//		dummyList.add("27502000");
//		pji.runMultithreaded(dummyList, 2, numberOfThreads, blockSize);
//		pji.runMultithreaded(pji.zoneList, 2, numberOfThreads, blockSize);
		pji.runMultithreaded(pji.zoneList, numberOfThreads, blockSize);
		
		pji.aggregate();
		
		Header.printFooter();
	}

	
	/**
	 * Instantiating the Python-R-integration. The existence of the base folder
	 * is checked, and the zones that must be fitted is parsed from the control
	 * totals file. 
	 * @param baseFolder absolute path of the base folder.
	 */
	public PythonRIntegrator(String baseFolder) {
		File folder = new File(baseFolder);
		if(!folder.exists() || !folder.isDirectory()){
			throw new RuntimeException("The base folder is invalid. ABORTING");
		}
		this.baseFolder = baseFolder;

		DateString ds = new DateString();
		ds.setTimeInMillis(System.currentTimeMillis());
		File of = new File(baseFolder + "output_" + ds.toString() + "/");
		boolean createdOutputFolder = of.mkdirs();
		if(!createdOutputFolder){
			throw new RuntimeException("Could not create output folder ... ABORTING.");
		}
		this.outputFolder = of.getAbsolutePath() + "/";
		this.controlTotalsMap = parseControlTotals(this.baseFolder + "template/controlTotals.csv");
		this.zoneList = new ArrayList<String>();
		for(String id : this.controlTotalsMap.keySet()){
			this.zoneList.add(id);
		}
	}
	
	
	public void runMultithreaded(List<String> zoneList, int sampleSize, int numberOfThreads, int blockSize){
		LOG.info("Running multi-threaded job...");
		LOG.info("  number of threads: " + numberOfThreads);
		LOG.info("     jobs per block: " + blockSize);
		Counter counter = new Counter("  subplaces # ");		
				
		int zoneCounter = 0;
		ExecutorService threadExecutor = null;
		while(zoneCounter < Math.min(zoneList.size(), sampleSize)){
			int blockCounter = 0;
			threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
			
			/* Assign the jobs in blocks. */
			while(blockCounter++ < blockSize && zoneCounter < Math.min(zoneList.size(), sampleSize)){
				String theZone = zoneList.get(zoneCounter++);
				String thisZoneFolder = copyAllFilesToTemporaryFolder(baseFolder, theZone);
				if(thisZoneFolder != null){
					PythonRIntegratorRunnable job = new PythonRIntegratorRunnable(counter, thisZoneFolder, theZone, outputFolder);
					threadExecutor.execute(job);					
				} else{
					LOG.error("Could not create a successful folder for zone " + theZone);
				}
			}
			
			/* Wait for the block to terminate. */
			threadExecutor.shutdown();
			while(!threadExecutor.isTerminated()){
			}			
		}
		counter.printCounter();
		LOG.info("Done with multi-threaded job.");
	}
	
	
	public void runMultithreaded(List<String> zoneList, int numberOfThreads, int blockSize){
		this.runMultithreaded(zoneList, Integer.MAX_VALUE, numberOfThreads, blockSize);
	}
	
	
	/**
	 * Copies all the necessary files into a temporary folder for the run. 
	 * These include:
	 * <ol>
	 * 	<li> the control totals and reference sample;
	 * 	<li> all the Python classes; and
	 * 	<li> the R sampling script file.
	 * </ol> 
	 * @param basefolder
	 * @param theZone
	 * @return the absolute path of the folder containing all the files, but 
	 * 		only if all files were copied successfully. Otherwise returns null. 
	 */
	private String copyAllFilesToTemporaryFolder(String basefolder, String theZone){
		/* --- 1. Create a temporary folder --- */
		DateString	ds = new DateString();
		ds.setTimeInMillis(System.currentTimeMillis());
		String thisFoldername = baseFolder + ds.toString() + "/";	
		File folder = new File(thisFoldername);
		boolean createdThreadFolder = folder.mkdirs();
		if(!createdThreadFolder){
			LOG.error("Could not create a thread-specific folder for " + theZone);
		} 
		
		/* --- 2. Move the necessary files into the temporary folder --- */
		boolean filesCopiedSuccessfully = true;
		/* Data. */
		writeControlTotals(thisFoldername, theZone);
		File oConfig = new File(baseFolder + "template/config.xml");
		File dConfig = new File(thisFoldername + "config.xml");
		File oReference = new File(baseFolder + "template/REFSAMPLE.dat");
		File dReference = new File(thisFoldername + "REFSAMPLE.dat");
		File oIndividualZero = new File(baseFolder + "template/INC_zero.csv");
		File dIndividualZero = new File(thisFoldername + "INC_zero.csv");
		File oGroupZero = new File(baseFolder + "template/POP_GEN_zero.csv");
		File dGroupZero = new File(thisFoldername + "POP_GEN_zero.csv");
		try {
			FileUtils.copyFile(oConfig, dConfig);
			FileUtils.copyFile(oReference, dReference);
			FileUtils.copyFile(oIndividualZero, dIndividualZero);
			FileUtils.copyFile(oGroupZero, dGroupZero);
		} catch (IOException e) {
			LOG.error("Could not copy the data files for zone " + theZone);
			filesCopiedSuccessfully = false;
		}
		
		/* Python. */
		FileFilter pythonfilter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if(pathname.getName().endsWith(".py")){
					return true;
				}
				return false;
			}
		};
		File[] pythonFiles = (new File(baseFolder + "template/src/")).listFiles(pythonfilter);
		for(File f : pythonFiles){
			File destinationFile = new File(thisFoldername + f.getName());
			try {
				FileUtils.copyFile(f, destinationFile);
			} catch (IOException e) {
				LOG.error("Could not copy the Python files for zone " + theZone);
				filesCopiedSuccessfully = false;
			}
		}
		
		/* R. */
		File oR = new File(baseFolder + "template/R/sampling.R");
		File dR = new File(thisFoldername + "sampling.R");
		try {
			FileUtils.copyFile(oR, dR);
		} catch (IOException e) {
			LOG.error("Could not copy R file for zone " + theZone);
			filesCopiedSuccessfully = false;
		}
		if(filesCopiedSuccessfully){
			return thisFoldername;
		} else{
			return null;
		}
	}
	
	
	/**
	 * TODO Explain the detailed file layout required. This is also an 
	 * implementation-specific method. Should possible make this an interface 
	 * and force the user to create their unique methods. 
	 * @param folder
	 * @param zone
	 */
	private void writeControlTotals(String folder, String zone){
		String[] sa = this.controlTotalsMap.get(zone).split(",");
		BufferedWriter bw = null;
		
		/* Write the individual control totals. */
		bw = IOUtils.getBufferedWriter(folder + "individualCT.dat");
		try{
			bw.write(String.format("POP\tGEN\tN\n"));
			bw.write(String.format("1\t1\t%s\n", sa[2]));
			bw.write(String.format("2\t1\t%s\n", sa[3]));
			bw.write(String.format("3\t1\t%s\n", sa[4]));
			bw.write(String.format("4\t1\t%s\n", sa[5]));
			bw.write(String.format("1\t2\t%s\n", sa[6]));
			bw.write(String.format("2\t2\t%s\n", sa[7]));
			bw.write(String.format("3\t2\t%s\n", sa[8]));
			bw.write(String.format("4\t2\t%s", sa[9]));
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter for individual control totals for zone " + zone);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter for individual control totals for zone " + zone);
			}
		}

		/* Write the group control totals. */
		bw = IOUtils.getBufferedWriter(folder + "groupCT.dat");
		try{
			bw.write(String.format("INC\tN\n"));
			bw.write(String.format("1\t%s\n", sa[10]));
			bw.write(String.format("3\t%s\n", sa[11]));
			bw.write(String.format("5\t%s\n", sa[12]));
			bw.write(String.format("7\t%s\n", sa[13]));
			bw.write(String.format("9\t%s\n", sa[14]));
			bw.write(String.format("11\t%s\n", sa[15]));
			bw.write(String.format("12\t%s", sa[16]));
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter for group control totals for zone " + zone);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter for group control totals for zone " + zone);
			}
		}
	}

					
	/**
	 * Reads in all the subplace CSV files generated as output, and create two 
	 * aggregate files: one for persons, and the other for households.
	 * TODO In the end I think we only really need the persons file, as it
	 * contains the household Id too. For now, leave it, it's part of what
	 * Pieter's R code generates. 
	 */
	public void aggregate(){
		File outputFolder = new File(this.outputFolder);
		
		/* Start with household file. */
		FileFilter householdFileFilter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				String filename = pathname.getName();
				if(filename.startsWith("hh") && filename.endsWith(".csv")){
					return true;
				}
				return false;
			}
		};
		BufferedWriter bwHouseholds = IOUtils.getBufferedWriter(this.outputFolder + "households.txt.gz");
		aggregateOutput(outputFolder, householdFileFilter, bwHouseholds, "HHNR,w");

		/* Start with household file. */
		FileFilter personFileFilter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				String filename = pathname.getName();
				if(filename.startsWith("pax") && filename.endsWith(".csv")){
					return true;
				}
				return false;
			}
		};
		BufferedWriter bwPersons = IOUtils.getBufferedWriter(this.outputFolder + "persons.txt.gz");
		aggregateOutput(outputFolder, personFileFilter, bwPersons,"HHNR,PNR,HHS,LQ,POP,INC,HPNR,AGE,GEN,REL,EMPL,SCH");
	}
	
	
	public static void aggregateOutput(File folder, FileFilter filter, BufferedWriter bw, String header){		
		try{
			bw.write(header);
			bw.newLine();
			File[] householdFiles = folder.listFiles(filter);
			BufferedReader br = null;
			for(File f : householdFiles){
				String subplace = f.getName().substring(f.getName().indexOf("_")+1, f.getName().indexOf("."));
				br = IOUtils.getBufferedReader(f.getAbsolutePath());
				try{
					String line = br.readLine(); /* Header */
					while((line = br.readLine()) != null && line.length() > 0){
						bw.write(line);
						bw.write(",");
						bw.write(subplace);
						bw.newLine();
					}
				} finally{
					try {
						br.close();
					} catch (IOException e) {
						throw new RuntimeException("Could not close BufferedReader for " + f.getAbsolutePath());
					}
				}
				f.delete();
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter for aggregate output.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter for aggregate output.");
			}
		}
	}

	
	/**
	 * This implementation specific. The Iterative Proportional Fitting used
	 * in South Africa has used 15 fields. The first two fields contain the zone
	 * id and zone description, the next eight (8) were the individual control 
	 * totals for four (4) race and two (2) gender classes. The last seven (7)
	 * fields were the household control totals for seven income classes.
	 * @param filename
	 * @return
	 */
	private Map<String, String> parseControlTotals(String filename){
		LOG.info("Parsing control totals from " + filename);
		Map<String, String> map = new TreeMap<>();
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = br.readLine(); /* Header. */
			while((line = br.readLine()) != null){
				String[] sa = line.split(",");
				if(sa.length == 17){
					map.put(sa[0], line);					
				} else{
					LOG.warn("Zone " + sa[0] + " does not have 17 field - zone will be ignored.");
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not read from BufferedReader for " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedReader for " + filename);
			}
		}
		LOG.info("Done parsing control totals (" + map.size() + " found)");
		return map;
	}

}


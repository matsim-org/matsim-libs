package playground.southafrica.population.algorithms.pythonR;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.FileUtils;

public class PythonRIntegratorRunnable implements Runnable {
	private final Logger log = Logger.getLogger(PythonRIntegratorRunnable.class);
	private Counter counter;
	private String folder;
	private String zone;
	private String outputFolder;
	
	public PythonRIntegratorRunnable(Counter counter, String folder, String zone, String outputFolder) {
		this.counter = counter;
		this.folder = folder;
		this.zone = zone;
		this.outputFolder = outputFolder;
	}

	@Override
	public void run() {
		/* Read the group control totals */
		BufferedReader br = IOUtils.getBufferedReader(folder + "groupCT.dat");
		int totalNumberOfGroups = 0;
		try{
			String line = br.readLine(); /* Header */
			while((line = br.readLine()) != null){
				String[] sa = line.split("\t");
				if(sa.length == 2){
					totalNumberOfGroups += Math.round(Double.parseDouble(sa[1]));
				}
			}
		} catch (NumberFormatException e) {
			throw new RuntimeException("NumberFormatException reading group control totals for zone " + zone);
		} catch (IOException e) {
			throw new RuntimeException("Could not read group control totals for zone " + zone);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close the BufferedReader for zone " + zone);
			}
		}
		
		/* Do the run. */
		/* Only perform the computationally expensive fitting IF there is a population to generate. */
		if(totalNumberOfGroups > 0){
			log.info("   zone " + zone + "; " + totalNumberOfGroups);
			@SuppressWarnings("unused")
			String line;
			
			/* Execute python.
			 * Output folders are created in the 'current' folder, each denoting
			 * an iteration.*/
			Process pProcess = null;
			try {
				String pythonScript = "python " + folder + "main.py "  + folder;
				pProcess = Runtime.getRuntime().exec(pythonScript);
				BufferedReader in = new BufferedReader(
						new InputStreamReader(pProcess.getInputStream()));  
				line = null;  
				while ((line = in.readLine()) != null) {
//					log.info("  | " + line);
				}  	
//				log.info("  |______________________________");
			} catch (IOException e) {
				throw new RuntimeException("Could not run python for zone " + zone);
			} 
//			finally{
//				pProcess.destroy();
//			}

			/* Move the last iteration's CSV file into position for R. */
			File lastIterationWeights = getLastIterationFile();
			if(lastIterationWeights != null){
				File weightFile = new File(folder + "weights.csv");
				try {
					FileUtils.copyFile(lastIterationWeights, weightFile);
				} catch (IOException e) {
					log.warn("IOException - Couldn't copy output weights " + lastIterationWeights.getAbsolutePath());
				}
				
				/* Execute R. */
				Process rProcess = null;
				try {
					String rScript = "Rscript " + folder + "sampling.R " + totalNumberOfGroups + " " + folder;
					rProcess = Runtime.getRuntime().exec( rScript );
					BufferedReader in = new BufferedReader(
							new InputStreamReader(rProcess.getInputStream()),500);  
					line = null;  
					while ((line = in.readLine()) != null) {
//					log.info("  | " + line);
					}  	
//				log.info("  |______________________________");
				} catch (IOException e) {
					throw new RuntimeException("Could not run R for zone " + zone);
				}
//				finally{
//					rProcess.destroy();
//				}
				
				/* Move the R output, a population CSV file to some output folder
				 * for later aggregation. Also copy the weight file. It is 
				 * computationally EXPENSIVE and need not be done more than once.
				 * The same weights file can be used to sample different 
				 * populations from. */
				File fileHousehold = new File(folder + "homesynth_hh.csv");
				File filePersons = new File(folder + "synth_pax.csv");
				
				File output1 = new File(outputFolder + "hh_" + zone + ".csv");
				File output2 = new File(outputFolder + "pax_" + zone + ".csv");
				File output3 = new File(outputFolder + "weights_" + zone + ".csv");
				try {
					FileUtils.copyFile(fileHousehold, output1);
					FileUtils.copyFile(filePersons, output2);
					FileUtils.copyFile(weightFile, output3);
					
					/* Clean up folder, but ONLY once the output was successfully copied. */
					FileUtils.delete(new File(folder));
				} catch (IOException e) {
					log.error("IOException in copying population files for zone " + zone + " ignored!!");
					e.printStackTrace();
				}
			} else{
				log.error("Weight file is null: zone " + zone + "; " + folder);
			}
		}
		
		counter.incCounter();
	}
	

	public File getLastIterationFile(){
		/* Get the output folder. */
		File[] fa = (new File(folder)).listFiles(new pythonOutputfilter());
		if(fa.length > 1){
			log.warn("    Found more than one output folder for zone " + zone);
			log.warn("    Using only " + fa[0].getAbsolutePath());
		}

		/* Find the last iteration file */
		int maxValue = Integer.MIN_VALUE;
		File[] iterationfiles = fa[0].listFiles();
		if(iterationfiles.length == 0){
			log.warn("There are no weight files for any iteration!");
		}
		
		for(File f : iterationfiles){
			String[] sa = f.getName().split("-");
			if(sa.length == 3){
				String ss = sa[2];
				int it = Integer.parseInt(ss.substring(0, ss.indexOf(".")));
				maxValue = Math.max(maxValue, it);
			}			
		}
		for(File f : iterationfiles){
			String[] sa = f.getName().split("-");
			if(sa.length == 3){
				String ss = sa[2];
				int it = Integer.parseInt(ss.substring(0, ss.indexOf(".")));
				if(it == maxValue){
					return f;
				}
			}
		}
		return null;
	}


	private class pythonOutputfilter implements FileFilter{
		@Override
		public boolean accept(File pathname) {
			String name = pathname.getName();
			if(pathname.isDirectory() && name.contains("ml_ipf")){
				return true;
			}
			return false;
		}
	}

}

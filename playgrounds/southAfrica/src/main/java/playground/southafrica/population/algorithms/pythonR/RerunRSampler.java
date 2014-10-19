package playground.southafrica.population.algorithms.pythonR;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;

public class RerunRSampler {
	private final static Logger LOG = Logger.getLogger(RerunRSampler.class);

	/**
	 * Given that a weights file exist for each subplace, this code will 
	 * regenerate a population without refitting the weights.
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(RerunRSampler.class.toString(), args);
		String inputFolder = args[0];
		String outputFolder = args[1];
		String referenceFile = args[2];
		String controlTotalsFile = args[3];
		String rFile = args[4];
		
		/* Parse the control totals */
		Map<Id<MyZone>, Integer> householdCountMap = parseHouseholdCountFromControlTotals(controlTotalsFile);		
		
		/* Get all the subplaces from their weights file. */
		FileFilter weightFileFilter = new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				String filename = pathname.getName();
				if(filename.startsWith("weights") && filename.endsWith(".csv")){
					return true;
				}
				return false;
			}
		};
		File[] weightFiles = (new File(inputFolder)).listFiles(weightFileFilter);
		
		/* Create for each subplace its population. */
		Counter counter = new Counter(" subplaces # ");
		for(File f : weightFiles){
			String zoneCode = f.getName().substring(f.getName().indexOf("_")+1, f.getName().indexOf("."));
			Id<MyZone> zoneId = Id.create(zoneCode, MyZone.class);
			if(householdCountMap.containsKey(zoneId)){
				int numberOfHouseholdsToCreate = householdCountMap.get(zoneId);
				String script = "Rscript " + rFile + " " +
						numberOfHouseholdsToCreate + " " +
						referenceFile + " " +
						f.getAbsolutePath() + " " +
						outputFolder + "pax_" + zoneCode + ".csv";
				try {
					Process rProcess = Runtime.getRuntime().exec(script);
					BufferedReader in = new BufferedReader(
							new InputStreamReader(rProcess.getInputStream()),500);  
					String line = null;  
					while ((line = in.readLine()) != null) {
					}  	
					
				} catch (IOException e) {
					LOG.error("Could not execute R for " + zoneCode + "... zone ignored") ;
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		/* Aggregate the output. */
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
		BufferedWriter bwPersons = IOUtils.getBufferedWriter(outputFolder + "persons.txt.gz");
		PythonRIntegrator.aggregateOutput(new File(outputFolder), personFileFilter, bwPersons,"HHNR,PNR,HHS,LQ,POP,INC,HPNR,AGE,GEN,REL,EMPL,SCH,SUBPLACE");
		
		Header.printFooter();
	}
	
	/**
	 * Assuming the control totals file have 17 columns (tab-separated), of which 
	 * columns indices 10 - 16 contain the number of households in each income class.
	 * @param file
	 * @return
	 */
	public static Map<Id<MyZone>,Integer> parseHouseholdCountFromControlTotals(String file){
		Map<Id<MyZone>, Integer> map = new HashMap<>();
		BufferedReader br = IOUtils.getBufferedReader(file);
		try{
			String line = br.readLine(); /* Header */
			while((line = br.readLine()) != null){
				int count = 0;
				String[] sa = line.split("\t");
				if(sa.length == 17){
					for(int i = 10; i < 17; i++){
						count += ((int) Math.round(Double.parseDouble(sa[i])));
					}
				}
				map.put(Id.create(sa[0], MyZone.class), count);
			}
		} catch (IOException e) {
			throw new RuntimeException("Couldn't read from BufferedReader " + file);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				throw new RuntimeException("Couldn't close Bufferedreader for " + file);
			}
		}
		return map;
	}

}

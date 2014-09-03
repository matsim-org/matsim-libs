/**
 * 
 */
package playground.southafrica.projects.treasury2014;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

import playground.southafrica.population.HouseholdSampler;
import playground.southafrica.utilities.Header;

/**
 * Class to automate, in one script, generate 10% samples for all the study 
 * areas covered in the Treasury project.
 *  
 * @author jwjoubert
 */
public class GenerateTenPercentSamples {
	private final static Logger LOG = Logger.getLogger(GenerateTenPercentSamples.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GenerateTenPercentSamples.class.toString(), args);
		run(args);																																												
		Header.printFooter();
	}
	
	public static void run(String[] args){
		String inputFolder = args[0];																																																																																																																																																																																																																																																																										
		String outputFolder = args[1];
		Long seed = Long.parseLong(args[2]);																																																																															
		
		MatsimRandom.reset(seed);
		
		for(StudyAreas area : StudyAreas.values()){
			LOG.info("----------------------------------------------------------");																																																																																																																									  
			LOG.info(" Generating 10% sample for " + area.getName());
			LOG.info("----------------------------------------------------------");
			String areaInputFolder = inputFolder + (inputFolder.endsWith("/") ? "" : "/") + area.getName() + "/";
			String areaOutputFolder = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + area.getName() + "/";
			/* Warn if output folder already exists. */
			File f = new File(areaOutputFolder);
			if(f.exists()){
				throw new RuntimeException(areaOutputFolder + "already exists and will be not be deleted!");
			}
			if(f.mkdirs()){
				LOG.info("Area directory successfully created at " + areaOutputFolder);
			} else{
				throw new RuntimeException("Could not create area directory " + areaOutputFolder);
			}
	
			HouseholdSampler hhs = new HouseholdSampler(MatsimRandom.getRandom());
			hhs.sampleHouseholds(areaInputFolder, 0.10);
			try {
				hhs.writeSample(areaOutputFolder);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("May not overwrite any population files!");
			}
			hhs = null;
			LOG.info("==> done with " + area.getName());
		}
	}

}

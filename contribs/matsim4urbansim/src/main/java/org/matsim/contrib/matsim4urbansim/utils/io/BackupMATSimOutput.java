package org.matsim.contrib.matsim4urbansim.utils.io;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.matsim4urbansim.config.ConfigurationUtils;
import org.matsim.contrib.matsim4urbansim.config.M4UConfigUtils;
import org.matsim.contrib.matsim4urbansim.config.modules.M4UControlerConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;

public class BackupMATSimOutput {
	
	// logger
	private static final Logger log = Logger.getLogger(BackupMATSimOutput.class);
	
	public static final String OUTPUT_CONFIG_FILE_NAME = "output_config.xml.gz";
	public static final String OUTPUT_SCORESTATS_TXT = "scorestats.txt";
	public static final String OUTPUT_SCORESTATS_PNG = "scorestats.png";
	public static final String OUTPUT_TRAVELDISTANCESTATS_TXT = "traveldistancestats.txt";
	public static final String OUTPUT_TRAVELDISTANCESTATS_PNG = "traveldistancestats.png";
	public static final String OUTPUT_STOPWATCH = "stopwatch.txt";
	
	/**
	 * Saving UrbanSim and MATSim results for current run in a backup directory ...
	 */
	public static void saveRunOutputs(Scenario scenario) {
		log.info("Saving UrbanSim and MATSim outputs ...");

//		M4UControlerConfigModuleV3 m4ucModule = M4UConfigUtils.getMATSim4UrbaSimControlerConfigAndPossiblyConvert(scenario.getConfig()) ;
		UrbanSimParameterConfigModuleV3 uspModule = M4UConfigUtils.getUrbanSimParameterConfigAndPossiblyConvert(scenario.getConfig()) ;
//		AccessibilityConfigGroup acm = ConfigUtils.addOrGetModule(scenario.getConfig(), AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class) ;
		int currentYear = uspModule.getYear();
		
		String saveDirectory = "run" + currentYear;
		String savePath = Paths.checkPathEnding( uspModule.getMATSim4OpusBackup() + saveDirectory );
		// copy all files from matsim4opus/tmp to matsim4opus/backup
		// FileCopy.copyTree(InternalConstants.MATSIM_4_OPUS_TEMP, savePath);
		File saveDir = new File(savePath);
		if(!saveDir.exists())
			if(!saveDir.mkdirs())
				log.error("Creating the backup directory " + savePath + " failed!");
		
		throw new RuntimeException("BackupMATSimOutput functionality is disabled.  If this is needed, "
				+ "please re-implement using FileUtils.copyDirectory, see "
				+ "http://stackoverflow.com/questions/5368724/how-to-copy-a-folder-and-all-its-subfolders-and-files-into-another-folder") ;
		
//		// backup files from matsim output
//		try {
//			// backup plans files
//			FileCopy.fileCopy( new File(uspModule.getMATSim4OpusOutput() + InternalConstants.GENERATED_PLANS_FILE_NAME) , new File(savePath + InternalConstants.GENERATED_PLANS_FILE_NAME) );
//			// backup matsim config file
//			FileCopy.fileCopy( new File(uspModule.getMATSim4OpusOutput() + OUTPUT_CONFIG_FILE_NAME) , new File(savePath + OUTPUT_CONFIG_FILE_NAME) );
//			// backup score stats
//			FileCopy.fileCopy( new File(uspModule.getMATSim4OpusOutput() + OUTPUT_SCORESTATS_TXT) , new File(savePath + OUTPUT_SCORESTATS_TXT) );
//			FileCopy.fileCopy( new File(uspModule.getMATSim4OpusOutput() + OUTPUT_SCORESTATS_PNG) , new File(savePath + OUTPUT_SCORESTATS_PNG) );
//			FileCopy.fileCopy( new File(uspModule.getMATSim4OpusOutput() + OUTPUT_TRAVELDISTANCESTATS_TXT) , new File(savePath + OUTPUT_TRAVELDISTANCESTATS_TXT) );
//			FileCopy.fileCopy( new File(uspModule.getMATSim4OpusOutput() + OUTPUT_TRAVELDISTANCESTATS_PNG) , new File(savePath + OUTPUT_TRAVELDISTANCESTATS_PNG) );
//			FileCopy.fileCopy( new File(uspModule.getMATSim4OpusOutput() + OUTPUT_STOPWATCH) , new File(savePath + OUTPUT_STOPWATCH) );
//			// backup last iteration
//			int iteration = ((ControlerConfigGroup) scenario.getConfig().getModule(ControlerConfigGroup.GROUP_NAME)).getLastIteration();
//			FileCopy.copyTree(uspModule.getMATSim4OpusOutput() + "ITERS/it."+iteration, savePath + "ITERS/it."+iteration);
//
//			// backup zone csv file (feedback for UrbanSim)
//			if(new File(uspModule.getMATSim4OpusTemp() + UrbanSimZoneCSVWriterV2.FILE_NAME).exists())
//				FileCopy.fileCopy(new File(uspModule.getMATSim4OpusTemp()  + UrbanSimZoneCSVWriterV2.FILE_NAME), new File(savePath + UrbanSimZoneCSVWriterV2.FILE_NAME) );
//			// backup parcel csv file (feedback for UrbanSim)
//			if(new File(uspModule.getMATSim4OpusTemp() + UrbanSimParcelCSVWriter.FILE_NAME).exists())
//				FileCopy.fileCopy(new File(uspModule.getMATSim4OpusTemp()  + UrbanSimParcelCSVWriter.FILE_NAME), new File(savePath + UrbanSimParcelCSVWriter.FILE_NAME) );
//			// backup person csv file (feedback for UrbanSim)
//			if(new File(uspModule.getMATSim4OpusTemp()  + UrbanSimPersonCSVWriter.FILE_NAME).exists())
//				FileCopy.fileCopy(new File(uspModule.getMATSim4OpusTemp()  + UrbanSimPersonCSVWriter.FILE_NAME), new File(savePath + UrbanSimPersonCSVWriter.FILE_NAME) );
//			// backup travel_data csv file (feedback for UrbanSim)
//			if(new File(uspModule.getMATSim4OpusTemp()  + Zone2ZoneImpedancesControlerListener.FILE_NAME).exists())
//				FileCopy.fileCopy(new File(uspModule.getMATSim4OpusTemp() + Zone2ZoneImpedancesControlerListener.FILE_NAME), new File(savePath + Zone2ZoneImpedancesControlerListener.FILE_NAME) );
//			
//			// backup plotting files free speed
//			String fileName = Labels.FREESPEED_FILENAME + (double)acm.getCellSizeCellBasedAccessibility() + InternalConstants.FILE_TYPE_TXT;
//			if( new File(uspModule.getMATSim4OpusTemp()  + fileName).exists() )
//				FileCopy.fileCopy(new File(uspModule.getMATSim4OpusTemp()  + fileName), new File(savePath + fileName) );
//			// backup plotting files for car
//			fileName = Labels.CAR_FILENAME + (double)acm.getCellSizeCellBasedAccessibility() + InternalConstants.FILE_TYPE_TXT;
//			if( new File(uspModule.getMATSim4OpusTemp()  + fileName).exists() )
//				FileCopy.fileCopy(new File(uspModule.getMATSim4OpusTemp()  + fileName), new File(savePath + fileName) );
//			// backup plotting files for bike
//			fileName = Labels.BIKE_FILENAME + (double)acm.getCellSizeCellBasedAccessibility() + InternalConstants.FILE_TYPE_TXT;
//			if( new File(uspModule.getMATSim4OpusTemp()  + fileName).exists() )
//				FileCopy.fileCopy(new File(uspModule.getMATSim4OpusTemp()  + fileName), new File(savePath + fileName) );
//			// backup plotting files for walk
//			fileName = Labels.WALK_FILENAME + (double)acm.getCellSizeCellBasedAccessibility() + InternalConstants.FILE_TYPE_TXT;
//			if( new File(uspModule.getMATSim4OpusTemp()  + fileName).exists() )
//				FileCopy.fileCopy(new File(uspModule.getMATSim4OpusTemp()  + fileName), new File(savePath + fileName) );
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		log.info("Saving UrbanSim and MATSim outputs done!");
	}
	
	/**
	 * Preparing hot start: Copying recent matsim plans file to a specified location (Matsim config).
	 * 						Matsim will check this location for plans file on run and activates hot start if the plans file is there
	 */
	public static void prepareHotStart(Scenario scenario){
		
		M4UControlerConfigModuleV3 module = ConfigurationUtils.getMATSim4UrbaSimControlerConfigModule(scenario);
		UrbanSimParameterConfigModuleV3 uspModule = M4UConfigUtils.getUrbanSimParameterConfigAndPossiblyConvert(scenario.getConfig()) ;
		
		String hotStartFile = module.getHotStartPlansFileLocation().trim();
		if(exists(hotStartFile)){
			
			String plansFile = uspModule.getMATSim4OpusOutput() + InternalConstants.GENERATED_PLANS_FILE_NAME;
			try{
				log.info("Preparing hot start for next MATSim run ...");
				FileCopy.fileCopy(new File(plansFile), new File(hotStartFile));
			} catch (Exception e) {
				log.error("Error while copying plans file, i. e. hot start will not work!");
				e.printStackTrace();
			}
			
			log.info("Hot start preparation successful!");
		}else{
			log.info("can not prepare hotStart. hotstart-file does not exist: " + hotStartFile);
		}
	}

	/**
	 * @param hotStartFile
	 * @return
	 */
	private static boolean exists(String hotStartFile) {
		if(hotStartFile.endsWith(".xml") || hotStartFile.endsWith(".xml.gz")){
			File f = new File(hotStartFile);
			String fileName = f.getName();
			String dir = f.getAbsolutePath().replaceAll(fileName, "");
			File directory = new File(dir);
			if(directory.exists()){
				return true;
			}
		}
		return false;
	}
	
//	/**
//	 * This is experimental
//	 * Removes UrbanSim output files for MATSim, since they are 
//	 * saved by performing saveRunOutputs() in a previous step.
//	 */
//	private static void cleanUrbanSimOutput(ScenarioImpl scenario){
//		
//		UrbanSimParameterConfigModuleV3 uspModule = M4UConfigUtils.getUrbanSimParameterConfigAndPossiblyConvert(scenario.getConfig()) ;
//		log.info("Cleaning MATSim4Opus temp directory (" + uspModule.getMATSim4OpusTemp() + ") from UrbanSim output." );
//		
//		
//		ArrayList<File> fileNames = FileCopy.listAllFiles(new File(uspModule.getMATSim4OpusTemp()), Boolean.FALSE);
//		Iterator<File> fileNameIterator = fileNames.iterator();
//		while(fileNameIterator.hasNext()){
//			File f = fileNameIterator.next();
//			try {
//				if(f.getCanonicalPath().endsWith(".tab") || f.getCanonicalPath().endsWith(".meta")){
//					log.info("Removing " + f.getCanonicalPath());
//					f.delete();
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//				log.info("While removing UrbanSim output an IO error occured. This is not critical.");
//			}
//		}
//		log.info("Cleaning MATSim4Opus temp directory done!");
//	}

}

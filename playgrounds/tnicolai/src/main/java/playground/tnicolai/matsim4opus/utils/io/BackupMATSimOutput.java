package playground.tnicolai.matsim4opus.utils.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.core.scenario.ScenarioImpl;

import playground.tnicolai.matsim4opus.config.ConfigurationModule;
import playground.tnicolai.matsim4opus.config.MATSim4UrbanSimControlerConfigModuleV3;
import playground.tnicolai.matsim4opus.config.UrbanSimParameterConfigModuleV3;
import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.matsim4urbansim.AccessibilityControlerListenerImpl;
import playground.tnicolai.matsim4opus.matsim4urbansim.Zone2ZoneImpedancesControlerListener;
import playground.tnicolai.matsim4opus.utils.io.writer.UrbanSimParcelCSVWriter;
import playground.tnicolai.matsim4opus.utils.io.writer.UrbanSimPersonCSVWriter;
import playground.tnicolai.matsim4opus.utils.io.writer.UrbanSimZoneCSVWriterV2;

public class BackupMATSimOutput {
	
	// logger
	private static final Logger log = Logger.getLogger(BackupMATSimOutput.class);
	
	public static void runBackup(ScenarioImpl scenario){
		
		UrbanSimParameterConfigModuleV3 module = ConfigurationModule.getUrbanSimParameterConfigModule(scenario);
		
		if(module == null)
			log.error("UrbanSimParameterConfigModule module is null. Can't determine if backup option is activated. No backup will be performed.");
		else if( module.isBackup() ){
			// saving results from current run
			saveRunOutputs(scenario);
			// cleanUrbanSimOutput(scenario); // tnicolai dec'12 not needed
		}
	}
	
	/**
	 * Saving UrbanSim and MATSim results for current run in a backup directory ...
	 */
	private static void saveRunOutputs(ScenarioImpl scenario) {
		log.info("Saving UrbanSim and MATSim outputs ...");

		MATSim4UrbanSimControlerConfigModuleV3 m4ucModule = ConfigurationModule.getMATSim4UrbaSimControlerConfigModule(scenario);
		UrbanSimParameterConfigModuleV3 uspModule = ConfigurationModule.getUrbanSimParameterConfigModule(scenario);
		int currentYear = uspModule.getYear();
		
		String saveDirectory = "run" + currentYear;
		String savePath = Paths.checkPathEnding( InternalConstants.MATSIM_4_OPUS_BACKUP + saveDirectory );
		// copy all files from matsim4opus/tmp to matsim4opus/backup
		// FileCopy.copyTree(InternalConstants.MATSIM_4_OPUS_TEMP, savePath);
		File saveDir = new File(savePath);
		if(!saveDir.exists())
			if(!saveDir.mkdirs())
				log.error("Creating the backup directory " + savePath + " failed!");
		
		// backup files from matsim output
		try {
			// backup plans files
			FileCopy.fileCopy( new File(InternalConstants.MATSIM_4_OPUS_OUTPUT + InternalConstants.GENERATED_PLANS_FILE_NAME) , new File(savePath + InternalConstants.GENERATED_PLANS_FILE_NAME) );
			// backup matsim config file
			FileCopy.fileCopy( new File(InternalConstants.MATSIM_4_OPUS_OUTPUT + InternalConstants.OUTPUT_CONFIG_FILE_NAME) , new File(savePath + InternalConstants.OUTPUT_CONFIG_FILE_NAME) );
			
			// backup zone csv file (feedback for UrbanSim)
			if(new File(InternalConstants.MATSIM_4_OPUS_TEMP + UrbanSimZoneCSVWriterV2.FILE_NAME).exists())
				FileCopy.fileCopy(new File(InternalConstants.MATSIM_4_OPUS_TEMP + UrbanSimZoneCSVWriterV2.FILE_NAME), new File(savePath + UrbanSimZoneCSVWriterV2.FILE_NAME) );
			// backup parcel csv file (feedback for UrbanSim)
			if(new File(InternalConstants.MATSIM_4_OPUS_TEMP + UrbanSimParcelCSVWriter.FILE_NAME).exists())
				FileCopy.fileCopy(new File(InternalConstants.MATSIM_4_OPUS_TEMP + UrbanSimParcelCSVWriter.FILE_NAME), new File(savePath + UrbanSimParcelCSVWriter.FILE_NAME) );
			// backup person csv file (feedback for UrbanSim)
			if(new File(InternalConstants.MATSIM_4_OPUS_TEMP + UrbanSimPersonCSVWriter.FILE_NAME).exists())
				FileCopy.fileCopy(new File(InternalConstants.MATSIM_4_OPUS_TEMP + UrbanSimPersonCSVWriter.FILE_NAME), new File(savePath + UrbanSimPersonCSVWriter.FILE_NAME) );
			// backup travel_data csv file (feedback for UrbanSim)
			if(new File(InternalConstants.MATSIM_4_OPUS_TEMP + Zone2ZoneImpedancesControlerListener.FILE_NAME).exists())
				FileCopy.fileCopy(new File(InternalConstants.MATSIM_4_OPUS_TEMP + Zone2ZoneImpedancesControlerListener.FILE_NAME), new File(savePath + Zone2ZoneImpedancesControlerListener.FILE_NAME) );
			
			// backup plotting files free speed
			String fileName = AccessibilityControlerListenerImpl.FREESEED_FILENAME + (double)m4ucModule.getCellSizeCellBasedAccessibility() + InternalConstants.FILE_TYPE_TXT;
			if( new File(InternalConstants.MATSIM_4_OPUS_TEMP + fileName).exists() )
				FileCopy.fileCopy(new File(InternalConstants.MATSIM_4_OPUS_TEMP + fileName), new File(savePath + fileName) );
			// backup plotting files for car
			fileName = AccessibilityControlerListenerImpl.CAR_FILENAME + (double)m4ucModule.getCellSizeCellBasedAccessibility() + InternalConstants.FILE_TYPE_TXT;
			if( new File(InternalConstants.MATSIM_4_OPUS_TEMP + fileName).exists() )
				FileCopy.fileCopy(new File(InternalConstants.MATSIM_4_OPUS_TEMP + fileName), new File(savePath + fileName) );
			// backup plotting files for bike
			fileName = AccessibilityControlerListenerImpl.BIKE_FILENAME + (double)m4ucModule.getCellSizeCellBasedAccessibility() + InternalConstants.FILE_TYPE_TXT;
			if( new File(InternalConstants.MATSIM_4_OPUS_TEMP + fileName).exists() )
				FileCopy.fileCopy(new File(InternalConstants.MATSIM_4_OPUS_TEMP + fileName), new File(savePath + fileName) );
			// backup plotting files for walk
			fileName = AccessibilityControlerListenerImpl.WALK_FILENAME + (double)m4ucModule.getCellSizeCellBasedAccessibility() + InternalConstants.FILE_TYPE_TXT;
			if( new File(InternalConstants.MATSIM_4_OPUS_TEMP + fileName).exists() )
				FileCopy.fileCopy(new File(InternalConstants.MATSIM_4_OPUS_TEMP + fileName), new File(savePath + fileName) );
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("Saving UrbanSim and MATSim outputs done!");
	}
	
	/**
	 * Preparing hot start: Copying recent matsim plans file to a specified location (Matsim config).
	 * 						Matsim will check this location for plans file on run and activates hot start if the plans file is there
	 */
	public static void prepareHotStart(ScenarioImpl scenario){
		
		MATSim4UrbanSimControlerConfigModuleV3 module = ConfigurationModule.getMATSim4UrbaSimControlerConfigModule(scenario);
		
		if(!module.getHotStartTargetLocation().equals("")){
			
			String plansFile = InternalConstants.MATSIM_4_OPUS_OUTPUT + InternalConstants.GENERATED_PLANS_FILE_NAME;
			try{
				log.info("Preparing hot start for next MATSim run ...");
				FileCopy.fileCopy(new File(plansFile), new File(module.getHotStartTargetLocation()));
			} catch (Exception e) {
				log.error("Error while copying plans file, i. e. hot start will not work!");
				e.printStackTrace();
			}
			
			log.info("Hot start preparation successful!");
		}
	}
	
	/**
	 * This is experimental
	 * Removes UrbanSim output files for MATSim, since they are 
	 * saved by performing saveRunOutputs() in a previous step.
	 */
	private static void cleanUrbanSimOutput(ScenarioImpl scenario){
		
		log.info("Cleaning MATSim4Opus temp directory (" + InternalConstants.MATSIM_4_OPUS_TEMP + ") from UrbanSim output." );
		
		ArrayList<File> fileNames = FileCopy.listAllFiles(new File(InternalConstants.MATSIM_4_OPUS_TEMP), Boolean.FALSE);
		Iterator<File> fileNameIterator = fileNames.iterator();
		while(fileNameIterator.hasNext()){
			File f = fileNameIterator.next();
			try {
				if(f.getCanonicalPath().endsWith(".tab") || f.getCanonicalPath().endsWith(".meta")){
					log.info("Removing " + f.getCanonicalPath());
					f.delete();
				}
			} catch (IOException e) {
				e.printStackTrace();
				log.info("While removing UrbanSim output an IO error occured. This is not critical.");
			}
		}
		log.info("Cleaning MATSim4Opus temp directory done!");
	}

}

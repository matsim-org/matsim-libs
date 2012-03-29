package playground.tnicolai.matsim4opus.utils.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.core.scenario.ScenarioImpl;

import playground.tnicolai.matsim4opus.config.ConfigurationModule;
import playground.tnicolai.matsim4opus.config.MATSim4UrbaSimControlerConfigModule;
import playground.tnicolai.matsim4opus.config.UrbanSimParameterConfigModule;
import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.DateUtil;

public class BackupRun {
	
	// logger
	private static final Logger log = Logger.getLogger(BackupRun.class);
	
	public static void runBackup(ScenarioImpl scenario){
		
//		String value = scenario.getConfig().getParam(Constants.URBANSIM_PARAMETER, Constants.BACKUP_RUN_DATA_PARAM);
		UrbanSimParameterConfigModule module = ConfigurationModule.getUrbanSimParameterConfigModule(scenario);
		
		if( module.isBackup() ){
			// saving results from current run
			saveRunOutputs(scenario);
			prepareHotStart(scenario);
			cleanUrbanSimOutput(scenario);
		}
	}
	
	/**
	 * Saving UrbanSim and MATSim results for current run in a backup directory ...
	 */
	private static void saveRunOutputs(ScenarioImpl scenario) {
		log.info("Saving UrbanSim and MATSim outputs ...");
		
//		String currentYear = scenario.getConfig().getParam(Constants.URBANSIM_PARAMETER, Constants.YEAR);
		UrbanSimParameterConfigModule module = ConfigurationModule.getUrbanSimParameterConfigModule(scenario);
		int currentYear = module.getYear();
		int lastIteration = scenario.getConfig().controler().getLastIteration();
		
		String saveDirectory = "run" + currentYear + "-" + DateUtil.now();
		String savePath = Paths.checkPathEnding( Constants.MATSIM_4_OPUS_BACKUP + saveDirectory );
		// copy all files from matsim4opus/tmp to matsim4opus/backup
		FileCopy.copyTree(Constants.MATSIM_4_OPUS_TEMP, savePath);
		// copy all files from matsim4opus/output/ITERS/it.XX to matsim4opus/backup
		FileCopy.copyTree(Constants.MATSIM_4_OPUS_OUTPUT + "ITERS/it." + lastIteration + "/" , savePath);
		
		// backup files from matsim output
		try {
			// backup plans files
			FileCopy.fileCopy( new File(Constants.MATSIM_4_OPUS_OUTPUT + Constants.GENERATED_PLANS_FILE_NAME) , new File(savePath + Constants.GENERATED_PLANS_FILE_NAME) );
			// backup score stats file
			FileCopy.fileCopy( new File(Constants.MATSIM_4_OPUS_OUTPUT + Constants.SCORESTATS_FILE_NAME) , new File(savePath + Constants.SCORESTATS_FILE_NAME) );
			// backup travel distance stats files
			FileCopy.fileCopy( new File(Constants.MATSIM_4_OPUS_OUTPUT + Constants.TRAVELDISTANCESSTAT_FILE_NAME) , new File(savePath + Constants.TRAVELDISTANCESSTAT_FILE_NAME) );
			// backup logfiles
			FileCopy.fileCopy( new File(Constants.MATSIM_4_OPUS_OUTPUT + Constants.LOG_FILE_NAME) , new File(savePath + Constants.LOG_FILE_NAME) );
			FileCopy.fileCopy( new File(Constants.MATSIM_4_OPUS_OUTPUT + Constants.LOG_FILE_WARNINGS_ERRORS_NAME) , new File(savePath + Constants.LOG_FILE_WARNINGS_ERRORS_NAME) );
			// backup matsim config file
			FileCopy.fileCopy( new File(Constants.MATSIM_4_OPUS_OUTPUT + Constants.OUTPUT_CONFIG_FILE_NAME) , new File(savePath + Constants.OUTPUT_CONFIG_FILE_NAME) );
			// backup matsim network
			FileCopy.fileCopy( new File(Constants.MATSIM_4_OPUS_OUTPUT + Constants.OUTPUT_NETWORK_FILE_NAME) , new File(savePath + Constants.OUTPUT_NETWORK_FILE_NAME) );
			// backup matsim 
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("Saving UrbanSim and MATSim outputs done!");
	}
	
	/**
	 * Preparing hot start: Copying recent matsim plans file to a specified location (Matsim config).
	 * 						Matsim will check this location for plans file on run and activates hot start if the plans file is there
	 */
	private static void prepareHotStart(ScenarioImpl scenario){
		
//		String targetLocationHotStartFile = scenario.getConfig().getParam(Constants.URBANSIM_PARAMETER, Constants.TARGET_LOCATION_HOT_START_PLANS_FILE);
		MATSim4UrbaSimControlerConfigModule module = ConfigurationModule.getMATSim4UrbaSimControlerConfigModule(scenario);
		
		if(!module.getHotStartTargetLocation().equals("")){
			
			String plansFile = Constants.MATSIM_4_OPUS_OUTPUT + Constants.GENERATED_PLANS_FILE_NAME;
			
			log.info("Preparing hot start for next MATSim run ...");
			boolean success = FileCopy.moveFileOrDirectory(plansFile, module.getHotStartTargetLocation());
			if(success)
				log.info("Hot start preparation successful!");
			else
				log.error("Error while moving plans file, i. e. hot start will not work!");
		}
	}
	
	/**
	 * This is experimental
	 * Removes UrbanSim output files for MATSim, since they are 
	 * saved by performing saveRunOutputs() in a previous step.
	 */
	private static void cleanUrbanSimOutput(ScenarioImpl scenario){
		
		log.info("Cleaning MATSim4Opus temp directory (" + Constants.MATSIM_4_OPUS_TEMP + ") from UrbanSim output." );
		
		ArrayList<File> fileNames = FileCopy.listAllFiles(new File(Constants.MATSIM_4_OPUS_TEMP), Boolean.FALSE);
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

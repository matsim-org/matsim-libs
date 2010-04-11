package playground.christoph.controler;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.Module;
import org.matsim.core.gbl.Gbl;

public class BatchRun {
	
	private static final Logger log = Logger.getLogger(BatchRun.class);
	
	//protected static String[] knowledgeFactors = {"1.05", "1.10", "1.15", "1.20", "1.25", "1.50", "1.75", "2.00", "2.50", "3.00", "full"}; 
	protected static String[] knowledgeFactors = {"2.0"};
	//protected static double[] probabilityFactors = {0.05, 0.15, 0.25, 0.35, 0.45, 0.55, 0.65, 0.75, 0.85, 0.95};
	protected static double[] probabilityFactors = {0.65};
		
/*	// no replanning, initial replanning, act end replanning, leave link replanning
	protected static int[][] Versuchsplan = {{4, 2, 3, 1},
											 {2, 3, 4, 1},
											 {1, 3, 5, 1},
											 {5, 1, 1, 3},
											 {4, 2, 2, 2},
											 {4, 4, 2, 0},
											 {3, 0, 1, 6},
											 {3, 2, 3, 2},
											 {1, 2, 4, 3},
											 {3, 4, 2, 1},
											 {2, 5, 2, 1},
											 {1, 4, 1, 4},
											 {4, 1, 3, 2},
											 {3, 3, 1, 3},
											 {0, 6, 2, 2},
											 {6, 1, 1, 2},
											 {2, 2, 3, 3},
											 {2, 1, 4, 3},
											 {0, 1, 4, 5},
											 {3, 4, 0, 3},
											 {4, 0, 4, 2},
											 {2, 2, 2, 4},
											 {2, 3, 2, 3},
											 {3, 3, 4, 0},
											 {0, 6, 3, 1}};
*/
	// no replanning, initial replanning, act end replanning, leave link replanning
//	protected static int[][] Versuchsplan = {{0, 0, 0, 10},
//											 {0, 1, 0, 9},
//											 {0, 2, 0, 8},
//											 {0, 3, 0, 7},
//											 {0, 4, 0, 6},
//											 {0, 5, 0, 5},
//											 {0, 6, 0, 4},
//											 {0, 7, 0, 3},
//											 {0, 8, 0, 2},
//											 {0, 9, 0, 1},
//											 {0, 10, 0, 0}};
	
	protected static int[][] Versuchsplan = {{0, 0, 10, 0}};
	
	
	// Default Config
	protected static String configFileName = "config.xml";
	protected static String configFilePath = "test/scenarios/berlin";	
	protected static String outbase = "output/scenarios/berlin";
	protected static String inbase = "test/scenarios/berlin";
	
//	protected static String outbase = "mysimulations/kt-zurich/output";
//	protected static String inbase = "mysimulations/kt-zurich/input";
//	protected static String configFilePath = "mysimulations/kt-zurich";
//	protected static String configFileName = "config.xml";
	
//	protected static String configFilePath = "mysimulations/zurich-cut";
//	protected static String configFileName = "config.xml";
//	protected static String outbase = "mysimulations/zurich-cut/output";
//	protected static String inbase = "mysimulations/zurich-cut/input";

//	protected static String configFilePath = "mysimulations/berlin";
//	protected static String configFileName = "config.xml";
//	protected static String outbase = "mysimulations/berlin/output";
//	protected static String inbase = "mysimulations/berlin";
	
	
	private final String separator = System.getProperty("file.separator");
	
	protected static String outputDirectory;
	protected static String inputDirectory;
	
	/*
	 * Select which kind of BatchRun you want to run.
	 */
	public static void main(final String[] args)
	{			
		BatchRun batchRun = new BatchRun();
		//batchRun.runBatchRunRandomCompass();
		//batchRun.runBatchRunCompass();
		//batchRun.runBatchRunWithinDay();
		batchRun.runBatchRunDoE();
		
		System.exit(0);
	}
	
	protected void runBatchRunRandomCompass()
	{
		for (int i = 0; i < knowledgeFactors.length; i++)
		{
			for (int j = 0; j < probabilityFactors.length; j++)
			{
				outputDirectory = outbase + "/RandomCompassRouter" + "_Knowledge" + knowledgeFactors[i] + "_Probability" + probabilityFactors[j];
				inputDirectory = inbase;
			
				outputDirectory = outputDirectory.replace("/", separator);
				inputDirectory = inputDirectory.replace("/", separator);
				
				Gbl.reset();
				
				// only for RandomCompassRoute Batch Runs...
//				RandomCompassRoute.compassProbability = probabilityFactors[j];
			
				Config config = readConfigFile();
				
				updateConfigData(config, knowledgeFactors[i]);
				
				//EventControler controler = new EventControler(config);
				String confFileName = configFilePath + separator + configFileName;
				String[] args = new String[1];
				args[0] = confFileName;
				WithinDayControler controler = new WithinDayControler(args);
				controler.setOverwriteFiles(true);
//				controler.pNoReplanning = 0.0;
				controler.pInitialReplanning = 1.0;
				controler.pActEndReplanning = 0.0;
				controler.pLeaveLinkReplanning = 0.0;
				controler.run();
				controler = null;
				
			}
		}
	}
	
	protected void runBatchRunCompass()
	{
		for (int i = 0; i < knowledgeFactors.length; i++)
		{
			outputDirectory = outbase + "/CompassRouter" + "_Knowledge" + knowledgeFactors[i];
			inputDirectory = inbase;
			
			outputDirectory = outputDirectory.replace("/", separator);
			inputDirectory = inputDirectory.replace("/", separator);
			
			Gbl.reset();
					
			Config config = readConfigFile();
				
			updateConfigData(config, knowledgeFactors[i]);
				
			WithinDayControler controler = new WithinDayControler(config);			
			controler.setOverwriteFiles(true);
//			controler.pNoReplanning = 0.0;
			controler.pInitialReplanning = 1.0;
			controler.pActEndReplanning = 0.0;
			controler.pLeaveLinkReplanning = 0.0;
			controler.run();
			controler = null;
		}
	}
	
	protected void runBatchRunWithinDay()
	{
		for (int i = 0; i < knowledgeFactors.length; i++)
		{
//			for (int j = 0; j < tbuffers.length; j++)
			{
				//outputDirectory = outbase + "/WithinDayRouter" + "_Knowledge" + knowledgeFactors[i] + "_Tbuffer" + tbuffers[j];
				//outputDirectory = outbase + "/ActEndReplanningRouter" + "_Knowledge" + knowledgeFactors[i];
				outputDirectory = outbase + "/LeaveLinkReplanningRouter" + "_Knowledge" + knowledgeFactors[i];
				inputDirectory = inbase;
			
				outputDirectory = outputDirectory.replace("/", separator);
				inputDirectory = inputDirectory.replace("/", separator);
				
				Gbl.reset();
			
				Config config = readConfigFile();
				
				updateConfigData(config, knowledgeFactors[i]);
				
				WithinDayControler controler = new WithinDayControler(config);			
				controler.setOverwriteFiles(true);
				controler.run();
				controler = null;
			}
		}
	}
	
	protected void runBatchRunDoE()
	{
		for (int i = 0; i < Versuchsplan.length; i++)
		{
			//outputDirectory = outbase + "/LeaveLinkReplanningRouter" + "_Knowledge" + knowledgeFactors[i];

			// use always full knowledge
			outputDirectory = outbase + "/DoE" + "_Knowledge_Full" + "_Exeriment_" + (i + 1);
			inputDirectory = inbase;
		
			outputDirectory = outputDirectory.replace("/", separator);
			inputDirectory = inputDirectory.replace("/", separator);
			
			Gbl.reset();
					
			Config config = readConfigFile();
			
			//updateConfigData(config, knowledgeFactors[i]);
			// use always full knowledge
			updateConfigData(config, "full");
			
			WithinDayControler controler = new WithinDayControler(config);
			controler.setOverwriteFiles(true);

			double pNoReplanning = Versuchsplan[i][0]/10.0;
			double pInitialReplanning = Versuchsplan[i][1]/10.0;
			double pActEndReplanning = Versuchsplan[i][2]/10.0;
			double pLeaveLinkReplanning = Versuchsplan[i][3]/10.0;

//			controler.pNoReplanning = pNoReplanning;
			controler.pInitialReplanning = pInitialReplanning;
			controler.pActEndReplanning = pActEndReplanning;
			controler.pLeaveLinkReplanning = pLeaveLinkReplanning;
			
			controler.run();
			controler = null;
			
//			log.info("Leave Link Replanning Counter: " + DuringLegReplanningModule.replanningCounter);
//			log.info("Act End Replanning Counter: " + DuringActivityReplanningModule.replanningCounter);
		}
		
	}
	
	protected Config readConfigFile()
	{		
		String dtdFileName = null;
		String confFileName = configFilePath + separator + configFileName;

		Config config = new Config();
		config.addCoreModules();
		
		if (confFileName != null) 
		{
			try 
			{
				new MatsimConfigReader(config).readFile(confFileName, dtdFileName);
			} 
			catch (IOException e) 
			{
				log.error("Problem loading the configuration file from " + confFileName);
				throw new RuntimeException(e);
			}
		}
		
		return config;
	}

	/*
	protected void writeConfigFile(Config config)
	{
		log.info("Complete config dump:");
		StringWriter writer = new StringWriter();
		ConfigWriter configwriter = new ConfigWriter(config, new PrintWriter(writer));
		configwriter.write();
		log.info("\n\n" + writer.getBuffer().toString());
		log.info("Complete config dump done.");
	}
	*/
	
	/*
	 * Set / Overwrite some Parameters in the Config Module.
	 */
	protected void updateConfigData(Config config, String knowledgeFactor)
	{	
		// if Module does not exist -> create it
		if (config.getModule("selection") == null)
		{
			config.addModule("selection", new Module("selection"));
		}
		// overwrite paths in Config
		config.getModule("config").addParam("outputConfigFile", outputDirectory + "/output_config.xml");
		
		//controler.getConfig().getModule("selection").addParam("inputSelectionFile", inputDirectory +  "/input_selection.xml.gz");
		//controler.getConfig().getModule("selection").addParam("inputSelectionFile", inputDirectory +  "/" + knowledgeFactors[i] + "_input_selection.xml.gz");
		config.getModule("selection").addParam("inputSelectionFile", inputDirectory +  "/" + knowledgeFactor + "_input_selection.xml.gz");
		config.getModule("selection").addParam("outputSelectionFile", outputDirectory +  "/output_selection.xml.gz");
		
		config.getModule("controler").addParam("outputDirectory", outputDirectory);

		config.getModule("events").addParam("inputFile",inputDirectory + "/events.txt.gz");
		config.getModule("events").addParam("outputFile" , outputDirectory + "/output_events.txt.gz");

		config.getModule("facilities").addParam("inputFacilitiesFile", inputDirectory + "/facilities.xml.gz");
		config.getModule("facilities").addParam("outputFacilitiesFile", outputDirectory + "/output_facilities.xml.gz");

		config.getModule("matrices").addParam("inputMatricesFile", inputDirectory + "/matrices.xml");
		config.getModule("matrices").addParam("outputMatricesFile", outputDirectory + "/output_matrices.xml");

		config.getModule("network").addParam("inputNetworkFile", inputDirectory + "/network.xml");
		config.getModule("network").addParam("outputNetworkFile", outputDirectory + "/output_network.xml");

		config.getModule("plans").addParam("inputPlansFile", inputDirectory + "/plans.xml.gz");
		config.getModule("plans").addParam("outputPlansFile", outputDirectory + "/output_plans.xml.gz");

	}
	
}

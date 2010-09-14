//package playground.mzilske.controller;
//
//import org.openstreetmap.osmosis.core.OsmosisConstants;
//import org.openstreetmap.osmosis.core.TaskRegistrar;
//import org.openstreetmap.osmosis.core.cli.CommandLineParser;
//import org.openstreetmap.osmosis.core.pipeline.common.Pipeline;
//
//public class Matsim {
//	
//	public static void main(String[] args) {
//		CommandLineParser commandLineParser;
//		TaskRegistrar taskRegistrar;
//		Pipeline pipeline;
//		long startTime;
//		long finishTime;
//		
//		startTime = System.currentTimeMillis();
//		
//		configureLoggingConsole();
//		
//		configFileParser = new ConfigFileParser();
//		configFileParser.parse(args);
//		
//		configureLoggingLevel(commandLineParser.getLogLevel());
//		
//		LOG.info("Matsim Version " + OsmosisConstants.VERSION);
//		taskRegistrar = new TaskRegistrar();
//		taskRegistrar.initialize(configFileParser.getPlugins());
//		
//		pipeline = new Pipeline(taskRegistrar.getFactoryRegister());
//		
//		LOG.info("Preparing pipeline.");
//		pipeline.prepare(configFileParser.getTaskInfoList());
//		
//		LOG.info("Launching pipeline execution.");
//		pipeline.execute();
//		
//		LOG.info("Pipeline executing, waiting for completion.");
//		pipeline.waitForCompletion();
//		
//		LOG.info("Pipeline complete.");
//		
//		finishTime = System.currentTimeMillis();
//
//		LOG.info("Total execution time: " + (finishTime - startTime) + " milliseconds.");
//	}
//
//}

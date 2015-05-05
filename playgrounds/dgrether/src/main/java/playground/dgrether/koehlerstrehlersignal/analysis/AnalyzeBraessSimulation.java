/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.RunResultsLoader;

/**
 * Class to start the analysis of a MATSim simulation of Braess' example.
 * 
 * @author tthunig
 * 
 */
public class AnalyzeBraessSimulation {
	
	private static final Logger log = Logger.getLogger(AnalyzeBraessSimulation.class);
	
	// input and output information
	private String runDirectory;
	private String outputDir;
	private int lastIteration;

	// fields for results
	private double totalTT;
	private double[] totalRouteTTs;
	private double[] avgRouteTTs;
	private int[] routeUsers;
	private Map<Double, double[]> routeStartsPerSecond;
	private Map<Double, double[]> onRoutePerSecond;

	public AnalyzeBraessSimulation(String runDirectory, int lastIteration,
			String outputDir) {
		this.runDirectory = runDirectory;
		this.outputDir = outputDir;
		this.lastIteration = lastIteration;
	}
	
	/**
	 * starts the analysis of the last iteration 
	 * without using the main method of this class
	 */
	public void analyzeLastIt() {
		calculateResults();
		writeResults();
		writeOnRoutes();
		writeRouteStarts();
	}

	private void calculateResults() {
		RunResultsLoader runDir = new RunResultsLoader(runDirectory, null);
		String eventsFilename = runDir.getEventsFilename(lastIteration);

		EventsManager eventsManager = new EventsManagerImpl();
		BraessRouteDistributionAndTT handler = new BraessRouteDistributionAndTT();
		eventsManager.addHandler(handler);

		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFilename);

		this.totalTT = handler.getTotalTT();
		this.totalRouteTTs = handler.getTotalRouteTTs();
		this.avgRouteTTs = handler.getAvgRouteTTs();
		this.routeUsers = handler.getRouteUsers();
		this.routeStartsPerSecond = handler.getRouteStartsPerSecond();
		this.onRoutePerSecond = handler.getOnRoutePerSecond();
		
		log.info("The total travel time is " + totalTT);
		log.info(routeUsers[0] + " are using the upper route, " + routeUsers[1] 
				+ " the middle one and " + routeUsers[2] + " the lower one.");
	}

	private void writeRouteStarts() {
		PrintStream stream;
		try {
			stream = new PrintStream(new File(outputDir + "startsPerRoute.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "time\t#starts up\t#starts mid\t#starts low\t#starts total";
		stream.println(header);
		for (Double time : this.routeStartsPerSecond.keySet()) {
			StringBuffer line = new StringBuffer();
			double[] routeStarts = this.routeStartsPerSecond.get(time);
			double totalStarts = 0.0;
			
			line.append(time);
			for (int i = 0; i < 3; i++) {
				line.append("\t" + routeStarts[i]);
				totalStarts += routeStarts[i];
			}
			line.append("\t" + totalStarts);
			stream.println(line.toString());
		}

		stream.close();
		
		log.info("output written to " + outputDir + "startsPerRoute.txt");
	}

	private void writeOnRoutes() {
		PrintStream stream;
		try {
			stream = new PrintStream(new File(outputDir + "onRoutes.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "time\t#users up\t#users mid\t#users low\t#users total";
		stream.println(header);
		for (Double time : this.onRoutePerSecond.keySet()) {
			StringBuffer line = new StringBuffer();
			double[] onRoutes = this.onRoutePerSecond.get(time);
			double totalOnRoute = 0.0;
			
			line.append(time);
			for (int i = 0; i < 3; i++) {
				line.append("\t" + onRoutes[i]);
				totalOnRoute += onRoutes[i];
			}
			line.append("\t" + totalOnRoute);
			stream.println(line.toString());
		}

		stream.close();
		
		log.info("output written to " + outputDir + "onRoutes.txt");
	}

	private void writeResults() {
		PrintStream stream;
		try {
			stream = new PrintStream(new File(outputDir + "results.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "run\ttotal tt[s]\t#users up\t#users mid\t#users low\tavg tt[s] up\tavg tt[s] mid\tavg tt[s] low\ttotal tt[s] up\ttotal tt[s] mid\ttotal tt[s] low";
		stream.println(header);
		StringBuffer line = new StringBuffer();
		line.append("\t");
		line.append(totalTT);
		for (int i = 0; i < 3; i++) {
			line.append("\t" + routeUsers[i]);
		}
		for (int i = 0; i < 3; i++) {
			line.append("\t" + avgRouteTTs[i]);
		}
		for (int i = 0; i < 3; i++) {
			line.append("\t" + totalRouteTTs[i]);
		}
		stream.println(line.toString());

		stream.close();
		
		log.info("output written to " + outputDir + "results.txt");
	}
	
	/**
	 * analyzes all iterations in terms of route choice and travel time
	 */
	public void analyzeAllIt(){
		RunResultsLoader runDir = new RunResultsLoader(runDirectory, null);
		
		// prepare writing
		PrintStream stream;
		try {
			stream = new PrintStream(new File(outputDir + "results.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		String header = "it\ttotal tt[s]\t#users up\t#users mid\t#users low\tavg tt[s] up\tavg tt[s] mid\tavg tt[s] low";
		stream.println(header);
		StringBuffer line = new StringBuffer();
		
		double totalTTIt;
		double[] avgRouteTTsIt;
		int[] routeUsersIt;
		for (int i=0; i<=lastIteration; i++){
			// analyze single iterations
			String eventsFilename = runDir.getEventsFilename(i);

			EventsManager eventsManager = new EventsManagerImpl();
			BraessRouteDistributionAndTT handler = new BraessRouteDistributionAndTT();
			eventsManager.addHandler(handler);

			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.readFile(eventsFilename);

			// get results
			totalTTIt = handler.getTotalTT();
			avgRouteTTsIt = handler.getAvgRouteTTs();
			routeUsersIt = handler.getRouteUsers();
			
			// write results
			line.append(i + "\t" + totalTTIt);
			for (int j = 0; j < 3; i++) {
				line.append("\t" + routeUsersIt[j]);
			}
			for (int j = 0; j < 3; i++) {
				line.append("\t" + avgRouteTTsIt[j]);
			}
			stream.println(line.toString());			
		}
		stream.close();
		
		log.info(lastIteration + " Iterations analyzed.");
	}

	
	/* -------------------- static methods ------------------- */
	
	/**
	 * start to analyze a braess simulation.
	 * 
	 * if args contains information, this tool will analyze the simulation in the
	 * run directory given by the first entry. the second entry should give the 
	 * iteration number.
	 * 
	 * if args is empty, the simulation will be started via code configuration
	 * 
	 * @param args information for the run to analyze 
	 */
	public static void main(String[] args) {
		
		if (args == null || args.length == 0){
			log.info("run analysis from code");
			AnalyzeBraessSimulation.runFromCode();
		}
		else {
			log.info("run analysis from args");
			AnalyzeBraessSimulation.runFromArgs(args);
		}
		
	}

	/**
	 * starts the analysis with the given input
	 * 
	 * @param args
	 */
	private static void runFromArgs(String[] args) {
		String runDirectory = args[0];
		String outputDir = runDirectory + "analysis/";
		new File(outputDir).mkdir();

		int lastIteration = Integer.parseInt(args[1]);
		
		AnalyzeBraessSimulation analyzer = new AnalyzeBraessSimulation(
				runDirectory, lastIteration, outputDir);
		analyzer.calculateResults();
		analyzer.writeResults();
		analyzer.writeOnRoutes();
		analyzer.writeRouteStarts();
	}

	/**
	 * starts the analysis with a code configuration.
	 * please adapt your run properties here.
	 * 
	 */
	private static void runFromCode() {
		List<String> coordNames = new ArrayList<>();
//		coordNames.add("minCoord");
		coordNames.add("greenWaveZ");
//		coordNames.add("maxCoord");
//		coordNames.add("maxCoordEmptyZ");
//		coordNames.add("maxCoordFullZ");
//		coordNames.add("minCoordFullZ");
//		coordNames.add("basecaseContinued");
//		coordNames.add("basecase");
		
		List<String> ttZs = new ArrayList<>();
//		ttZs.add("0s");
		ttZs.add("5s");
//		ttZs.add("10s");
//		ttZs.add("200s");
		
		String cap = "8640";
		String date = "2015-04-15";
		int tbs = 1;
		int numberOfAgents = 3600;
		int lastIterationNonBC = 200;
		double expBeta = 2.0;
		double reRoute = 0.1;
		
		for (String coordName : coordNames){
			for (String ttZ : ttZs){
				String runDirectory = DgPaths.REPOS
						+ "runs-svn/cottbus/braess/" + date + "_tbs" + tbs + "_net" + cap
						+ "-" + ttZ + "_p" + numberOfAgents + "_" + coordName + "_it" 
						+ lastIterationNonBC + "_expBeta" + expBeta + "_reRoute" + reRoute + "/";
				String outputDir = runDirectory + "analysis/";
				new File(outputDir).mkdir();

				int lastIteration;
				if (coordName == "basecase")
					lastIteration = 100;
				else
//					lastIteration = 200;
					lastIteration = lastIterationNonBC;
					
				AnalyzeBraessSimulation analyzer = new AnalyzeBraessSimulation(
						runDirectory, lastIteration, outputDir);
				analyzer.calculateResults();
				analyzer.writeResults();
				// TODO latex writer bei LatexResultsWriter abgucken
			}
		}
	}
}

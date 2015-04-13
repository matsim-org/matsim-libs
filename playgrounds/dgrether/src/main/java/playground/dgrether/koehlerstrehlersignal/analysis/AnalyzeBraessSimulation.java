/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.RunResultsLoader;
import playground.dgrether.koehlerstrehlersignal.analysis.DgAnalyseCottbusKS2010.RunInfo;

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
	private RunInfo ri;

	// fields for results
	private double totalTT;
	private double[] totalRouteTTs;
	private double[] avgRouteTTs;
	private int[] routeUsers;

	public AnalyzeBraessSimulation(String runDirectory, RunInfo ri,
			String outputDir) {
		this.runDirectory = runDirectory;
		this.outputDir = outputDir;
		this.ri = ri;
	}

	private void calculateResults() {
		RunResultsLoader runDir = new RunResultsLoader(runDirectory, ri.runId);
		String eventsFilename = runDir.getEventsFilename(ri.iteration);

		EventsManager eventsManager = new EventsManagerImpl();
		BraessRouteDistributionAndTT handler = new BraessRouteDistributionAndTT();
		eventsManager.addHandler(handler);

		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFilename);

		this.totalTT = handler.getTotalTT();
		this.totalRouteTTs = handler.getTotalRouteTTs();
		this.avgRouteTTs = handler.getAvgRouteTTs();
		this.routeUsers = handler.getRouteUsers();
		
		log.info("The total travel time is " + totalTT);
		log.info(routeUsers[0] + " are using the upper route, " + routeUsers[1] 
				+ " the middle one and " + routeUsers[2] + " the lower one.");
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
		line.append(ri.remark);
		line.append("\t");
		line.append(totalTT);
		for (int i = 0; i < 3; i++) {
			line.append("\t");
			line.append(routeUsers[i]);
		}
		for (int i = 0; i < 3; i++) {
			line.append("\t");
			line.append(avgRouteTTs[i]);
		}
		for (int i = 0; i < 3; i++) {
			line.append("\t");
			line.append(totalRouteTTs[i]);
		}
		stream.println(line.toString());

		stream.close();
		
		log.info("output written to " + outputDir + "results.txt");
	}

	/**
	 * @param args not used
	 */
	public static void main(String[] args) {
		
		List<String> coordNames = new ArrayList<>();
		coordNames.add("minCoord");
		coordNames.add("greenWaveZ");
		coordNames.add("maxCoord");
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
		String date = "2015-04-13";
		int tbs = 1;
		
		for (String coordName : coordNames){
			for (String ttZ : ttZs){
				String runDirectory = DgPaths.REPOS
						+ "runs-svn/cottbus/braess/" + date + "_tbs" + tbs + "_net" + cap
						+ "-" + ttZ + "_" + coordName + "/";
				String outputDir = runDirectory + "analysis/";
				new File(outputDir).mkdir();

				RunInfo ri = new RunInfo();
				ri.runId = null;
				if (coordName == "basecase")
					ri.iteration = 100;
				else
					ri.iteration = 200;
				ri.baseCase = true;
				ri.remark = coordName;

				AnalyzeBraessSimulation analyzer = new AnalyzeBraessSimulation(
						runDirectory, ri, outputDir);
				analyzer.calculateResults();
				analyzer.writeResults();
				// TODO latex writer bei LatexResultsWriter abgucken
			}
		}
		
	}

}

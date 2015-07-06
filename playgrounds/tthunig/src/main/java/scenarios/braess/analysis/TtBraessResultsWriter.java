package scenarios.braess.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author tthunig
 */
public class TtBraessResultsWriter {

	private static final Logger log = Logger.getLogger(TtBraessResultsWriter.class);
	
	private TtAnalyzeBraessRouteDistributionAndTT handler;
	private String outputDir;
	private PrintStream overallItWritingStream;
	
	public TtBraessResultsWriter(TtAnalyzeBraessRouteDistributionAndTT handler, String outputDir) {
		this.handler = handler;
		this.outputDir = outputDir;
		
		// prepare file for the results of all iterations
		prepareWriting();
	}

	private void prepareWriting() {
		try {
			this.overallItWritingStream = new PrintStream(new File(this.outputDir + "routesAndTTs.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		String header = "it\ttotal tt[s]\t#users up\t#users mid\t#users low\tavg tt[s] up\tavg tt[s] mid\tavg tt[s] low";
		this.overallItWritingStream.println(header);
	}

	public void addSingleItToResults(int iteration){
		
		log.info("Starting to analyze iteration " + iteration);

		// get results
		double totalTTIt = handler.getTotalTT();
		double[] avgRouteTTsIt = handler.calculateAvgRouteTTs();
		int[] routeUsersIt = handler.getRouteUsers();
		
		// write results
		StringBuffer line = new StringBuffer();
		line.append(iteration + "\t" + totalTTIt);
		for (int j = 0; j < 3; j++) {
			line.append("\t" + routeUsersIt[j]);
		}
		for (int j = 0; j < 3; j++) {
			line.append("\t" + avgRouteTTsIt[j]);
		}
		this.overallItWritingStream.println(line.toString());
	}

	public void writeFinalResults() {
		// close stream
		this.overallItWritingStream.close();
		
		// write last iteration specific analysis
		log.info("Final analysis:");		
		writeResults(handler.getTotalTT(), handler.getTotalRouteTTs(), 
				handler.calculateAvgRouteTTs(), handler.getRouteUsers());
		writeOnRoutes(handler.getOnRoutePerSecond());
		writeRouteStarts(handler.getRouteStartsPerSecond());
		writeAvgRouteTTs("Wait2Link", handler.calculateAvgRouteTTsPerWait2LinkTime());
		writeAvgRouteTTs("Arrival", handler.calculateAvgRouteTTsPerArrivalTime());
	}
	
	private void writeResults(double totalTT, double[] totalRouteTTs,
			double[] avgRouteTTs, int[] routeUsers) {

		log.info("The total travel time is " + totalTT);
		log.info(routeUsers[0] + " are using the upper route, " + routeUsers[1] 
				+ " the middle one and " + routeUsers[2] + " the lower one.");
		log.info("The average travel times are " + avgRouteTTs[0] + ", " + 
				avgRouteTTs[1] + " and " + avgRouteTTs[2] + ".");
		log.info("Latex format: " + (int)totalTT + " & " + routeUsers[0] + " & "
				+ routeUsers[1] + " & " + routeUsers[2] + " & "
				+ (int)avgRouteTTs[0] + " & " + (int)avgRouteTTs[1] + " & "
				+ (int)avgRouteTTs[2] + " \\\\");

		PrintStream stream;
		String filename = outputDir + "results.txt";
		try {
			stream = new PrintStream(new File(filename));
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
		
		log.info("output written to " + filename);
	}

	private void writeRouteStarts(Map<Double, double[]> routeStartsMap) {
		PrintStream stream;
		String filename = outputDir + "startsPerRoute.txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "time\t#starts up\t#starts mid\t#starts low\t#starts total";
		stream.println(header);
		for (Double time : routeStartsMap.keySet()) {
			StringBuffer line = new StringBuffer();
			double[] routeStarts = routeStartsMap.get(time);
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
		
		log.info("output written to " + filename);
	}

	private void writeOnRoutes(Map<Double, double[]> onRoutesMap) {
		PrintStream stream;
		String filename = this.outputDir + "onRoutes.txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = "time\t#users up\t#users mid\t#users low\t#users total";
		stream.println(header);
		for (Double time : onRoutesMap.keySet()) {
			StringBuffer line = new StringBuffer();
			double[] onRoutes = onRoutesMap.get(time);
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
		
		log.info("output written to " + filename);
	}

	/**
	 * 
	 * @param eventType Arrival or Wait2link
	 * @param avgTTs
	 */
	private void writeAvgRouteTTs(String eventType, Map<Double, double[]> avgTTs) {
		PrintStream stream;
		String filename = outputDir + "avgRouteTTsPer" + eventType + ".txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		String header = eventType + "Time\tavg tt up\tavg tt mid\tavg tt low";
		stream.println(header);
		for (Double eventTime : avgTTs.keySet()) {
			StringBuffer line = new StringBuffer();
			double[] avgRouteTTs = avgTTs.get(eventTime);
			
			line.append(eventTime);
			for (int i = 0; i < 3; i++) {
				line.append("\t" + avgRouteTTs[i]);
			}
			stream.println(line.toString());
		}

		stream.close();
		
		log.info("output written to " + filename);
	}
	
}

/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisSummary.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.meisterk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.LegHistogram;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.algorithms.PlanAverageScore;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;

public class AnalysisSummary {

	public static final String CONFIG_ANALYSIS = "analysis";

	public static final String CONFIG_ANALYSIS_THE_RUN = "theRun";
	private static String theRun;

	public static final String CONFIG_ANALYSIS_MIN_ITERATION = "minIteration";
	private static int minIteration;

	public static final String CONFIG_ANALYSIS_MAX_ITERATION = "maxIteration";
	private static int maxIteration;

	public static final String CONFIG_ANALYSIS_TIME_BIN_SIZE = "volumeAnalyzer_timeBinSize";
	private static int timeBinSize;

	public static final String CONFIG_ANALYSIS_MAX_TIME = "volumeAnalyzer_maxTime";
	private static int maxTime;

	public static final String CONFIG_ANALYSIS_MIN_TIME_OUTPUT = "minTimeOutput";
	private static int minTimeOutput;

	public static final String CONFIG_ANALYSIS_MAX_TIME_OUTPUT = "maxTimeOutput";
	private static int maxTimeOutput;

	public static final String CONFIG_ANALYSIS_LINK_ID = "linkId";
	private static String linkId;


    public static void main(final String[] args) throws Exception {

		Gbl.createConfig(args);

//		Gbl.createWorld();
//		Gbl.createFacilities();

		initConfig();
		run();

    }

    private static void initConfig() {

    	theRun = Gbl.getConfig().getParam(AnalysisSummary.CONFIG_ANALYSIS, AnalysisSummary.CONFIG_ANALYSIS_THE_RUN);
    	minIteration = Integer.parseInt(Gbl.getConfig().getParam(AnalysisSummary.CONFIG_ANALYSIS, AnalysisSummary.CONFIG_ANALYSIS_MIN_ITERATION));
    	maxIteration = Integer.parseInt(Gbl.getConfig().getParam(AnalysisSummary.CONFIG_ANALYSIS, AnalysisSummary.CONFIG_ANALYSIS_MAX_ITERATION));
    	timeBinSize = Integer.parseInt(Gbl.getConfig().getParam(AnalysisSummary.CONFIG_ANALYSIS, AnalysisSummary.CONFIG_ANALYSIS_TIME_BIN_SIZE));
    	maxTime = Integer.parseInt(Gbl.getConfig().getParam(AnalysisSummary.CONFIG_ANALYSIS, AnalysisSummary.CONFIG_ANALYSIS_MAX_TIME));
    	minTimeOutput = Integer.parseInt(Gbl.getConfig().getParam(AnalysisSummary.CONFIG_ANALYSIS, AnalysisSummary.CONFIG_ANALYSIS_MIN_TIME_OUTPUT));
    	maxTimeOutput = Integer.parseInt(Gbl.getConfig().getParam(AnalysisSummary.CONFIG_ANALYSIS, AnalysisSummary.CONFIG_ANALYSIS_MAX_TIME_OUTPUT));
    	linkId = Gbl.getConfig().getParam(AnalysisSummary.CONFIG_ANALYSIS, AnalysisSummary.CONFIG_ANALYSIS_LINK_ID);

    }

	public static void run() {

		Plans population = null;
		NetworkLayer network = null;

		Integer[] averageLinkResults = null;

		System.out.println("  creating network layer... ");
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		System.out.println("  done");

		System.out.println("  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		// open analysis summary file
		String analysisDirName = theRun + System.getProperty("file.separator") + "analysis";

		File analysisDir = new File(analysisDirName);
		analysisDir.mkdir();

		String analysisSummaryFilename = analysisDirName + System.getProperty("file.separator") + "iterationSummary.txt";
		BufferedWriter analysisSummaryFile = null;
		try {
			analysisSummaryFile = new BufferedWriter(new FileWriter(analysisSummaryFilename));

			analysisSummaryFile.write(
					"#[it]" + "\t" +
					"[plan performance]" + "\t" +
					"[average score]" + "\t" +
					"[average trip duration]" + "\t"
					);
			analysisSummaryFile.newLine();


			// loop through ITERS directories of a run
			File mainDir = new File(theRun + System.getProperty("file.separator") + "ITERS");

			for (int ii=minIteration; ii <= AnalysisSummary.maxIteration; ii+=10) {

				System.out.println("    [AnalysisSummary] - iteration #" + ii);

				analysisSummaryFile.write(ii + "\t");

				// read population of that iteration
				String populationFilename =
					theRun + System.getProperty("file.separator") +
					"ITERS" + System.getProperty("file.separator") +
					"it." + ii + System.getProperty("file.separator") +
					ii + ".plans.xml.gz";

				System.out.println("  reading plans xml file... ");
				population = new Plans(Plans.NO_STREAMING);
				PlansReaderI plansReader = new MatsimPlansReader(population);
				plansReader.readFile(populationFilename);
				population.printPlansCount();
				System.out.println("  reading plans xml file...DONE.");

				// create events object
				Events events = new Events();

				// register event handlers
				// - scoring
				CharyparNagelScoringFunctionFactory sfFactory = new CharyparNagelScoringFunctionFactory();
				EventsToScore planScorer = new EventsToScore(population, sfFactory);
				events.addHandler(planScorer);
				// - duration spent in traffic
				CalcLegTimes legTimes = new CalcLegTimes(population);
				events.addHandler(legTimes);
				// - statistics of a single link
//				LinkQueueStats lqs = new LinkQueueStats(linkId);
//				events.addHandler(lqs);
				// - activity departure/arrival statistics
				int binSize = 300;
				LegHistogram histogram = new LegHistogram(binSize);
				events.addHandler(histogram);

				// read events from an iteration
				String eventsFilename =
					theRun + System.getProperty("file.separator") +
					"ITERS" + System.getProperty("file.separator") +
					"it." + ii + System.getProperty("file.separator") +
					ii + ".events.txt.gz";
				System.out.println("  reading events file and (probably) running events algos");
				new MatsimEventsReader(events).readFile(eventsFilename);
				System.out.println("  done.");
				System.out.flush();

				// readout summarizing methods of event handlers and/or plan/person algorithms, and append data to iteration line
				// - average plan performance of last iteration
				planScorer.finish();
				analysisSummaryFile.write(Double.toString(planScorer.getAveragePlanPerformance()));
				analysisSummaryFile.write("\t");

				// - average of all plan scores in the population
				PlanAverageScore average = new PlanAverageScore();
				average.run(population);
				analysisSummaryFile.write(Double.toString(average.getAverage()));
				analysisSummaryFile.write("\t");

				// - average trip durations
				analysisSummaryFile.write(Double.toString(legTimes.getAverageTripDuration()));
				analysisSummaryFile.write("\t");

				// write a new line and flush so we will see something
				analysisSummaryFile.newLine();
				analysisSummaryFile.flush();

				// link statistics in a time range
//				String linkQueueStatsFilename = analysisDirName + System.getProperty("file.separator") + ii + ".linkQueueStats.txt";
//				BufferedWriter linkQueueStatsFile = new BufferedWriter(new FileWriter(linkQueueStatsFilename));
//				lqs.dumpStats(linkQueueStatsFile);
//				linkQueueStatsFile.close();

				// departure time distributions of first leg (home -> work)
				String altFilename = analysisDirName + System.getProperty("file.separator") + ii + ".depTimes.txt";
//				BufferedWriter analyzeLegTimesFile = new BufferedWriter(new FileWriter(altFilename));
//				int[][] depCounts = alt.getLegDepCounts();
//
//				analyzeLegTimesFile.write("#bin\tt_dep");
//				analyzeLegTimesFile.newLine();
//				for (int dc=0; dc < depCounts[0].length; dc++) {
//					analyzeLegTimesFile.write(new Integer(dc * binSize) + "\t" + depCounts[0][dc]);
//					analyzeLegTimesFile.newLine();
//				}
//				analyzeLegTimesFile.close();
				histogram.write(altFilename);

				// reset event handlers after analysis of an iteration
				events.resetHandlers(ii);

			}

			// close analysis summary file
			analysisSummaryFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

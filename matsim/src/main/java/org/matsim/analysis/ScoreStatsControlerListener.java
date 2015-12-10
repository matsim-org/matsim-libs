/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreStats.java
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

package org.matsim.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * Calculates at the end of each iteration the following statistics:
 * <ul>
 * <li>average score of the selected plan</li>
 * <li>average of the score of the worst plan of each agent</li>
 * <li>average of the score of the best plan of each agent</li>
 * <li>average of the average score of all plans of each agent</li>
 * </ul>
 * Plans with undefined scores
 * are not included in the statistics. The calculated values are written to a file, each iteration on
 * a separate line.
 *
 * @author mrieser
 */
public class ScoreStatsControlerListener implements StartupListener, IterationEndsListener, ShutdownListener, ScoreStats {

    public static final String FILENAME_SCORESTATS = "scorestats";
    final public static int INDEX_WORST = 0;
    final public static int INDEX_BEST = 1;
    final public static int INDEX_AVERAGE = 2;
    final public static int INDEX_EXECUTED = 3;

    final private Population population;
    final private BufferedWriter out;
    final private String fileName;

    private final boolean createPNG;
    private final Config config;
    private double[][] history = null;
    private boolean overflown = false;
    private int minIteration = 0;

    private final static Logger log = Logger.getLogger(ScoreStatsControlerListener.class);

    @Inject
    ScoreStatsControlerListener(Config config, Population population, OutputDirectoryHierarchy controlerIO) {
        this(config, population, controlerIO.getOutputFilename(FILENAME_SCORESTATS), config.controler().isCreateGraphs());
    }

    /**
     * Creates a new ScoreStats instance.
     *
     *
     * @param filename including the path, excluding the file type extension
     * @param createPNG true if in every iteration, the scorestats should be visualized in a graph and written to disk.
     * @throws UncheckedIOException
     */
    public ScoreStatsControlerListener(Config config, final Population population, final String filename, final boolean createPNG) throws UncheckedIOException {
        this.config = config;
        this.population = population;
        this.fileName = filename;
        this.createPNG = createPNG;
        if (filename.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            this.out = IOUtils.getBufferedWriter(filename);
        } else {
            this.out = IOUtils.getBufferedWriter(filename + ".txt");
        }
        try {
            this.out.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void notifyStartup(final StartupEvent event) {
        this.minIteration = config.controler().getFirstIteration();
        int maxIter = config.controler().getLastIteration();
        int iterations = maxIter - this.minIteration;
        if (iterations > 5000) iterations = 5000; // limit the history size
        this.history = new double[4][iterations+1];
    }

    @Override
    public void notifyIterationEnds(final IterationEndsEvent event) {
        double sumScoreWorst = 0.0;
        double sumScoreBest = 0.0;
        double sumAvgScores = 0.0;
        double sumExecutedScores = 0.0;
        int nofScoreWorst = 0;
        int nofScoreBest = 0;
        int nofAvgScores = 0;
        int nofExecutedScores = 0;

        for (Person person : this.population.getPersons().values()) {
            Plan worstPlan = null;
            Plan bestPlan = null;
            double worstScore = Double.POSITIVE_INFINITY;
            double bestScore = Double.NEGATIVE_INFINITY;
            double sumScores = 0.0;
            double cntScores = 0;
            for (Plan plan : person.getPlans()) {

                if (plan.getScore() == null) {
                    continue;
                }
                double score = plan.getScore();

                // worst plan
                if (worstPlan == null) {
                    worstPlan = plan;
                    worstScore = score;
                } else if (score < worstScore) {
                    worstPlan = plan;
                    worstScore = score;
                }

                // best plan
                if (bestPlan == null) {
                    bestPlan = plan;
                    bestScore = score;
                } else if (score > bestScore) {
                    bestPlan = plan;
                    bestScore = score;
                }

                // avg. score
                sumScores += score;
                cntScores++;

                // executed plan?
                if (PersonUtils.isSelected(plan)) {
                    sumExecutedScores += score;
                    nofExecutedScores++;
                }
            }

            if (worstPlan != null) {
                nofScoreWorst++;
                sumScoreWorst += worstScore;
            }
            if (bestPlan != null) {
                nofScoreBest++;
                sumScoreBest += bestScore;
            }
            if (cntScores > 0) {
                sumAvgScores += (sumScores / cntScores);
                nofAvgScores++;
            }
        }
        log.info("-- avg. score of the executed plan of each agent: " + (sumExecutedScores / nofExecutedScores));
        log.info("-- avg. score of the worst plan of each agent: " + (sumScoreWorst / nofScoreWorst));
        log.info("-- avg. of the avg. plan score per agent: " + (sumAvgScores / nofAvgScores));
        log.info("-- avg. score of the best plan of each agent: " + (sumScoreBest / nofScoreBest));

        try {
            this.out.write(event.getIteration() + "\t" + (sumExecutedScores / nofExecutedScores) + "\t" +
                    (sumScoreWorst / nofScoreWorst) + "\t" + (sumAvgScores / nofAvgScores) + "\t" + (sumScoreBest / nofScoreBest) + "\n");
            this.out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int index = event.getIteration() - this.minIteration;
        if (index < this.history[0].length) { // otherwise, we cannot store more information, so stop collecting and graphing.
            this.history[INDEX_WORST][index] = (sumScoreWorst / nofScoreWorst);
            this.history[INDEX_BEST][index] = (sumScoreBest / nofScoreBest);
            this.history[INDEX_AVERAGE][index] = (sumAvgScores / nofAvgScores);
            this.history[INDEX_EXECUTED][index] = (sumExecutedScores / nofExecutedScores);

            if (this.createPNG && event.getIteration() != this.minIteration) {
                // create chart when data of more than one iteration is available.
                XYLineChart chart = new XYLineChart("Score Statistics", "iteration", "score");
                double[] iterations = new double[index + 1];
                for (int i = 0; i <= index; i++) {
                    iterations[i] = i + this.minIteration;
                }
                double[] values = new double[index + 1];
                System.arraycopy(this.history[INDEX_WORST], 0, values, 0, index + 1);
                chart.addSeries("avg. worst score", iterations, values);
                System.arraycopy(this.history[INDEX_BEST], 0, values, 0, index + 1);
                chart.addSeries("avg. best score", iterations, values);
                System.arraycopy(this.history[INDEX_AVERAGE], 0, values, 0, index + 1);
                chart.addSeries("avg. of plans' average score", iterations, values);
                System.arraycopy(this.history[INDEX_EXECUTED], 0, values, 0, index + 1);
                chart.addSeries("avg. executed score", iterations, values);
                chart.addMatsimLogo();
                chart.saveAsPng(this.fileName + ".png", 800, 600);
            }
        } else {
            this.overflown = true;
        }
    }

    @Override
    public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
        try {
            this.out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public double[][] getHistory() {
        if (overflown) {
            log.warn("The data structure for score statistics has overflown (too many iterations), but it is being queried. Likely an error.");
        }
        return this.history.clone();
    }

}

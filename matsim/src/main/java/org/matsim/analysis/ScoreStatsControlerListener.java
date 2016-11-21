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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

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
	// yy might make sense to either divide this into ScoreStats and ModeStats.  Or rename into IterationStats.  kai, nov'16

	public static final String FILENAME_SCORESTATS = "scorestats";
	public static final String FILENAME_MODESTATS = "modestats";
	public static enum ScoreItem { worst, best, average, executed } ; 

	final private Population population;
	final private BufferedWriter out;
	final private String fileName;
	
	final private BufferedWriter modeOut ;
	final private String modeFileName ;

	private final boolean createPNG;
	private final ControlerConfigGroup controlerConfigGroup;

	Map<ScoreItem,Map< Integer, Double>> scoreHistory = new HashMap<>() ;
	Map<String,Map<Integer,Double>> modeHistories = new HashMap<>() ;
	private int minIteration = 0;
	private StageActivityTypes stageActivities;
	private MainModeIdentifier mainModeIdentifier;
	private Map<String,Double> modeCnt = new TreeMap<>() ;
	
	private boolean ini = true ;
	private final Set<String> modes;

	private final static Logger log = Logger.getLogger(ScoreStatsControlerListener.class);

	@Inject
	ScoreStatsControlerListener(ControlerConfigGroup controlerConfigGroup, Population population1, OutputDirectoryHierarchy controlerIO,
			TripRouter tripRouter, PlanCalcScoreConfigGroup scoreConfig ) {
		this.controlerConfigGroup = controlerConfigGroup;
		this.population = population1;
		this.fileName = controlerIO.getOutputFilename(FILENAME_SCORESTATS);
		this.modeFileName = controlerIO.getOutputFilename( FILENAME_MODESTATS ) ;
		this.createPNG = controlerConfigGroup.isCreateGraphs();
		this.out = IOUtils.getBufferedWriter(this.fileName + ".txt");
		this.modeOut = IOUtils.getBufferedWriter(this.modeFileName + ".txt");
		try {
			this.out.write("ITERATION\tavg. EXECUTED\tavg. WORST\tavg. AVG\tavg. BEST\n");
			this.modeOut.write("Iteration");
			this.modes = scoreConfig.getModes().keySet();
			for ( String mode : modes ) {
				this.modeOut.write("\t" + mode);
			}
			this.modeOut.write("\n"); ;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		this.stageActivities = tripRouter.getStageActivityTypes() ;
		this.mainModeIdentifier = tripRouter.getMainModeIdentifier() ;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		this.minIteration = controlerConfigGroup.getFirstIteration();
		//		int maxIter = controlerConfigGroup.getLastIteration();
		//		int iterations = maxIter - this.minIteration;
		//		if (iterations > 5000) iterations = 5000; // limit the history size
		for ( ScoreItem item : ScoreItem.values() ) {
			scoreHistory.put( item, new TreeMap<Integer,Double>() ) ;
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		collectScoreInfo(event);
		collectModeShareInfo(event) ;
	}

	private void collectModeShareInfo(final IterationEndsEvent event) {
		for (Person person : this.population.getPersons().values()) {
			Plan plan = person.getSelectedPlan() ;
			List<Trip> trips = TripStructureUtils.getTrips(plan, stageActivities) ;
			for ( Trip trip : trips ) {
				String mode = this.mainModeIdentifier.identifyMainMode( trip.getTripElements() ) ;
				// yy as stated elsewhere, the "computer science" mode identification may not be the same as the "transport planning" 
				// mode identification.  Maybe revise.  kai, nov'16
				
				Double cnt = this.modeCnt.get( mode );
				if ( cnt==null ) {
					cnt = 0. ;
				}
				this.modeCnt.put( mode, cnt + 1 ) ;
			}
		}

		double sum = 0 ;
		for ( Double val : this.modeCnt.values() ) {
			sum += val ;
		}
		
		try {
			this.modeOut.write( event.getIteration() ) ;
			for ( String mode : modes ) {
				Double cnt = this.modeCnt.get(mode) ;
				double share = 0. ;
				if ( cnt!=null ) {
					share = cnt/sum;
				}
				log.info("-- mode share of mode " + mode + " = " + share );
				this.modeOut.write( "\t" + share ) ;
				
				Map<Integer, Double> modeHistory = this.modeHistories.get(mode) ;
				if ( modeHistory == null ) {
					modeHistory = new TreeMap<>() ;
				}
				modeHistory.put( event.getIteration(), share ) ;
				
			}
			this.modeOut.write("\n");
			this.modeOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}


		if (this.createPNG && event.getIteration() != this.minIteration) {
			// create chart when data of more than one iteration is available.
			XYLineChart chart = new XYLineChart("Mode Statistics", "iteration", "mode");
			for ( Entry<String, Map<Integer, Double>> entry : this.modeHistories.entrySet() ) {
				String mode = entry.getKey() ;
				Map<Integer, Double> history = entry.getValue() ;
				log.warn( "about to add the following series:" ) ;
				for ( Entry<Integer, Double> item : history.entrySet() ) {
					log.warn( item.getKey() + " -- " + item.getValue() );
				}
				chart.addSeries(mode, history ) ;
			}
			chart.addMatsimLogo();
			chart.saveAsPng(this.modeFileName + ".png", 800, 600);
		}
	}
	private void collectScoreInfo(final IterationEndsEvent event) {
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

//		int index = event.getIteration() - this.minIteration;

		this.scoreHistory.get( ScoreItem.worst ).put( event.getIteration(), sumScoreWorst / nofScoreWorst ) ;
		this.scoreHistory.get( ScoreItem.best ).put( event.getIteration(), sumScoreBest / nofScoreBest ) ;
		this.scoreHistory.get( ScoreItem.average ).put( event.getIteration(), sumAvgScores / nofAvgScores ) ;
		this.scoreHistory.get( ScoreItem.executed ).put( event.getIteration(), sumExecutedScores / nofExecutedScores ) ;

		if (this.createPNG && event.getIteration() != this.minIteration) {
			// create chart when data of more than one iteration is available.
			XYLineChart chart = new XYLineChart("Score Statistics", "iteration", "score");
//			double[] iterations = new double[index + 1];
//			for (int i = 0; i <= index; i++) {
//				iterations[i] = i + this.minIteration;
//			}
			chart.addSeries("avg. worst score", this.scoreHistory.get( ScoreItem.worst ) ) ;
			chart.addSeries("avg. best score", this.scoreHistory.get( ScoreItem.best) );
			chart.addSeries("avg. of plans' average score", this.scoreHistory.get( ScoreItem.average) );
			chart.addSeries("avg. executed score", this.scoreHistory.get( ScoreItem.executed ) );
			chart.addMatsimLogo();
			chart.saveAsPng(this.fileName + ".png", 800, 600);
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
	public Map<ScoreItem, Map<Integer, Double>> getScoreHistory() {
		return Collections.unmodifiableMap( this.scoreHistory ) ;
	}

}

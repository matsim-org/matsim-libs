package playground.florian.ScoreStatsHandler;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.analysis.ScoreStats;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.charts.XYLineChart;


public class ScoreStatsTest implements StartupListener, IterationEndsListener, ShutdownListener {
	
	final private static int INDEX_WORST = 0;
	final private static int INDEX_BEST = 1;
	final private static int INDEX_AVERAGE = 2;
	final private static int INDEX_EXECUTED = 3;
	
	final private PopulationImpl population;
	private final String filename;
	private final boolean createPNG;
	private final ScoreXMLWriter out;
	private double[][] history = null;
	private int minIteration = 0;
	private final static Logger log = Logger.getLogger(ScoreStats.class);
	
	
	public ScoreStatsTest(final PopulationImpl population, final String filename, final boolean createPNG) throws FileNotFoundException, IOException {
		this.population = population;
		this.createPNG = createPNG;
		this.filename = filename;
		this.out = new ScoreXMLWriter();
	}



	public void notifyStartup(StartupEvent event) {
		if (this.createPNG) {
			Controler controler = event.getControler();
			this.minIteration = controler.getFirstIteration();
			int maxIter = controler.getLastIteration();
			int iterations = maxIter - this.minIteration;
			if (iterations > 10000) iterations = 1000; // limit the history size
			this.history = new double[4][iterations+1];
		}
		
	}


	public void notifyIterationEnds(IterationEndsEvent event) {
		double sumScoreWorst = 0.0;
		double sumScoreBest = 0.0;
		double sumAvgScores = 0.0;
		double sumExecutedScores = 0.0;
		int nofScoreWorst = 0;
		int nofScoreBest = 0;
		int nofAvgScores = 0;
		int nofExecutedScores = 0;
//		int nofExecutedIvPlans = 0;
//		int nofExecutedOevPlans = 0;

		for (PersonImpl person : this.population.getPersons().values()) {
			PlanImpl worstPlan = null;
			PlanImpl bestPlan = null;
			double worstScore = Double.POSITIVE_INFINITY;
			double bestScore = Double.NEGATIVE_INFINITY;
			double sumScores = 0.0;
			double cntScores = 0;
			for (PlanImpl plan : person.getPlans()) {

				if (plan.getScore() == null) {
					continue;
				}
				double score = plan.getScore().doubleValue();

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
				if (plan.isSelected()) {
					sumExecutedScores += score;
					nofExecutedScores++;
//					if (plan.getType() == Plan.Type.CAR) {
//						nofExecutedIvPlans ++;
//					}
//					else if (plan.getType() == Plan.Type.PT) {
//						nofExecutedOevPlans++;
//					}
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
//		log.info("-- number of executed plans: "  + nofExecutedScores);
//		log.info("-- number of executed iv plans: "  + nofExecutedIvPlans);
//		log.info("-- number of executed oev plans: "  + nofExecutedOevPlans);
//		log.info("-- modal split iv: "  + ((nofExecutedScores == 0) ? 0 : ((double)nofExecutedIvPlans / (double)nofExecutedScores * 100d)) +
//				" % oev: " + ((nofExecutedScores == 0) ? 0 : ((double)nofExecutedOevPlans / (double)nofExecutedScores * 100d)) + " %");
		log.info("-- avg. score of the worst plan of each agent: " + (sumScoreWorst / nofScoreWorst));
		log.info("-- avg. of the avg. plan score per agent: " + (sumAvgScores / nofAvgScores));
		log.info("-- avg. score of the best plan of each agent: " + (sumScoreBest / nofScoreBest));
		
		double averageAverageScore = sumAvgScores / nofAvgScores;
		double averageBestScore = sumScoreBest / nofScoreBest;
		double averageExecutedScore = sumExecutedScores / nofExecutedScores;
		double averageWorstScore = sumScoreWorst / nofScoreWorst;
		
		out.addScore(event.getIteration(), averageAverageScore, averageBestScore, averageWorstScore, averageExecutedScore);
		out.writeFile(this.filename);
		
		if (this.history != null) {
			int index = event.getIteration() - this.minIteration;
			this.history[INDEX_WORST][index] = (sumScoreWorst / nofScoreWorst);
			this.history[INDEX_BEST][index] = (sumScoreBest / nofScoreBest);
			this.history[INDEX_AVERAGE][index] = (sumAvgScores / nofAvgScores);
			this.history[INDEX_EXECUTED][index] = (sumExecutedScores / nofExecutedScores);
			
			if (event.getIteration() != this.minIteration) {
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
				chart.saveAsPng(Controler.getOutputFilename("scorestats.png"), 800, 600);
			}
			if (index == this.history[0].length) {
				// we cannot store more information, so disable the graph feature.
				this.history = null;
			}
		}
	}
		


	public void notifyShutdown(ShutdownEvent event) {

		
	}
	
	public double[][] getHistory() {
		return this.history.clone();
	}


}


package org.matsim.contrib.pseudosimulation.distributed.instrumentation.scorestats;

import org.apache.log4j.Logger;
import org.matsim.analysis.ScoreStats;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

/**
 * Created by fouriep on 11/28/14.
 */
public class SlaveScoreStatsCalculator{
    Logger log = Logger.getLogger(this.getClass());

    final public static int INDEX_WORST = 0;
    final public static int INDEX_BEST = 1;
    final public static int INDEX_AVERAGE = 2;
    final public static int INDEX_EXECUTED = 3;

    public double[] calculateScoreStats(Population population) {
        double sumScoreWorst = 0.0;
        double sumScoreBest = 0.0;
        double sumAvgScores = 0.0;
        double sumExecutedScores = 0.0;
        int nofScoreWorst = 0;
        int nofScoreBest = 0;
        int nofAvgScores = 0;
        int nofExecutedScores = 0;

        for (Person person : population.getPersons().values()) {
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
                if (plan.isSelected(plan)) {
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
        double[] out = new double[4];
        out[INDEX_EXECUTED] = (sumExecutedScores / nofExecutedScores);
        out[INDEX_WORST] = (sumScoreWorst / nofScoreWorst);
        out[INDEX_AVERAGE] = (sumExecutedScores / nofExecutedScores);
        out[INDEX_BEST] = (sumScoreBest / nofScoreBest);
        return out;
    }

}
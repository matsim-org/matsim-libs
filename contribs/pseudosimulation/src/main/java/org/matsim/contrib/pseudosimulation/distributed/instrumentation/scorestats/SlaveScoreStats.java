package org.matsim.contrib.pseudosimulation.distributed.instrumentation.scorestats;

import java.util.Map;

import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.core.config.Config;

/**
 * Created by fouriep on 11/28/14.
 */
public class SlaveScoreStats implements ScoreStats {

    final public static int INDEX_WORST = 0;
    final public static int INDEX_BEST = 1;
    final public static int INDEX_AVERAGE = 2;
    final public static int INDEX_EXECUTED = 3;
    private double[][] history;
    int firstIter;

    public SlaveScoreStats(Config config) {
        firstIter = config.controler().getFirstIteration();
        int size = config.controler().getLastIteration() - firstIter+1;
        this.history = new double[4][size];
    }

    public void insertEntry(int iterationNumber, int slavePopulationSize, int totalPopulationSize, double[] entry) {
        double weight = (double) slavePopulationSize / (double) totalPopulationSize;
        history[INDEX_WORST][iterationNumber - firstIter] += weight * entry[INDEX_WORST];
        history[INDEX_AVERAGE][iterationNumber - firstIter] += weight * entry[INDEX_AVERAGE];
        history[INDEX_BEST][iterationNumber - firstIter] += weight * entry[INDEX_BEST];
        history[INDEX_EXECUTED][iterationNumber - firstIter] += weight * entry[INDEX_EXECUTED];
    }

    public double[][] getScoreHistoryAsArray() {
	    // yyyy retrofitting to old interface. kai, nov'16
        return this.history;
    }

@Override
public Map<ScoreItem, Map<Integer, Double>> getScoreHistory() {
	// TODO Auto-generated method stub
	throw new RuntimeException("not implemented") ;
}
    
    
}

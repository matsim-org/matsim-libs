package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

public interface RecreateSolutionAcceptor {

    boolean acceptSolutionOrNot(double currentScore, double previousScore, int currentIteration, int totalIterations);
}

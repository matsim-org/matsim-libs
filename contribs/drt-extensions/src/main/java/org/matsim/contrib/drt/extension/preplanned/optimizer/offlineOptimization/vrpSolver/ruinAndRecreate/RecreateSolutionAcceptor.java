package org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.vrpSolver.ruinAndRecreate;

public interface RecreateSolutionAcceptor {

    boolean acceptSolutionOrNot(double currentScore, double previousScore, int currentIteration, int totalIterations);
}

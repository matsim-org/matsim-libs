package org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.vrp_solver.ruinAndRecreate;

public interface RecreateSolutionAcceptor {

    boolean acceptSolutionOrNot(double currentScore, double previousScore, int currentIteration, int totalIterations);
}

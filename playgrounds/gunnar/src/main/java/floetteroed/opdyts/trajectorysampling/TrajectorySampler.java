/*
 * Opdyts - Optimization of dynamic traffic simulations
 *
 * Copyright 2015 Gunnar Flötteröd
 * 
 *
 * This file is part of Opdyts.
 *
 * Opdyts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opdyts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Opdyts.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */
package floetteroed.opdyts.trajectorysampling;

import java.util.Map;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterionResult;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface TrajectorySampler<U extends DecisionVariable> {

	/**
	 * Indicates if there is (no) need to further continue the iterations.
	 * 
	 * TODO Rename into something more meaningful, perhaps "stopIterations".
	 */
	public boolean foundSolution();

	public U getCurrentDecisionVariable();

	public int getTotalTransitionCnt();

	public Map<U, ConvergenceCriterionResult> getDecisionVariable2convergenceResultView();

	public void addStatistic(final String logFileName,
			final Statistic<SamplingStage<U>> statistic);

	public void setStandardLogFileName(String logFileName);

	/**
	 * Call once before the simulation is started. This implements a randomly
	 * selected decision variable in the simulation, with the objective to
	 * enable a first simulation transition.
	 * 
	 */
	public void initialize();

	/**
	 * To be called once after each simulation iteration. Registers the
	 * simulation state reached after that iteration and implements a new trial
	 * decision variable in the simulation.
	 * 
	 * @param newState
	 *            the newly reached simulator state
	 */
	public void afterIteration(SimulatorState newState);

	// TODO NEW
	public ObjectiveFunction getObjectiveFunction();

}
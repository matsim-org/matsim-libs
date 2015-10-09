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
package floetteroed.opdyts.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.trajectorysampling.SamplingStage;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class AlphaStatistic implements Statistic<SamplingStage<?>> {

	// -------------------- MEMBERS --------------------

	private final List<DecisionVariable> decisionVariables;

	// -------------------- CONSTRUCTION --------------------

	public AlphaStatistic(
			final Set<? extends DecisionVariable> decisionVariables) {
		this.decisionVariables = new ArrayList<DecisionVariable>(
				decisionVariables);
	}

	// -------------------- IMPLEMENTATION OF Statistic --------------------

	@Override
	public String label() {
		final StringBuffer result = new StringBuffer("alpha("
				+ this.decisionVariables.get(0).toString() + ")");
		for (int i = 1; i < this.decisionVariables.size(); i++) {
			result.append("\t");
			result.append("alpha(" + this.decisionVariables.get(i) + ")");
		}
		return result.toString();
	}

	@Override
	public String value(final SamplingStage<?> data) {
		final StringBuffer result = new StringBuffer(Double.toString(data
				.getAlphaSum(this.decisionVariables.get(0))));
		for (int i = 1; i < this.decisionVariables.size(); i++) {
			result.append("\t");
			result.append(Double.toString(data
					.getAlphaSum(this.decisionVariables.get(i))));
		}
		return result.toString();
	}
}

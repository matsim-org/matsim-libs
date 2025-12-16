/**
 * se.vti.atap
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.atap.minimalframework.defaults;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.Logger;
import se.vti.atap.minimalframework.NetworkConditions;

/**
 * 
 * @author GunnarF
 *
 * @param <A>
 * @param <T>
 */
public class BasicLoggerImpl<A extends Agent<?>, T extends NetworkConditions>
		implements Logger<A, T>, StatisticsComparisonPrinter.DescriptiveStatisticsLogger {

	private final List<List<Double>> gapStatistics = new ArrayList<>();

	public BasicLoggerImpl() {
	}

	@Override
	public final void log(Set<A> agents, T networkConditions, int iteration) {
		while (this.gapStatistics.size() <= iteration) {
			this.gapStatistics.add(new ArrayList<>());
		}
		this.gapStatistics.get(iteration).add(this.computeGap(agents, networkConditions, iteration));
	}

	public double computeGap(Set<A> agents, T networkConditions, int iteration) {
		return agents.stream().mapToDouble(a -> a.computeGap()).average().getAsDouble();
	}

	public List<List<Double>> getAverageGaps() {
		return this.gapStatistics;
	}
	
	@Override
	public int getNumberOfIterations() {
		return this.gapStatistics.size();
	}

	@Override
	public List<Double> getDataOrNull(int iteration) {
		if (this.gapStatistics.size() <= iteration) {
			return null;
		} else {
			return this.gapStatistics.get(iteration);
		}
	}
}

/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration.datastructures;

import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.pseudosimulation.searchacceleration.utils.SetUtils;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class CountIndicatorUtils {

	private CountIndicatorUtils() {
	}

	public static <L> DynamicData<L> newUnweightedCounts(final TimeDiscretization timeDiscr,
			final Collection<SpaceTimeIndicators<L>> allIndicators) {
		final DynamicData<L> result = new DynamicData<L>(timeDiscr);
		for (SpaceTimeIndicators<L> indicators : allIndicators) {
			for (int bin = 0; bin < indicators.getTimeBinCnt(); bin++) {
				for (L locObj : indicators.getVisitedSpaceObjects(bin)) {
					result.add(locObj, bin, 1.0);
				}
			}
		}
		return result;
	}

	// public static <L> DynamicData<L> newWeightedLinkCounts(final
	// Collection<SpaceTimeIndicators<L>> allIndicators,
	// final AccelerationConfigGroup replParams) {
	// final DynamicData<L> result = new
	// DynamicData<L>(replParams.getTimeDiscretization());
	// for (SpaceTimeIndicators<L> indicators : allIndicators) {
	// for (int bin = 0; bin < indicators.getTimeBinCnt(); bin++) {
	// for (L locObj : indicators.getVisitedSpaceObjects(bin)) {
	// result.add(locObj, bin, replParams.getLinkWeight(locObj, bin));
	// }
	// }
	// }
	// return result;
	// }
	
	public static <L> DynamicData<L> newWeightedCounts(final Collection<SpaceTimeIndicators<L>> allIndicators,
			final Map<L, Double> weights, final TimeDiscretization timeDiscr) {
		final DynamicData<L> result = new DynamicData<L>(timeDiscr);
		for (SpaceTimeIndicators<L> indicators : allIndicators) {
			for (int bin = 0; bin < indicators.getTimeBinCnt(); bin++) {
				for (L locObj : indicators.getVisitedSpaceObjects(bin)) {
					final Double weight = weights.get(locObj);
					// Logger.getLogger(CountIndicatorUtils.class).info("Weight of " + locObj + " is " + weight);
					if (weight != null) {
						result.add(locObj, bin, weight); // TODO Vectorize in DynamicData!
					} else {
						result.add(locObj, bin, 0.0); // TODO Vectorize in DynamicData!
					}
				}
			}
		}
		// System.exit(0);
		return result;
	}

	// public static <L> double sumOfEntries2(final DynamicData<L> data) {
	// double result = 0.0;
	// for (L locObj : data.keySet()) {
	// for (int bin = 0; bin < data.getBinCnt(); bin++) {
	// final double val = data.getBinValue(locObj, bin);
	// result += val * val;
	// }
	// }
	// return result;
	// }

	// public static <L> int count(final DynamicData<L> data, final double minVal) {
	// int result = 0;
	// for (L locObj : data.keySet()) {
	// for (int bin = 0; bin < data.getBinCnt(); bin++) {
	// if (data.getBinValue(locObj, bin) >= minVal) {
	// result++;
	// }
	// }
	// }
	// return result;
	// }

	public static <L> double sumOfDifferences2(final DynamicData<L> counts1, final DynamicData<L> counts2) {
		if (counts1.getBinCnt() != counts2.getBinCnt()) {
			throw new RuntimeException(
					"counts1 has " + counts1.getBinCnt() + " bins, but counts2 has " + counts2.getBinCnt() + " bins.");
		}
		double result = 0.0;
		for (L locObj : SetUtils.union(counts1.keySet(), counts2.keySet())) {
			for (int bin = 0; bin < counts1.getBinCnt(); bin++) {
				final double diff = counts1.getBinValue(locObj, bin) - counts2.getBinValue(locObj, bin);
				result += diff * diff;
			}
		}
		return result;
	}

	public static <L> DynamicData<L> newWeightedDifference(final DynamicData<L> data1, final DynamicData<L> data2,
			final double weight) {
		if (data1.getBinCnt() != data2.getBinCnt()) {
			throw new RuntimeException("currentWeightedCounts has " + data1.getBinCnt()
					+ " bins; newWeightedCounts has " + data2.getBinCnt() + " bins.");
		}
		final DynamicData<L> result = new DynamicData<L>(data1.getStartTime_s(), data1.getBinSize_s(),
				data1.getBinCnt());
		for (L locObj : SetUtils.union(data1.keySet(), data2.keySet())) {
			for (int bin = 0; bin < data1.getBinCnt(); bin++) {
				result.put(locObj, bin, weight * (data1.getBinValue(locObj, bin) - data2.getBinValue(locObj, bin)));
			}
		}
		return result;
	}
}

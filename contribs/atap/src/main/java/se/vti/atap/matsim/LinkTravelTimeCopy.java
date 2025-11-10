/**
 * org.matsim.contrib.atap
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
package se.vti.atap.matsim;

import static java.lang.Math.ceil;
import static java.lang.Math.min;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import se.vti.utils.misc.dynamicdata.UpdatedDynamicData;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LinkTravelTimeCopy implements TravelTime {

	private final UpdatedDynamicData<Id<Link>> data_s;

	public LinkTravelTimeCopy(final TravelTime travelTimes, final Config config, final Network network) {

//		final TravelTime travelTimes = services.getLinkTravelTimes();
//		final Config config = services.getConfig();

		// TODO 2025-05-20 Added cast to int when updating to matsim 2024. Gunnar
		final int binSize_s = (int) Math.round(config.travelTimeCalculator().getTraveltimeBinSize());
		final int binCnt = (int) ceil(((double) config.travelTimeCalculator().getMaxTime()) / binSize_s);

		this.data_s = new UpdatedDynamicData<Id<Link>>(0, binSize_s, binCnt);

		for (Link link : network.getLinks().values()) {
			for (int bin = 0; bin < binCnt; bin++) {
				this.data_s.put(link.getId(), bin,
						travelTimes.getLinkTravelTime(link, (bin + 0.5) * binSize_s, null, null));
			}
		}
	}

	public LinkTravelTimeCopy(final LinkTravelTimeCopy parent) {
		this.data_s = new UpdatedDynamicData<>(parent.data_s);
	}

	@Override
	public synchronized double getLinkTravelTime(Link link, double time_s, Person person, Vehicle vehicle) {
		final int bin = min(this.data_s.getBinCnt() - 1, (int) (time_s / this.data_s.getBinSize_s()));
		return this.data_s.getBinValue(link.getId(), bin);
	}

	public void multiply(final double factor) {
		this.data_s.multiply(factor);
	}

	public void add(final LinkTravelTimeCopy other, final double otherFactor) {
		this.data_s.add(other.data_s, otherFactor);
	}

	public static LinkTravelTimeCopy newWeightedSum(final List<LinkTravelTimeCopy> addends,
			final List<Double> weights) {
		assert(addends.size() == weights.size());
		final LinkTravelTimeCopy result = new LinkTravelTimeCopy(addends.get(0));
		result.multiply(weights.get(0));
		for (int i = 1; i < addends.size(); i++) {
			result.add(addends.get(i), weights.get(i));
		}
		return result;
	}

	// for testing
	public double sum() {
		double result = 0.0;
		for (Id<Link> link : this.data_s.keySet()) {
			for (int bin = 0; bin < this.data_s.getBinCnt(); bin++) {
				result += this.data_s.getBinValue(link, bin);
			}
		}
		return result;
	}
}

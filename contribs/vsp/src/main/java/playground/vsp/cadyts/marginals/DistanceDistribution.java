package playground.vsp.cadyts.marginals;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class DistanceDistribution {

	private Map<Id<DistanceBin>, DistanceBin> distanceBins = new HashMap<>();
	private final double scalingFactor;

	/**
	 * Creates an empty distance distribution
	 *
	 * @param scalingFactor scaling factor by which the population sample is scaled. If a scenario is run with a 1% sample
	 *                      of an original population the scaling factor must be set to 0.1
	 */
	public DistanceDistribution(double scalingFactor) {
		this.scalingFactor = scalingFactor;
	}

	public double getScalingFactor() {
		return scalingFactor;
	}

	public void add(String mode, double lowerLimit, double upperLimit, double standardDeviation, double value) {

		DistanceBin bin = new DistanceBin(mode, lowerLimit, upperLimit, standardDeviation, value);
		distanceBins.put(bin.getId(), bin);
	}

	public void increaseCountByOne(String mode, double distance) {
		DistanceBin distanceBin = distanceBins.values().stream()
				.filter(bin -> bin.getMode().equals(mode))
				.filter(bin -> bin.getDistanceRange().isWithinRange(distance))
				.findAny()
				.orElseThrow(() -> new RuntimeException("Could not find distance bin for: " + mode + "->" + distance));

		distanceBin.increaseCountByOne();
	}

	public Collection<DistanceBin> getDistanceBins() {
		return distanceBins.values();
	}

	public DistanceBin getBin(Id<DistanceBin> id) {
		return distanceBins.get(id);
	}

	public DistanceBin getBin(String mode, double distance) {
		return distanceBins.values().stream()
				.filter(bin -> bin.getMode().equals(mode))
				.filter(bin -> bin.getDistanceRange().isWithinRange(distance))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Could not find distance bin for: " + distance));
	}

	public DistanceDistribution copyWithEmptyBins() {
		DistanceDistribution result = new DistanceDistribution(this.scalingFactor);
		for (DistanceBin bin : distanceBins.values()) {
			result.add(bin.getMode(), bin.getDistanceRange().getLowerLimit(), bin.getDistanceRange().getUpperLimit(), bin.getStandardDeviation(), 0);
		}
		return result;
	}

    public static class DistanceBin implements Identifiable<DistanceBin> {

		private final DistanceRange distanceRange;
		private double value;
		private final double stdDev;
		private final Id<DistanceBin> id;
		private final String mode;

		private DistanceBin(String mode, double lowerLimit, double upperLimit, double standardDeviation, double value) {
			this.distanceRange = new DistanceRange(lowerLimit, upperLimit);
			this.stdDev = standardDeviation;
			this.value = value;
			this.mode = mode;
			this.id = Id.create(mode + "_" + lowerLimit + "_" + upperLimit, DistanceBin.class);
		}

		DistanceRange getDistanceRange() {
			return distanceRange;
		}

		double getValue() {
			return value;
		}

		@Override
		public Id<DistanceBin> getId() {
			return id;
		}

		double getStandardDeviation() {
			return stdDev;
		}

		public String getMode() {
			return mode;
		}

		synchronized void increaseCountByOne() {
			this.value++;
		}

		@Override
		public String toString() {
            return "Id: " + id + ", [" + distanceRange.lowerLimit + " - " + distanceRange.upperLimit + "], mode: " + mode + ", value " + value;
		}
	}

	static class DistanceRange {
		private final double lowerLimit;
		private final double upperLimit; // allow infinity for upperLimit value

		DistanceRange(double low, double high) {
			this.lowerLimit = low;
			this.upperLimit = high;
		}

		double getLowerLimit() {
			return lowerLimit;
		}

		double getUpperLimit() {
			return upperLimit;
		}

		boolean isWithinRange(double distance) {
			return lowerLimit <= distance && distance <= upperLimit;
		}

		@Override
		public String toString() {
			return "DistanceRange[" +
					"lowerLimit=" + lowerLimit +
					", upperLimit=" + upperLimit +
					']';
		}
	}
}

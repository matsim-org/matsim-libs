/**
 *
 */
package playground.johannes.eut;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelTime;

/**
 * @author illenberger
 *
 */
public class TravelTimeMemory {

	private LinkedList<TimevariantTTStorage> storageList = new LinkedList<TimevariantTTStorage>();

	private int maxMemomry = 11;

	private double learningrate = 0.1;

	public TimevariantTTStorage makeTTStorage(TravelTime ttcalc, Network network, int binsize, int starttime, int endtime) {
		TimevariantTTStorage storage = new TimevariantTTStorage(network, starttime, endtime, binsize);

		for(Link link : network.getLinks().values()) {
			for(int t = starttime; t < endtime; t += binsize) {
				storage.setLinkTravelTime(link, t, ttcalc.getLinkTravelTime(link, t));
			}
		}

		return storage;
	}

	protected LinkedList<TimevariantTTStorage> getStorageList() {
		return this.storageList;
	}

	public void setMaxMemorySlots(int slots) {
		this.maxMemomry = slots;
	}

	public int getMaxMemorySlots() {
		return this.maxMemomry;
	}

	public void setLearningRate(double rate) {
		this.learningrate = rate;
	}

	public double getLearningRate() {
		return this.learningrate;
	}

	public void appendNewStorage(TimevariantTTStorage storage) {
		this.storageList.add(storage);
		if(this.storageList.size() > this.maxMemomry) {
			TimevariantTTStorage history = this.storageList.remove();
			this.storageList.getFirst().accumulate(history, 1 - this.learningrate);
		}
	}

	public TimevariantTTStorage getTravelTimes(int index) {
		return this.storageList.get(index);
	}

	public List<TimevariantTTStorage> getTravelTimes() {
		return this.storageList;
	}

	public TravelTime getMeanTravelTimes() {
		return new MeanLinkCost(this.storageList);

	}

	private class MeanLinkCost implements TravelTime {

		private List<TimevariantTTStorage> linkcosts;

		public MeanLinkCost(List<TimevariantTTStorage> linkcosts) {
			this.linkcosts = linkcosts;
		}

		public double getLinkTravelTime(Link link, double time) {
			double sum = 0;
			for(TravelTime linkcost : this.linkcosts)
				sum += linkcost.getLinkTravelTime(link, time);

			return sum/this.linkcosts.size();
		}

	}

//	private class EvalLinkCost implements EvaluatedLinkCostI {
//
//		private MeanLinkCost meanlinkcost;
//
//		public EvalLinkCost(MeanLinkCost linkcost) {
//			meanlinkcost = linkcost;
//		}
//
//		public ScalarRandomPropertiesI getLinkCost(BasicLinkI link, int time_s) {
//			return new Properties(link, time_s);
//		}
//
//		private class Properties implements ScalarRandomPropertiesI {
//
//			private double expectation;
//
//			private double variance;
//
//			public Properties(BasicLinkI link, int time) {
//				expectation = meanlinkcost.getLinkTravelCost(link, time);
//
//				double sum = 0;
//				for (RoutableLinkCostI cost : meanlinkcost.linkcosts)
//					sum += Math.pow(cost.getLinkTravelCost(link, time) - expectation, 2);
//
//				variance = Math.sqrt((1.0 / (meanlinkcost.linkcosts.size() - 1)) * sum);
//			}
//
//			public double getExpectation() {
//				return expectation;
//			}
//
//			public double getVariance() {
//				return variance;
//			}
//		}
//
//	}
}

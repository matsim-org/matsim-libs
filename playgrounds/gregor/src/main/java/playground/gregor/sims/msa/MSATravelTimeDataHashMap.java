package playground.gregor.sims.msa;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.trafficmonitoring.TravelTimeDataHashMap;
import org.matsim.core.utils.misc.IntegerCache;

public class MSATravelTimeDataHashMap extends TravelTimeDataHashMap {

	private final int binSize;
	private final Map<Integer,Double> msaTravelTimes;
	private int msaIt = 0;

	public MSATravelTimeDataHashMap(Link link, int binSize) {
		super(link);
		this.binSize = binSize;
		this.msaTravelTimes = new ConcurrentHashMap<Integer,Double>();
	}


	@Override
	public void resetTravelTimes() {
		double oldCoef = this.msaIt/(1.+this.msaIt);
		double newCoef = 1./(1.+this.msaIt);
		for (Entry<Integer, Double> e : this.msaTravelTimes.entrySet()) {
			double time = getTimeFromSlotIdx(e.getKey());
			double newTime = Math.min(super.link.getLength()/0.01,super.getTravelTime(e.getKey(), time));
			double oldTime = e.getValue();
			e.setValue(oldCoef * oldTime + newCoef * newTime);
		}
		this.msaIt++;
		super.resetTravelTimes();
	}
	
	@Override
	public double getTravelTime(final int timeSlice, final double now) {
		 Double ret = this.msaTravelTimes.get(IntegerCache.getInteger(timeSlice));
		 if (ret == null) {
			 ret = super.getTravelTime(timeSlice, now);
			 this.msaTravelTimes.put(timeSlice, ret);
		 }
		 return ret;
	}
	
	private double getTimeFromSlotIdx(int timeSlice) {
		return timeSlice * this.binSize;
	}

}

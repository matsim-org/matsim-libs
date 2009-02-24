package playground.andreas.intersection.dijkstra;

import java.util.HashMap;

import org.matsim.network.Link;
import org.matsim.trafficmonitoring.TravelTimeData;
import org.matsim.utils.misc.IntegerCache;

// No use for that one, ask Gregor
public class MyTravelTimeRole implements TravelTimeData {
	
	private final HashMap<Integer, TimeStruct> travelTimes;
	private final Link link;

	public MyTravelTimeRole(final Link link, final int numSlots) {
		this.travelTimes = new HashMap<Integer, TimeStruct>();
		this.link = link;
		resetTravelTimes();
	}

	public void resetTravelTimes() {
		this.travelTimes.clear();
	}

	public void addTravelTime(final int timeSlice, final double traveltime) {
		TimeStruct curr = this.travelTimes.get(IntegerCache.getInteger(timeSlice));
		if (curr != null) {
			curr.cnt += 1;
			curr.timeSum += traveltime;
		} else {
			this.travelTimes.put(IntegerCache.getInteger(timeSlice), new TimeStruct(traveltime, 1));
		}
	}

	public double getTravelTime(final int timeSlice, final double now) {
		TimeStruct ts = this.travelTimes.get(IntegerCache.getInteger(timeSlice));
		if (ts == null) {
			return this.link.getLength() / this.link.getFreespeed(now);
		}

		return ts.timeSum / ts.cnt;
	}

	private static class TimeStruct {
		public double timeSum;
		public int cnt;

		public TimeStruct(final double timeSum, final int cnt) {
			this.cnt = cnt;
			this.timeSum = timeSum;
		}
	}

}
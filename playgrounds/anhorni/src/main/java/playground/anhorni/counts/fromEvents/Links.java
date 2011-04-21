package playground.anhorni.counts.fromEvents;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

public class Links {
	
	private final TreeMap<Id, Double> linkCounts = new TreeMap<Id, Double>();
	
	public void incrementLinkEnterCount(Id id, double increment) {
		double prevLinkCount = 0.0;
		if (this.linkCounts.get(id) != null) {
			prevLinkCount = this.linkCounts.get(id);
		}
		this.linkCounts.put(id, prevLinkCount + increment);
	}
	
	public double getLinkEnterCount(Id id) {
		if (this.linkCounts.get(id) == null) return 0.0;
		return this.linkCounts.get(id);
	}
	
	public void setLinkEnterCount(Id id, double count) {
		this.linkCounts.put(id, count);
	}

}

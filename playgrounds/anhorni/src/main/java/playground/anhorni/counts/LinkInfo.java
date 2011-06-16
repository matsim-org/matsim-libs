package playground.anhorni.counts;

import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

public class LinkInfo {
		
	private TreeMap<String, String> ids = new TreeMap<String, String>();
	private TreeMap<Integer, List<Double>> yearCountVals = new TreeMap<Integer, List<Double>>();
	
	private TreeMap<Integer, List<Double>> dailyCountVals = new TreeMap<Integer, List<Double>>();
	private TreeMap<Integer, Double> simVals =  new TreeMap<Integer, Double>();
	
	private boolean removeZeroVolumes = false;
	
	private Aggregator aggregator = new Aggregator();
	
	public LinkInfo(String direction, String linkidTeleatlas,
			String linkidNavteq, String linkidIVTCH, boolean removeZeroVolumes) {	
		this.ids.put("teleatlas", linkidTeleatlas);
		this.ids.put("navteq", linkidNavteq);
		this.ids.put("ivtch", linkidIVTCH);
		
		for (int h = 0; h < 24; h++) {
			this.yearCountVals.put(h, new Vector<Double>());
		}
		
		this.removeZeroVolumes = removeZeroVolumes;
	}
	
	public double getAbsoluteDifference_TimeAvg(int hour) {
		return Math.abs(this.getSimVal(hour) - this.aggregator.getAvg()[hour]);
	}
	
	public double getAbsoluteStandardDev_TimeAvg(int hour) {
		return this.aggregator.getStandarddev()[hour];
	}
	
	public double getRelativeError_TimeAvg(int hour) {
		return Math.abs((this.getSimVal(hour) - this.aggregator.getAvg()[hour]) / this.aggregator.getAvg()[hour]);
	}
	
	public double getRelativeStandardDeviation(int hour) {
		return Math.abs(this.aggregator.getStandarddev()[hour] / this.aggregator.getAvg()[hour]); 
	}
	
	public boolean addSimValforLinkId(String networkName, String linkId, int hour, double simVal) {
		if (ids.get(networkName).equals(linkId)) {
			this.addSimVal(hour, simVal);
			return true;
		}
		return false;
	}
	
	public void addYearCountVal(int hour, double count) {
		if (this.yearCountVals.get(hour) == null) {
			this.yearCountVals.put(hour, new Vector<Double>());
		}
		if (!(this.removeZeroVolumes && count < 0.0)) {
			this.yearCountVals.get(hour).add(count);
		}
	}
	
	public void addDailyCountVal(int date, double count) {
		if (this.dailyCountVals.get(date) == null) {
			this.dailyCountVals.put(date, new Vector<Double>());
		}
		if (!(this.removeZeroVolumes && count < 0.0)) {
			this.dailyCountVals.get(date).add(count);
		}
	}
	
	public void aggregate(boolean removeOutliers) {
		this.aggregator.aggregate(this.yearCountVals, this.dailyCountVals, removeOutliers);
	}
	
	public String getLinkidTeleatlas() {
		return this.ids.get("teleatlas");
	}

	public String getLinkidNavteq() {
		return this.ids.get("navteq");
	}

	public String getLinkidIVTCH() {
		return this.ids.get("ivtch");
	}
	
	public void addSimVal(int hour, double simVal) {
		this.simVals.put(hour, simVal);
	}

	public Aggregator getAggregator() {
		return aggregator;
	}

	public void setAggregator(Aggregator aggreagtor) {
		this.aggregator = aggreagtor;
	}
	
	public double getSimVal(int hour) {
		if (this.simVals.get(hour) == null) return -1.0;
		else return this.simVals.get(hour);
	}

	public TreeMap<Integer, Double> getSimVals() {
		return simVals;
	}

	public void setSimVals(TreeMap<Integer, Double> simVals) {
		this.simVals = simVals;
	}
}

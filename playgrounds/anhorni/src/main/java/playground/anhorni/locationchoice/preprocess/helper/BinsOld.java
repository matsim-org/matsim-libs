package playground.anhorni.locationchoice.preprocess.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


public class BinsOld {
	
	private int interval;
	private int numberOfBins;
	private ArrayList<List<Double>> bins = new ArrayList<List<Double>>();
	private double maxVal;
	
	public BinsOld(int interval, double maxVal) {
		this.interval = interval;
		this.maxVal = maxVal;
		this.numberOfBins = (int)(maxVal / interval);
		for (int i = 0; i < numberOfBins; i++) {
			bins.add(new Vector<Double>());
		}
	}
	
	public void addVal(double indexVal, double val) {
		indexVal = Math.min(maxVal -1.0, indexVal);
		int index = (int)Math.floor(indexVal / interval);
		this.bins.get(index).add(val);
	}
	
	public ArrayList<Double> getAvg() {	
		ArrayList<Double> binsAvg = new ArrayList<Double>();
		
		for (int i = 0; i < numberOfBins; i++) {
			binsAvg.add(0.0);
		}
		
		for (int i = 0; i < numberOfBins; i++) {
			Iterator<Double> it = this.bins.get(i).iterator();
			while (it.hasNext()) {
				double oldVal = binsAvg.get(i);
				binsAvg.set(i, oldVal + it.next() / this.bins.get(i).size());
			}
		}
		return binsAvg;
	}
	
	public double [] getSizes() {
		double [] seizes = new double[numberOfBins];
		
		for (int i = 0; i < numberOfBins; i++) {
			seizes[i] = 0.0;
		}
		
		for (int i = 0; i < numberOfBins; i++) {
			seizes[i] = this.bins.get(i).size();
		}
		return seizes;
	}
			
	public ArrayList<Double> getMedian() {
		ArrayList<Double> binsMedian = new ArrayList<Double>();
		
		for (int i = 0; i < numberOfBins; i++) {
			binsMedian.add(0.0);
		}
		
		for (int i = 0; i < numberOfBins; i++) {
			binsMedian.set(i, Utils.median(this.bins.get(i)));	
		}
		return binsMedian;
	}
}

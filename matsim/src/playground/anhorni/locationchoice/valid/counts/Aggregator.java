package playground.anhorni.locationchoice.valid.counts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;

public class Aggregator {
	
	private final static Logger log = Logger.getLogger(Aggregator.class);
	
	private TreeMap<Integer, List<Double>> volumes1 = new TreeMap<Integer, List<Double>>();
	private TreeMap<Integer, List<Double>> volumes2 = new TreeMap<Integer, List<Double>>();
	
	private double [][] avg = {{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
			{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}};
	
	private double [][] median = {{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
		0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
		{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}};
	
	private double [][] standarddev = {{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
		0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, 
		{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}};
		
	
	public void aggregate(List<RawCount> counts) {
		this.fillVolumes(counts);
		
		for (int i = 0; i < 2; i++) {
			this.removeOutliers(i);
			this.sum(i);
			this.standarddev(i);
		}
		this.median();	
	}
	
	private void fillVolumes(List<RawCount> counts) {
		for (int i = 0; i < 24; i++) {
			volumes1.put(i, new ArrayList<Double>());
			volumes2.put(i, new ArrayList<Double>());
		}
		Iterator<RawCount> count_it = counts.iterator();
		while (count_it.hasNext()) {
			RawCount rawCount = count_it.next();		
			int hour = rawCount.getHour()-1;
			if (hour < 24) {				
				if (rawCount.getVol1() > -1.0) {
					volumes1.get(hour).add(rawCount.getVol1());
				}
				if (rawCount.getVol2() > -1.0) {
					volumes2.get(hour).add(rawCount.getVol2());
				}
			}
		}
	}
	
	private void removeOutliers(int direction) {
		TreeMap<Integer, List<Double>> volumes;
		if (direction == 0) {
			volumes = volumes1;
		}
		else  {
			volumes = volumes2;
		}
				
		for (int hour = 0; hour < 24; hour++) {		
			List<Double> filteredVolumes = new Vector<Double>();		
			List<Double> volumesTemp = volumes.get(hour);
			Collections.sort(volumesTemp);
						
			int numberOfElements = volumesTemp.size();
			int numberOfElements2RemovePerSide = (int)Math.ceil(numberOfElements * 0.05 /2.0);

			if (volumesTemp.size() > 0) {			
				filteredVolumes.addAll(volumesTemp.subList(
						numberOfElements2RemovePerSide, (int)Math.floor(numberOfElements - 1 - numberOfElements2RemovePerSide)));	
				
				volumes.get(hour).clear();
				volumes.get(hour).addAll(filteredVolumes);
			}
		}
	}
	
	private void sum(int direction) {	
		TreeMap<Integer, List<Double>> volumes;
		if (direction == 0) {
			volumes = volumes1;
		}
		else  {
			volumes = volumes2;
		}	
		for (int hour = 0; hour < 24; hour++) {
			Iterator<Double> vol_it;
			vol_it = volumes.get(hour).iterator();
			while (vol_it.hasNext()) {
				double vol = vol_it.next();		
				avg[direction][hour] += vol;
			}
		}
		for (int i = 0; i < 24; i++) {
			avg[direction][i] /= volumes.get(i).size();
		}	
	}
	
	private void standarddev(int direction) {
		TreeMap<Integer, List<Double>> volumes;
		if (direction == 0) {
			volumes = volumes1;
		}
		else  {
			volumes = volumes2;
		}
		for (int hour = 0; hour < 24; hour++) {
			Iterator<Double> vol_it;
			vol_it = volumes.get(hour).iterator();
			while (vol_it.hasNext()) {
				double vol = vol_it.next();		
				if (vol > -1.0) {
					standarddev[direction][hour] += Math.pow(vol - avg[direction][hour] , 2.0);
				}
			}
		}
		for (int i = 0; i < 24; i++) {
			standarddev[direction][i] = Math.sqrt(standarddev[direction][i] / (volumes.get(i).size() -1));
		}	
	}
		
	private void median() {
		for (int i = 0; i < 24; i++) {
			median[0][i] = this.medianPerHour(volumes1.get(i));
			median[1][i] = this.medianPerHour(volumes2.get(i));
		}
	}
	
	private double medianPerHour(List<Double> values) {
		
		if (values.size() == 0) return 0.0;
		
	    Collections.sort(values);
	 
	    if (values.size() % 2 == 1) {
	    	return values.get((values.size()+1)/2-1);
	    }
	    else {
	    	double lower = values.get(values.size()/2-1);
	    	double upper = values.get(values.size()/2);
	    	return (lower + upper) / 2.0;
	    }	
	}

	public double[][] getAvg() {
		return avg;
	}

	public void setAvg(double[][] avg) {
		this.avg = avg;
	}

	public double[][] getMedian() {
		return median;
	}

	public void setMedian(double[][] median) {
		this.median = median;
	}

	public double[][] getStandarddev() {
		return standarddev;
	}

	public void setStandarddev(double[][] standarddev) {
		this.standarddev = standarddev;
	}	
}

package playground.anhorni.counts;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

//import org.apache.log4j.Logger;

public class Aggregator {
	
	//private final static Logger log = Logger.getLogger(Aggregator.class);
	
	private TreeMap<Integer, List<Double>> volumes = new TreeMap<Integer, List<Double>>();
		
	private double [] avg = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
	private double [] median = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
		0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
	
	private double [] standarddev = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
		0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		
	
	public void aggregate(TreeMap<Integer, List<Double>> volumes, boolean removeOutliers) {
		this.volumes = volumes;		
		if (removeOutliers) this.removeOutliers();
		this.avg();
		this.standarddev();
		this.median();	
	}
	
	public int getSize(int hour) {
		return this.volumes.get(hour).size();
	}
	
		
	private void removeOutliers() {
				
		for (int hour = 0; hour < 24; hour++) {		
			List<Double> filteredVolumes = new Vector<Double>();		
			List<Double> volumesTemp = volumes.get(hour);
			Collections.sort(volumesTemp);
			
				//This exactly reproduces the former PERL script
						
//						int numberOfElements = volumesTemp.size();
//						int numberOfElements2Remove = (int)Math.ceil(numberOfElements * 0.05);
//			
//						if (volumesTemp.size() > 0) {			
//							filteredVolumes.addAll(volumesTemp.subList(
//									(int)Math.ceil(numberOfElements2Remove/2.0), (int)Math.floor(numberOfElements - numberOfElements2Remove/2.0)));	
//							
//							volumes.get(hour).clear();
//							volumes.get(hour).addAll(filteredVolumes);
//						}
			
			int numberOfElements = volumesTemp.size();
			int numberOfElements2RemovePerSide = (int)Math.round(numberOfElements * 0.05 / 2.0);

			if (volumesTemp.size() > 0) {
				
				// List<E> subList(int fromIndex, int toIndex);
				// @param fromIndex low endpoint (inclusive) of the subList.
			    // @param toIndex high endpoint (exclusive) of the subList.
				
				filteredVolumes.addAll(volumesTemp.subList(
						numberOfElements2RemovePerSide, (int)Math.round(numberOfElements - numberOfElements2RemovePerSide)));	
								
				volumes.get(hour).clear();
				volumes.get(hour).addAll(filteredVolumes);
			}
		}
	}
	
	private void avg() {	
		for (int hour = 0; hour < 24; hour++) {
			Iterator<Double> vol_it;
			vol_it = volumes.get(hour).iterator();
			while (vol_it.hasNext()) {
				double vol = vol_it.next();		
				avg[hour] += vol;
			}
		}
		for (int i = 0; i < 24; i++) {
			avg[i] /= volumes.get(i).size();
		}	
	}
	
	private void standarddev() {
		for (int hour = 0; hour < 24; hour++) {
			Iterator<Double> vol_it;
			vol_it = volumes.get(hour).iterator();
			while (vol_it.hasNext()) {
				double vol = vol_it.next();		
				if (vol > -1.0) {
					standarddev[hour] += Math.pow(vol - avg[hour] , 2.0);
				}
			}
		}
		for (int i = 0; i < 24; i++) {
			standarddev[i] = Math.sqrt(standarddev[i] / (volumes.get(i).size() -1));
		}	
	}
		
	private void median() {
		for (int i = 0; i < 24; i++) {
			median[i] = this.medianPerHour(volumes.get(i));
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

	public double[] getAvg() {
		return avg;
	}
	public double[] getMedian() {
		return median;
	}
	public double[] getStandarddev() {
		return standarddev;
	}	
}

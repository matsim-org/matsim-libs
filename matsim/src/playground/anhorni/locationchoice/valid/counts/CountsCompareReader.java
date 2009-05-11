package playground.anhorni.locationchoice.valid.counts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.matsim.core.gbl.Gbl;

public class CountsCompareReader {
	
	String countsCompareFile = "input/counts/countscompare.txt";
	private TreeMap<Integer, List<Double>> volumes = new TreeMap<Integer, List<Double>>();
	
	
	public void read() {
		try {
			FileReader fileReader = new FileReader(countsCompareFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);	
			
			String curr_line = bufferedReader.readLine(); // Skip header
			while ((curr_line = bufferedReader.readLine()) != null) {
				
				String[] entries = curr_line.split("\t", -1);
				int hour = Integer.parseInt( entries[1].trim()) - 1;
				double matsimVol = Double.parseDouble( entries[2].trim());
				
				if (volumes.get(hour) == null) {
					volumes.put(hour, new Vector<Double>());
				}
				volumes.get(hour).add(matsimVol);				
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	public Double [] getAvgs() {
		Double [] avgs = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		
		for (int hour = 0; hour < 24; hour++) {
			Iterator<Double> vol_it;
			vol_it = volumes.get(hour).iterator();
			while (vol_it.hasNext()) {
				double vol = vol_it.next();		
				avgs[hour] += vol;
			}
		}
		for (int i = 0; i < 24; i++) {
			avgs[i] /= volumes.get(i).size();
		}	
		return avgs;
	}
	
	
	public Double [] getMedians() {
		Double [] medians = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		
		for (int hour = 0; hour < 24; hour++) {
			 medians[hour] = this.medianPerHour(volumes.get(hour));
		}
		return medians;
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
}

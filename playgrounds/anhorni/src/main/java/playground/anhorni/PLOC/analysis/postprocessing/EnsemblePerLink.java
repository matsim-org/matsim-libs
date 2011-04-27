package playground.anhorni.PLOC.analysis.postprocessing;

import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

public class EnsemblePerLink {
	private final static Logger log = Logger.getLogger(EnsemblePerLink.class);
	private Id id;
	private TreeMap<Integer, List<Double>> simVolumes = new TreeMap<Integer, List<Double>>();
	
	public EnsemblePerLink(Id id) {
		this.id = id;
	}
	
	public void addVolume(int hour, double volume) {
		if (this.simVolumes.get(hour) == null) this.simVolumes.put(hour, new Vector<Double>());
		this.simVolumes.get(hour).add(volume);
	}
	
	public double getAverageVolume(int hour) {
		if (this.simVolumes.get(hour).size() == 0) return 0.0; 
		double sum = 0.0;
		for (int i = 0; i < this.simVolumes.get(hour).size(); i++) {
			sum += this.simVolumes.get(hour).get(i);
		}
		return sum / this.simVolumes.get(hour).size();
	}
	
	public boolean hasVolumes(int hour) {
		return this.simVolumes.get(hour).size() > 0;
	}
	
	public double getStandardDev_s(int hour) {
		if (this.simVolumes.get(hour).size() == 0) {
			log.warn("Link without volumes!");
			return 0.0;
		}
		double mean = this.getAverageVolume(hour);
		double variance = 0.0;
		double n = this.simVolumes.get(hour).size();
		for (int i = 0; i < n; i++) {
			variance += Math.pow(this.simVolumes.get(hour).get(i) - mean, 2.0) / (n - 1);
		}
		return Math.sqrt(variance);
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}
}

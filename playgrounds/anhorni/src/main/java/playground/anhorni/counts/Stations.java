package playground.anhorni.counts;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

public class Stations {
	
	private final static Logger log = Logger.getLogger(Stations.class);
	List<CountStation> countStations = new Vector<CountStation>();

	public List<CountStation> getCountStations() {
		return countStations;
	}

	public void setCountStations(List<CountStation> countStations) {
		this.countStations = countStations;
	}
	
	
	public boolean addSimValforLinkId(String networkName, String linkId, int hour, double simVal) {
		Iterator<CountStation> station_it = this.countStations.iterator();
		while (station_it.hasNext()) {
			CountStation station = station_it.next();	
			if (station.addSimValforLinkId(networkName, linkId, hour, simVal)) {
				return true;
			}
		}
		return false;
	}
}

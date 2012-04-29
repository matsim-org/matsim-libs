/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.anhorni.counts;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class Stations {
	List<CountStation> countStations = new Vector<CountStation>();

	public List<CountStation> getCountStations() {
		return countStations;
	}

	public void setCountStations(List<CountStation> countStations) {
		this.countStations = countStations;
	}
	
	public void addCountStation(CountStation station) {
		this.countStations.add(station);
	}
	
	public void removeCountStation(CountStation station) {
		this.countStations.remove(station);
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

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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

public class Converter {
	
	Counts countsIVTCH = new Counts();
	Counts countsNavteq = new Counts();
	Counts countsTeleatlas = new Counts();
		
	public void convert(List<CountStation> incounts) {
		
		Iterator<CountStation> countStation_it = incounts.iterator();
		while (countStation_it.hasNext()) {
			CountStation countStation = countStation_it.next();
			
			this.createCounts(countStation, countsIVTCH, 
					Id.create(countStation.getLink1().getLinkidIVTCH(), Link.class),
					Id.create(countStation.getLink2().getLinkidIVTCH(), Link.class));
									
			this.createCounts(countStation, countsNavteq, 
					Id.create(countStation.getLink1().getLinkidNavteq(), Link.class),
					Id.create(countStation.getLink2().getLinkidNavteq(), Link.class));
			
			this.createCounts(countStation, countsTeleatlas, 
					Id.create(countStation.getLink1().getLinkidTeleatlas(), Link.class),
					Id.create(countStation.getLink2().getLinkidTeleatlas(), Link.class));			
		}		
	}
	
	private void createCounts(CountStation countStation, Counts counts, Id<Link> locId1, Id<Link> locId2) {
		
//		if (countStation.getId().equals("ASTRA066")) {
//			log.info("SIZE ----------: " + countStation.getLink1().getAggregator().getSize(0));
//			log.info("locs ----------: " + locId1.toString());
//			log.info("STUNDE0: " + countStation.getLink1().getAggregator().getAvg()[0]);
//			log.info("STUNDE7: " + countStation.getLink1().getAggregator().getAvg()[7]);
//		}
				
		if (locId1.compareTo(Id.create("-", Link.class)) == 0 || locId2.compareTo(Id.create("-", Link.class)) == 0) {
			return;
		}
		
		Count count0 = counts.createAndAddCount(locId1, countStation.getId());		
		if (count0 != null) {
			for (int i = 0; i < 24; i++) {
				count0.createVolume(i+1, countStation.getLink1().getAggregator().getAvg()[i]);
			}
		}
		
		Count count1 = counts.createAndAddCount(locId2, countStation.getId());
		if (count1 != null) {
			for (int i = 0; i < 24; i++) {
				count1.createVolume(i+1, countStation.getLink2().getAggregator().getAvg()[i]);
			}
		}
	}

	public Counts getCountsIVTCH() {
		return countsIVTCH;
	}

	public void setCountsIVTCH(Counts countsIVTCH) {
		this.countsIVTCH = countsIVTCH;
	}

	public Counts getCountsNavteq() {
		return countsNavteq;
	}

	public void setCountsNavteq(Counts countsNavteq) {
		this.countsNavteq = countsNavteq;
	}

	public Counts getCountsTeleatlas() {
		return countsTeleatlas;
	}

	public void setCountsTeleatlas(Counts countsTeleatlas) {
		this.countsTeleatlas = countsTeleatlas;
	}	
}

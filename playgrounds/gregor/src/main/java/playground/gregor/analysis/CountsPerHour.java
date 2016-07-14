/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.Volume;

public class CountsPerHour implements LinkEnterEventHandler {

	private final double hFrom = 16*3600;
	private final double hTo = 17*3600;
	private Counts c;
	
	private final Map<Id<Link>,CntVal> cnts = new HashMap<>();
	
	public CountsPerHour(Counts c) {
		
		this.c = c;
	}
	


	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getTime() < hFrom || event.getTime() >= hTo) {
			return;
		}
		if (this.c.getCounts().containsKey(event.getLinkId())) {
			CntVal val = cnts.get(event.getLinkId());
			if (val == null) {
				val = new CntVal();
				this.cnts.put(event.getLinkId(), val);
			}
			val.val++;
		}
		
	}
	
	
	private final class CntVal {
		int val = 0;
	}
	
	public static void main (String [] args) {
		String eFile = "/Users/laemmel/devel/nyc/output2/ITERS/it.99/99.events.xml.gz";
		String cFile = "/Users/laemmel/devel/nyc/gct_vicinity/counts.xml.gz";
		
		Counts c = new Counts();
		new CountsReaderMatsimV1(c).parse(cFile);
		
		CountsPerHour cph = new CountsPerHour(c);
		EventsManagerImpl em = new EventsManagerImpl();
		em.addHandler(cph);
		
		new EventsReaderXMLv1(em).parse(eFile);
		
		
		List<Result> results = new ArrayList<>();
		double totalDiff = 0;
		for (Entry<Id<Link>, CntVal> e : cph.cnts.entrySet()) {
			Count count = c.getCount(e.getKey());
			double cval = count.getVolume(16).getValue();
			double diff = (cval-e.getValue().val);
//			System.out.println(e.getKey() + "  " + e.getValue().val + " " + cval + "  " + diff);
			totalDiff += diff; //Math.abs(diff);
			Result r = new Result();
			r.cval = cval;
			r.sval = e.getValue().val;
			r.err = r.cval/r.sval;
			r.diff = diff;
			r.id = Integer.parseInt(e.getKey().toString());
			results.add(r);
		}
		Collections.sort(results);
		int usable = 0;
		for (Result r : results) {
			System.out.println(r.id + " " + r.sval + " " + r.cval + " " + r.diff + " " + r.err);
			if (r.err >= 0.7 && r.err <= 1.42) {
				usable++;
			}
		}
		System.out.println(totalDiff + " " +results.size() + " " + usable);
		
	}
	
	private static final class Result implements Comparable<Result>{
		double cval;
		double sval;
		double diff;
		int id;
		double err;
		
		@Override
		public int compareTo(Result o) {
			if (err < o.err) {
				return -1;
			} 
			if (err > o.err) {
				return 1;
			}
			return 0;
		}
		
	}
}

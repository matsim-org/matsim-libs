/* *********************************************************************** *
 * project: org.matsim.*
 * TTOverIteration.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;

public class TTOverIteration  implements PersonDepartureEventHandler, PersonArrivalEventHandler{

	private int cnt;
	private double tt;

	private final Map<Id,Double> deps = new HashMap<Id,Double>();


	public static void main (String [] args) throws IOException {


		{
			String baseDir = "/Users/laemmel/devel/hhw_hybrid/output_SO/ITERS/";

			List<Double> tt = new ArrayList<Double>();
			for (int i = 0; i <= 100; i++) {
				String dir = baseDir + "it." + i + "/" + i + ".events.xml.gz";
				EventsManager e = new EventsManagerImpl();
				TTOverIteration t = new TTOverIteration();
				e.addHandler(t);
				EventsReaderXMLv1 r = new EventsReaderXMLv1(e);
				r.parse(dir);
				tt.add(t.getAvgTT());
				System.out.println(t.getAvgTT());

			}
			BufferedWriter bf = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/hhw_hybrid/ttCarSO")));
			for (int i = 0; i<= 100; i++) {
				System.out.println(i + " " + tt.get(i));
				bf.append(i + " " + tt.get(i) + "\n");
			}
			bf.close();
		}
		{
			String baseDir = "/Users/laemmel/devel/hhw_hybrid/output_NE/ITERS/";

			List<Double> tt = new ArrayList<Double>();
			for (int i = 0; i <= 100; i++) {
				String dir = baseDir + "it." + i + "/" + i + ".events.xml.gz";
				EventsManager e = new EventsManagerImpl();
				TTOverIteration t = new TTOverIteration();
				e.addHandler(t);
				EventsReaderXMLv1 r = new EventsReaderXMLv1(e);
				r.parse(dir);
				tt.add(t.getAvgTT());
				System.out.println(t.getAvgTT());

			}
			BufferedWriter bf = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/hhw_hybrid/ttCarNE")));
			for (int i = 0; i<= 100; i++) {
				System.out.println(i + " " + tt.get(i));
				bf.append(i + " " + tt.get(i) + "\n");
			}
			bf.close();
		}
	}



	private double getAvgTT() {
		return this.tt/this.cnt;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double e = this.deps.remove(event.getPersonId());
		if (event.getPersonId().toString().contains("car")){
			this.tt += event.getTime()-e;
			this.cnt++;
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.deps.put(event.getPersonId(), event.getTime());

	}
}

package playground.gregor.abmus2016;
/* *********************************************************************** *
 * project: org.matsim.*
 *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by laemmel on 01/02/16.
 */
public class TravelTimes implements PersonDepartureEventHandler, PersonArrivalEventHandler {


	private final Id<Link> from1 = Id.createLinkId("l25");
	private final Id<Link> to2 = Id.createLinkId("l24");

	private final BufferedWriter bf1;
	private final BufferedWriter bf2;
	private final Set<Id> handled = new HashSet<>();
	private Map<Id, PersonDepartureEvent> ps = new HashMap<>();

	public TravelTimes(String arr, String dep) throws IOException {
		this.bf1 = new BufferedWriter(new FileWriter(new File(arr)));
		this.bf2 = new BufferedWriter(new FileWriter(new File(dep)));
	}

	public static void main(String[] args) throws IOException {
		String baseDir = "/Users/laemmel/svn/unimb/ABMUS2016/scenario/results/";
//		String scenario = "base_scenario_NE/";
		String scenario = "separated_OPT/";

		String it = "49";

		String baseOutputDir = "/Users/laemmel/svn/unimb/ABMUS2016/paper/figs/plots/";

		String scDir = baseDir + scenario;
		String outDir = baseOutputDir + scenario + "it." + it + "/";
//
//		String cf = scDir+"output_config.xml.gz";
//
//		Config c = ConfigUtils.createConfig();
//		ConfigUtils.loadConfig(c,cf);
//
//		c.network().setInputFile(scDir+"/output_network.xml.gz");
//		c.p
		EventsManager em = new EventsManagerImpl();
		TravelTimes tt = new TravelTimes(outDir + "tt_arr", outDir + "tt_dep");
		em.addHandler(tt);

		EventsReaderXMLv1 r = new EventsReaderXMLv1(em);
		r.parse(scDir + "it." + it + "/" + it + ".events.xml.gz");

		tt.done();
	}

	public void done() throws IOException {
		this.bf1.close();
		this.bf2.close();
		for (Id id : handled) {
			System.out.println(id);
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		PersonDepartureEvent dep = ps.remove(event.getPersonId());
		double tt = event.getTime() - dep.getTime();
		try {
			if (dep.getLinkId().equals(from1)) {
				this.bf1.append(tt + "");
				this.bf1.append('\n');
			}
			else {
				if (event.getLinkId().equals(to2)) {
					this.bf2.append(tt + "");
					this.bf2.append('\n');
				}
				else {
					throw new RuntimeException();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		handled.add(event.getLinkId());
		ps.put(event.getPersonId(), event);

	}

	@Override
	public void reset(int iteration) {

	}
}

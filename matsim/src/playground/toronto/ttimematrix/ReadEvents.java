/* *********************************************************************** *
 * project: org.matsim.*
 * ReadEvents.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.toronto.ttimematrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.Events;
import org.matsim.events.EventsReaderTXTv1;
import org.matsim.gbl.Gbl;


public class ReadEvents {
	
	private static final Map<Id,Id> parseL2ZMapping(String infile) {
		Map<Id,Id> l2zMapping = new HashMap<Id,Id>();
		try {
			FileReader fr = new FileReader(infile);
			BufferedReader br = new BufferedReader(fr);

			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// lid  zid
				// 0    1
				Id lid = new IdImpl(entries[0]);
				Id zid = new IdImpl(entries[1]);
				l2zMapping.put(lid,zid);
			}
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		return l2zMapping;
	}

	public static void main(String[] args) {
		// input arguments
		String eventsfile = "../../input/events.txt.gz";
		String mapfile = "../../input/l2z-mapping.txt";
		int[] hours = {7,8,9,17,18,19};
		String outfile = "../../output/ttimes.txt";

		Events events = new Events();
		TTimeMatrixCalculator ttmc = new TTimeMatrixCalculator(parseL2ZMapping(mapfile),hours);
		events.addHandler(ttmc);
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		reader.readFile(eventsfile);
		ttmc.writeMatrix(outfile);
	}
}

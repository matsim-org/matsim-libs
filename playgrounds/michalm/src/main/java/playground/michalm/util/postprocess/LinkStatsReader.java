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

package playground.michalm.util.postprocess;

import java.util.*;

import org.matsim.core.utils.io.IOUtils;

public class LinkStatsReader {
	public static class LinkStats {
		String linkId, fromId, toId;
		double length, freeSpeed, capacity;

		double[] hrs;
		double dailyHrs;
		double[] tt;
	}

	public static List<? extends LinkStats> readLinkStats(String file) {
		List<LinkStats> linkStatsList = new ArrayList<>();
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(IOUtils.getBufferedReader(file)).useLocale(Locale.ENGLISH);

		// ============ HEADER ==========
		// LINK ORIG_ID FROM TO LENGTH FREESPEED CAPACITY
		// HRS0-1min HRS0-1avg HRS0-1max
		// ...............
		// HRS23-24min HRS23-24avg HRS23-24max
		// HRS0-24min HRS0-24avg HRS0-24max
		// TRAVELTIME0-1min TRAVELTIME0-1avg TRAVELTIME0-1max
		// ...............
		// TRAVELTIME23-24min TRAVELTIME23-24avg TRAVELTIME23-24max

		sc.nextLine();// skip header

		while (sc.hasNext()) {
			LinkStats ls = new LinkStats();

			ls.linkId = sc.next();
			// ORIG_ID is unused (obsolete column)
			ls.fromId = sc.next();
			ls.toId = sc.next();

			ls.length = sc.nextDouble();
			ls.freeSpeed = sc.nextDouble();
			ls.capacity = sc.nextDouble();

			// ========== VOLUMES ===========
			double[] hrs = ls.hrs = new double[24];
			for (int i = 0; i < 24; i++) {
				sc.next();// skip HRS min
				hrs[i] = sc.nextDouble();// HRS avg
				sc.next();// skip HRS max
			}

			sc.next();// skip HRS min
			ls.dailyHrs = sc.nextDouble();// HRS avg
			sc.next();// skip HRS max

			// ========== TRAVE TIMES ===========

			double[] tt = ls.tt = new double[24];
			for (int i = 0; i < 24; i++) {
				sc.next();// skip TT min
				tt[i] = sc.nextDouble();// TT avg
				sc.next();// skip TT max
			}

			linkStatsList.add(ls);
		}

		sc.close();
		return linkStatsList;
	}
}

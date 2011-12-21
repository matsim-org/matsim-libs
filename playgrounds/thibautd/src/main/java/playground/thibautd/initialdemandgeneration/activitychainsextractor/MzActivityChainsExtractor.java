/* *********************************************************************** *
 * project: org.matsim.*
 * Mz2000ActivityChainsExtractor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.activitychainsextractor;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;

/**
 * Extracts activity chains from the MZ 2000. Activities do not exist in the MZ
 * as such, and must be deduced from the trip purpose. Moreover, contrary to the
 * MZ 2005, the MZ 2000 do not provide coordinates, and for most of departure/arrivals,
 * the precision is at the ZIP-code level. This implies that the home activities
 * detection has to be based on cruder hypothesis than for the MZ 2005 (where
 * simple coordinates comparison is fine)
 *
 * Thus, the following assumptions are used:
 * <ul>
 * <li> the first activity of the day is a home activity
 * <li> if there exists a sequence of trips of same purpose, and that the localisation
 * information does not allow to determine whether trips are return from home or
 * move toward a new activity of same type, a "shuttle" structure is assumed (h-act-h-act-...)
 * <li> if there exists a trip with "serve passenger" or "transit transfer" purpose, and the target activity is short,
 * the consecutive trips are merged ( pick-up/drop-off are considered as part of a same leg).
 * The case where several "serve passenger" purpose are consecutive is not handled.
 * Serve passenger activities include types "Begleitweg" and "Serviceweg". The
 * difference is not clear, but it seems that "Begleitwege" are the trips corresponding
 * to joint activities rather than pick-up/drop-off (more long activity durations).
 * </ul>
 *
 * The following type of chains are removed:
 * <ul>
 * <li> the chains finishing on a non-home activity
 * <li> the chains with unknown activity types
 * <li> the plans with adress inconsistency (departure location do not corresponds
 * to the last arrival location)
 * <li> the chains with inconsistent time sequence or trip durations (ie duration is
 * different from the time elapsed between departure and arrival).
 * </ul>
 *
 * moreover, if trips from home to home exist, they are removed.
 * Attributes of the plans are set in the following way: score corresponds to the
 * weight in the MZ
 *
 * @author thibautd
 */
public class MzActivityChainsExtractor {
	public Scenario run2000(
			final String zpFile,
			final String wgFile,
			final String etFile,
			final String start,
			final String end) {
		GlobalMzInformation.setMzYear( 2000 );
		return run(zpFile, wgFile, etFile, new Interval(start, end));
	}

	public Scenario run1994(
			final String zpFile,
			final String wgFile,
			final String start,
			final String end) {
		GlobalMzInformation.setMzYear( 1994 );
		return run(zpFile, wgFile, null, new Interval(start, end));
	}

	private Scenario run(
			final String zpFile,
			final String wgFile,
			final String etFile,
			final Interval interval) {
		MzPopulation population = new MzPopulation( interval );

		try {
			// ////// add person info ///////
			BufferedReader reader = IOUtils.getBufferedReader( zpFile );

			MzPerson.notifyStructure( reader.readLine() );
			String line = reader.readLine();

			Counter count = new Counter("MzPerson # ");
			while (line != null) {
				count.incCounter();
				population.addPerson(
						new MzPerson( line ) );
				line = reader.readLine();
			}
			count.printCounter();

			// ////// add trip info //////////
			reader = IOUtils.getBufferedReader( wgFile );

			MzWeg.notifyStructure( reader.readLine() );
			line = reader.readLine();

			count = new Counter("MzWeg # ");
			while (line != null) {
				count.incCounter();
				population.addWeg(
						new MzWeg( line ) );
				line = reader.readLine();
			}
			count.printCounter();

			// ////// add etap info //////////
			if (GlobalMzInformation.getMzYear() == 2000) {
				reader = IOUtils.getBufferedReader( etFile );

				MzEtappe.notifyStructure( reader.readLine() );
				line = reader.readLine();

				count = new Counter("MzEtappe # ");
				while (line != null) {
					count.incCounter();
					population.addEtappe(
							new MzEtappe( line ) );
					line = reader.readLine();
				}
				count.printCounter();
			}
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}

		return population.getScenario();
	}

	public static class Interval {
		private final int start;
		private final int end;

		public Interval(
				final String dayStart,
				final String dayEnd) {
			this.start = Integer.parseInt( dayStart );
			this.end = Integer.parseInt( dayEnd );
		}

		public boolean contains(final int day) {
			return day >= start && day <= end;
		}
	}
}

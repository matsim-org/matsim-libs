/* *********************************************************************** *
 * project: org.matsim.*
 * PlansReaderKutter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.population;

import java.io.IOException;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.utils.WorldUtils;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

public class PopulationReaderKutter implements PopulationReader {

	private final static double ANTEIL = 10; // (1/ANTEIL) of the kutter-data will be used; 1 = 100%, 2 = 50%, 10 = 10%

	/*package*/ final Population population;
	private final PersonRowHandler rowHandler = new PersonRowHandler();
	private final TabularFileParser parser = new TabularFileParser();
	private final TabularFileParserConfig parserConfig = new TabularFileParserConfig();
	private long totalcnt = 0;
	private double totalsum = 0.0;

	public PopulationReaderKutter(final Population plans) {
		this.population = plans;
		this.parserConfig.setDelimiterRegex("\t");
	}

	public final void readFile(final String dirname) {
		for (int zw = 1; zw < 17; zw++) {
			for (int pg = 1; pg < 73; pg++) {
				readSingleFile(dirname, zw, pg);
			}
		}
		System.out.println("= Statistics: =====================");
		System.out.println("  created a total of " + this.totalcnt + " persons.");
		System.out.println("  sum of all person parsed: " + this.totalsum);
	}

	private final void readSingleFile(final String dirname, final int zw, final int pg) {
		String zw2 = Integer.toString(zw);
		String pg2 = Integer.toString(pg);
		if (zw < 10) {
			zw2 = "0" + zw2;
		}
		if (pg < 10) {
			pg2 = "0" + pg2;
		}
		String filename = dirname + "/Zw" + zw2 + "-Pg" + pg2 +".tab";

		System.out.println("    reading file " + filename + "...");

		this.rowHandler.setPersonGroup(pg);
		this.parserConfig.setFileName(filename);
		this.rowHandler.resetCounters();

		try {
			this.parser.parse(this.parserConfig, this.rowHandler);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		double sum = this.rowHandler.getPersonCountParsed();
		long cnt = this.rowHandler.getPersonCountCreated();
		System.out.println("      created cnt: " + cnt + "  sum: " + sum);

		this.totalcnt = this.totalcnt + cnt;
		this.totalsum = this.totalsum + sum;

		System.out.println("    done.");
	}

	private final class PersonRowHandler implements TabularFileHandler {

		private double currentSum = 0.0;

		private long idCnt = 0;
		private  long cnt = 0;
		private double sum = 0.0;
		private int pg_ = 0;	// person group
		private final String[] pgSex = {null, "m", "m", "f", "f", "m", "m", "f", "f", "m", "m", "f", "f", /* 0, 1-12 */
				"m", "m", "m", "m", "f", "f", "f", "f", "m", "m", "m", "m", "f", "f", "f", "f", /* 13-28 */
				"m", "m", "m", "m", "m", "m", "m", "m", "f", "f", "f", "f", "f", "f", "f", "f", /* 29-44 */
				"m", "m", "m", "m", "f", "f", "f", "f", /* 45-52 */
				"f", "f", "f", "f", "m", "m", "m", "m", "f", "f", "f", "f", /* 53-64 */
				"m", "m", "m", "m", "f", "f", "f", "f" /* 65-72 */
				};
		private final String[] pgLicense = {null, "no", "no", "no", "no", "no", "no", "no", "no", "no", "no", "no", "no", /* 0, 1-12 */
				"yes", "yes", "no", "no", "yes", "yes", "no", "no", "yes", "yes", "no", "no", "yes", "yes", "no", "no", /* 13-28 */
				"yes", "yes", "no", "no", "yes", "yes", "no", "no", "yes", "yes", "no", "no", "yes", "yes", "no", "no", /* 29-44 */
				"yes", "yes", "no", "no", "yes", "yes", "no", "no", /* 45-52 */
				"yes", "yes", "no", "no", "yes", "yes", "no", "no", "yes", "yes", "no", "no", /* 53-64 */
				"yes", "yes", "no", "no", "yes", "yes", "no", "no" /* 65-72 */
				};
		private final String[] pgCarAvail = {null, "never", "never", "never", "never", "never", "never", "never", "never", /* 0, 1-8 */
				"never", "never", "never", "never", "always", "sometimes", "never", "never", "always", "sometimes", "never", "never", /* 9-20 */
				"always", "sometimes", "never", "never", "always", "sometimes", "never", "never", /* 21-28 */
				"always", "sometimes", "never", "never", "always", "sometimes", "never", "never", /* 29-36 */
				"always", "sometimes", "never", "never", "always", "sometimes", "never", "never", /* 37-44 */
				"always", "sometimes", "never", "never", "always", "sometimes", "never", "never", /* 45-52 */
				"always", "sometimes", "never", "never", "always", "sometimes", "never", "never", /* 53-60 */
				"always", "sometimes", "never", "never", "always", "sometimes", "never", "never", /* 61-68 */
				"always", "sometimes", "never", "never" /* 69-72 */
				};
		private final String[] pgEmployed = {null, "no", "no", "no", "no", "no", "no", "no", "no", "no", "no", "no", "no", /* 0, 1-12 */
				"yes", "yes", "yes", "yes", "yes", "yes", "yes", "yes", "no", "no", "no", "no", "no", "no", "no", "no", /* 13-28 */
				"yes", "yes", "yes", "yes", "yes", "yes", "yes", "yes", "yes", "yes", "yes", "yes", "yes", "yes", "yes", "yes", /* 29-44 */
				"yes", "yes", "yes", "yes", "yes", "yes", "yes", "yes", /* 45-52 */
				"no", "no", "no", "no", "no", "no", "no", "no", "no", "no", "no", "no", /* 53-64 */
				"no", "no", "no", "no", "no", "no", "no", "no" /* 65-72 */
				};
		private final int[] pgMinAge = {0, 0, 0, 0, 0,  5,  5,  5,  5, 12, 12, 12, 12, /* 0, 1-12 */
				18, 18, 16, 16, 18, 18, 16, 16, 18, 18, 18, 18, 18, 18, 18, 18, /* 13-28 */
				20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, /* 29-44 */
				25, 25, 25, 25, 25, 25, 25, 25, /* 45-52 */
				25, 25, 25, 25, 20, 20, 20, 20, 20, 20, 20, 20, /* 53-64 */
				62, 62, 62, 62, 62, 62, 62, 62 /* 65-72 */
		};
		private final int[] pgMaxAge = {0, 5, 5, 5, 5, 12, 12, 12, 12, 18, 18, 18, 18, /* 0, 1-12 */
				20, 20, 20, 20, 20, 20, 20, 20, 28, 28, 28, 28, 28, 28, 28, 28, /* 13-28 */
				65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, /* 29-44 */
				65, 65, 65, 65, 65, 65, 65, 65, /* 45-52 */
				65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, /* 53-64 */
				99, 99, 99, 99, 99, 99, 99, 99 /* 65-72 */
				// numbers are inclusive, so, an age-range from 0-5 means, there can be people with age 0, 1, 2, 3, 4 or 5
		};
		private final String[] activities = {null, "edu", "edu", "edu", "work1", "uni", /* 0, 1-5 */
				"work1", "work2", "work3", "shop1", "shop2", "work2", "home2", /* 6-12 */
				"leisure1", "leisure2", "leisure2", "home2", "leisure1" /* 13-17 */
		};
		private final int[] durations = {0, 3*3600, 4*3600, 5*3600, 7*3600, 7*3600, /* 0, 1-5 */
				8*3600, 8*3600, 9*3600, 1800, 3600+1800, 1800, 1800, /* 6-12 */
				6*3600, 3*3600, 2*3600, 2*3600, 5*3600 /* 13-17 */
		};
		private final BasicLeg.Mode[] legModes = {BasicLeg.Mode.undefined, BasicLeg.Mode.walk, BasicLeg.Mode.bike, BasicLeg.Mode.car, BasicLeg.Mode.ride, BasicLeg.Mode.pt, BasicLeg.Mode.pt};
		private final ZoneLayer tvzLayer;
		private Person currPerson = null;
		private Plan currPlan = null;
		private Coord currHome = null;
		private int currTime = 0;

		public PersonRowHandler() {
			this.idCnt = 0;
			World world = Gbl.getWorld();
			this.tvzLayer = (ZoneLayer)world.getLayer(new IdImpl("tvz"));
		}

		public long getPersonCountCreated() {
			return this.cnt;
		}

		public double getPersonCountParsed() {
			return this.sum;
		}

		public void setPersonGroup(final int pg) {
			this.pg_ = pg;
		}

		public void resetCounters() {
			this.sum = 0.0;
			this.cnt = 0;
		}

		private Person parsePerson() {
			String id = Long.toString(this.idCnt);
			this.idCnt++;
			this.cnt++;
			int age = this.pgMinAge[this.pg_] + (int)Math.round(MatsimRandom.random.nextDouble()*(this.pgMaxAge[this.pg_]+1 - this.pgMinAge[this.pg_]));
			Person p = new Person(new IdImpl(id));
			p.setSex(this.pgSex[this.pg_]);
			p.setAge(age);
			p.setLicence(this.pgLicense[this.pg_]);
			p.setCarAvail(this.pgCarAvail[this.pg_]);
			p.setEmployed(this.pgEmployed[this.pg_]);
			return p;
		}

		private void handleActivity(final int acttype, final int cellid, final int legmode) {
			BasicLeg.Mode mode = this.legModes[legmode];

			int arrTime = this.currTime;
			int travTime = 0;

			Zone zone = (Zone)this.tvzLayer.getLocation(cellid);
			Coord coord = WorldUtils.getRandomCoordInZone(zone, this.tvzLayer);
			String activity = "";
			int duration = 0;
			boolean skipActivity = false;

			if (acttype < 18) {
				activity = this.activities[acttype];
				duration = this.durations[acttype];
			} else if (acttype == 99) {
				activity = "home";
				coord = this.currHome;
				duration = 24*3600 - this.currTime;	// the rest of the day
				if (duration < 0) {
					duration = 0;
				}
			} else {
				skipActivity = true;
			}

			if (!skipActivity) {
				Leg l = this.currPlan.createLeg(mode);
				l.setDepTime(this.currTime);
				l.setTravTime(travTime);
				l.setArrTime(arrTime);
				this.currTime = this.currTime + duration;
				Act a = this.currPlan.createAct(activity, coord);
				a.setStartTime(arrTime);
				a.setEndTime(this.currTime);
				a.setDur(duration);
			}
		}

		private void parsePlan(final String[] row) {
			this.currPlan = this.currPerson.createPlan(true);

			String homeCell = row[1];
			Zone zone = (Zone)this.tvzLayer.getLocation(homeCell);
			this.currHome = WorldUtils.getRandomCoordInZone(zone, this.tvzLayer);

			// read values of first not-home activity
			int acttype = Integer.parseInt(row[2]);
			int cellid = Integer.parseInt(row[3]);
			int legmode = Integer.parseInt(row[4]);

			// create home activity based on the next (first real) activity
			String activity = "";
			if (acttype < 18) {
				activity = this.activities[acttype];
			} else if (acttype == 99) {
				activity = "home";
			}
			long duration; // this is the duration of the first (home) activity, and equals to the end-time of the first act

			if (activity.equals("home")) {
				duration = Math.round(7.5*3600 + MatsimRandom.random.nextDouble()*2*3600); // 07:30 - 09:30
			} else if (activity.equals("edu")) {
				duration = Math.round(7.5*3600 + MatsimRandom.random.nextDouble()*2*3600); // 07:30 - 09:30
			} else if (activity.equals("uni")) {
				duration = Math.round(8.5*3600 + MatsimRandom.random.nextDouble()*2*3600); // 08:30 - 10:30
			} else if (activity.equals("work1")) {
				duration = Math.round(7.5*3600 + MatsimRandom.random.nextDouble()*1*3600); // 07:30 - 08:30
			} else if (activity.equals("work2")) {
				duration = Math.round(7.5*3600 + MatsimRandom.random.nextDouble()*2*3600); // 07:30 - 09:30
			} else if (activity.equals("work3")) {
				duration = Math.round(7.5*3600 + MatsimRandom.random.nextDouble()*2.5*3600); // 07:30 - 10:00
			} else if (activity.equals("shop1")) {
				duration = Math.round(8.5*3600 + MatsimRandom.random.nextDouble()*3*3600); // 08:30 - 11:30
			} else if (activity.equals("shop2")) {
				duration = Math.round(9*3600 + MatsimRandom.random.nextDouble()*2*3600); // 09:00 - 11:00
			} else if (activity.equals("home2")) {
				duration = Math.round(9*3600 + MatsimRandom.random.nextDouble()*2*3600); // 09:00 - 11:00
			} else if (activity.equals("leisure1")) {
				duration = Math.round(9*3600 + MatsimRandom.random.nextDouble()*3*3600); // 09:00 - 12:00
			} else if (activity.equals("leisure2")) {
				duration = Math.round(8*3600 + MatsimRandom.random.nextDouble()*2*3600); // 08:00 - 10:00
			} else {
				// well, this case should never happen, but just to be sure
				duration = Math.round(8*3600 + MatsimRandom.random.nextDouble()*2*3600); // 08:00 - 10:00
			}


			Act a = this.currPlan.createAct("home", this.currHome);
			a.setEndTime(duration);
			this.currTime = (int)duration;

			handleActivity(acttype, cellid, legmode);

			if (acttype == 99) {
				return;	// the person went home
			}

			acttype = Integer.parseInt(row[5]);
			cellid = Integer.parseInt(row[6]);
			legmode = Integer.parseInt(row[7]);

			handleActivity(acttype, cellid, legmode);

			if (acttype == 99) {
				return;	// the person went home
			}

			acttype = Integer.parseInt(row[8]);
			cellid = Integer.parseInt(row[9]);
			legmode = Integer.parseInt(row[10]);

			handleActivity(acttype, cellid, legmode);

			if (acttype == 99) {
				return;	// the person went home
			}

			acttype = Integer.parseInt(row[11]);
			cellid = Integer.parseInt(row[12]);
			legmode = Integer.parseInt(row[13]);

			handleActivity(acttype, cellid, legmode);

			if (acttype == 99) {
				return;	// the person went home
			}

			// if the person went not yet home, send it home now
			handleActivity(99, cellid, legmode); // cellid will be ignored, because the person goes home (there is only one home!)
		}

		public void startRow(final String[] row) {
			if (row == null) {
				throw new RuntimeException("row is null");
			}
			double freq = Double.parseDouble(row[0]);
			this.sum = this.sum + freq;

			this.currentSum = this.currentSum + freq;
			while (this.currentSum >= ANTEIL) {
				this.currPerson = parsePerson();
				parsePlan(row);
				PopulationReaderKutter.this.population.addPerson(this.currPerson);
				this.currPerson = null;
				this.currPlan = null;
				this.currHome = null;
				this.currTime = 0;

				this.currentSum -= ANTEIL;
			}
		}
	}
}

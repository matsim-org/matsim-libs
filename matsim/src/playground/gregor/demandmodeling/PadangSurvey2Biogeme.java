/* *********************************************************************** *
 * project: org.matsim.*
 * PadangSurvey2Biogeme.java
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

package playground.gregor.demandmodeling;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.StringUtils;

public class PadangSurvey2Biogeme {

	
	private final String surveyFilename;
	private final String zonesFilename;

	private int sumOpportunities;
	ArrayList<Zone> zones;
	
	
	public PadangSurvey2Biogeme(final String surveyFilename,final String zonesFilename) {
		this.surveyFilename = surveyFilename;
		this.zonesFilename = zonesFilename;
		init();
	}
	
	
	
	private void init() {
		this.zones = new ArrayList<Zone>(20);
		this.sumOpportunities = 0;

		// read zones
		try {
			final BufferedReader zonesReader = IOUtils.getBufferedReader(this.zonesFilename);
			String header = zonesReader.readLine();
			String line = zonesReader.readLine();
			while (line != null) {
				String[] parts = StringUtils.explode(line, ';');
				int numOpportunities = Integer.parseInt(parts[3]);
				Zone zone = new Zone(parts[0], new CoordImpl(parts[1], parts[2]), numOpportunities);
				this.zones.add(zone);
				this.sumOpportunities += numOpportunities;
				// --------
				line = zonesReader.readLine();
			}
			zonesReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
	}


	
	public void run(final String actType, final String biogemeFilename) {

		// process survey, line by line
		try {
			final BufferedReader surveyReader = IOUtils.getBufferedReader(this.surveyFilename);
			final BufferedWriter biogemeWriter = IOUtils.getBufferedWriter(biogemeFilename);
			String header = surveyReader.readLine();
			biogemeWriter.write("Id\tChoice\tdChosen\td1\td2\td3\td4\td5\td6\td7\td8\td9\n");
			String line = surveyReader.readLine();

			Counter counter = new Counter("entry ");
			while (line != null) {
				counter.incCounter();

				String[] parts = StringUtils.explode(line, ',');
				String id = parts[0];
				String type = parts[3];
				
				if (!type.contains(actType)) {
					line = surveyReader.readLine();
					continue;
				}
				
				
				Coord homeCoord = new CoordImpl(parts[1], parts[2]);
				Coord primActCoord = new CoordImpl(parts[5], parts[6]);
				double distance = CoordUtils.calcDistance(homeCoord, primActCoord);
				if (distance > 30000) {
					throw new RuntimeException("this should not happen!!");
				}
				Zone[] alternatives = new Zone[9];

				int numShorterTrips = 0;
				int numEqualTrips = 0;
				int numLongerTrips = 0;
				int numMissed = 0;
				int missed = 0; // security counter to prevent endless loops, in case the actual distance is e.g. shorter than distance to nearest zone


				while ( (numShorterTrips + numEqualTrips + numLongerTrips + numMissed) < 9) {

					// draw random zone based on numOpportunities
					int r = MatsimRandom.getRandom().nextInt(this.sumOpportunities);
					int tmpSum = 0;
					Zone tmpZone = null;
					for (Zone zone : this.zones) {
						tmpSum += zone.numOpportunities;
						if (r <= tmpSum) {
							tmpZone = zone;
							break;
						}
					}
					double tmpDistance = CoordUtils.calcDistance(homeCoord, tmpZone.coord);
					if ((tmpDistance < (0.7 * distance)) && (numShorterTrips < 3)) {
						alternatives[numShorterTrips + numEqualTrips + numLongerTrips + numMissed] = tmpZone;
						numShorterTrips++;
						missed = 0;
					} else if ((tmpDistance > (1.3 * distance)) && (numLongerTrips < 3)) {
						alternatives[numShorterTrips + numEqualTrips + numLongerTrips + numMissed] = tmpZone;
						numLongerTrips++;
						missed = 0;
					} else if (numEqualTrips < 3) {
						alternatives[numShorterTrips + numEqualTrips + numLongerTrips + numMissed] = tmpZone;
						numEqualTrips++;
						missed = 0;
					} else if (missed >= 100) {
						System.out.println("WARN: couldn't find appropriate alternative for survey: " + id
								+ ". #shorter=" + numShorterTrips + " #equal=" + numEqualTrips
								+ " #longer=" + numLongerTrips + " d=" + distance);
						alternatives[numShorterTrips + numEqualTrips + numLongerTrips + numMissed] = tmpZone;
						numMissed++;
					} else {
						missed++;
					}
				}

				biogemeWriter.write(id + "\t1\t" + distance);
				for (Zone alternative : alternatives) {
					biogemeWriter.write("\t" + CoordUtils.calcDistance(homeCoord, alternative.coord));
				}
				biogemeWriter.write('\n');

				// -------
				line = surveyReader.readLine();
			}
			counter.printCounter();
			surveyReader.close();
			biogemeWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	private static class Zone {
		public final String zoneId;
		public final Coord coord;
		public final int numOpportunities;

		public Zone(final String zoneId, final Coord coord, final int numOpportunities) {
			this.zoneId = zoneId;
			this.coord = coord;
			this.numOpportunities = numOpportunities;
		}
	}

	public static void main(final String[] args) {
		final String surveyFilename = "../inputs/padang/referencing/output/working-day_primAct.csv";
		final String zonesFilename = "/home/laemmel/arbeit/svn/vsp-svn/projects/LastMile/demand_generation/FINAL/biogeme/PadangSurvey2Biogeme_input/Zones.csv";
		
		PadangSurvey2Biogeme ps2b = new PadangSurvey2Biogeme(surveyFilename,zonesFilename);
		
		String biogemeFilename = "../inputs/padang/referencing/output/working-day_HOME.dat";
		ps2b.run("HOME", biogemeFilename);
		
		biogemeFilename = "../inputs/padang/referencing/output/working-day_HOUSEWORK.dat";
		ps2b.run("HOUSE", biogemeFilename);
		
		biogemeFilename = "../inputs/padang/referencing/output/working-day_WORK.dat";
		ps2b.run("WORK", biogemeFilename);

		biogemeFilename = "../inputs/padang/referencing/output/working-day_SOC.dat";
		ps2b.run("SOC", biogemeFilename);
		
		biogemeFilename = "../inputs/padang/referencing/output/working-day_EDU.dat";
		ps2b.run("EDU", biogemeFilename);
		

	}

}

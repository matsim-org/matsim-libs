/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLinkStats.java
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

package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Calculates the average link volumes and travel times over any number of iterations.
 *
 * @author mrieser
 */
public class CalcLinkStats {

	private final static Logger log = LogManager.getLogger(CalcLinkStats.class);

	private static class LinkData {
		public final double[][] volumes;
		public final double[][] ttimes;

		public LinkData(final double[][] linksVolumes, final double[][] linksTTimes) {
			this.volumes = linksVolumes.clone();
			this.ttimes = linksTTimes.clone();
		}
	}

	private double volScaleFactor = 1.0;

	private int count = 0;
	private final IdMap<Link, LinkData> linkData;
	private final int nofHours;
	private final Network network;

	private static final int MIN = 0;
	private static final int MAX = 1;
	private static final int SUM = 2;
	private static final int NOF_STATS = 3;

	@Inject
	public CalcLinkStats(final Network network) {
		this.network = network;
		this.linkData = new IdMap<>(Link.class);
		this.nofHours = 24;
		reset();
	}

	/**
	 * @param network
	 * @param vol_scale_factor scaling factor when reading in values from a file
	 *
	 * @see #readFile(String)
	 */
	public CalcLinkStats(final Network network, double vol_scale_factor) {
		this(network);
		this.volScaleFactor = vol_scale_factor;
	}

	public void addData(final VolumesAnalyzer analyzer, final TravelTime ttimes) {
		this.count++;
		// TODO verify ttimes has hourly timeBin-Settings

		// go through all links
		for (Id<Link> linkId : this.linkData.keySet()) {

			// retrieve link from link ID
			Link link = this.network.getLinks().get(linkId);

			// get the volumes for the link ID from the analyzier
			double[] volumes = analyzer.getVolumesPerHourForLink(linkId);

			// get the destination container for the data from link data (could have gotten this through iterator right away)
			LinkData data = this.linkData.get(linkId);

			// prepare the sum variables (for volumes);
			long sumVolumes = 0; // daily (0-24) sum

			// go through all hours:
			for (int hour = 0; hour < this.nofHours; hour++) {

				// get travel time for hour
				double ttime = ttimes.getLinkTravelTime(link, hour*3600, null, null);

				// add for daily sum:
				sumVolumes += volumes[hour];

				// the following has something to do with the fact that we are doing this for multiple iterations.  So there are variations.
				// this collects min and max.  There is, however, no good control over how many iterations this is collected.
				if (this.count == 1) {
					data.volumes[MIN][hour] = volumes[hour];
					data.volumes[MAX][hour] = volumes[hour];
					data.ttimes[MIN][hour] = ttime;
					data.ttimes[MAX][hour] = ttime;
				} else {
					if (volumes[hour] < data.volumes[MIN][hour]) data.volumes[MIN][hour] = volumes[hour];
					if (volumes[hour] > data.volumes[MAX][hour]) data.volumes[MAX][hour] = volumes[hour];
					if (ttime < data.ttimes[MIN][hour]) data.ttimes[MIN][hour] = ttime;
					if (ttime > data.ttimes[MAX][hour]) data.ttimes[MAX][hour] = ttime;
				}

				// this is the regular summing up for each hour
				data.volumes[SUM][hour] += volumes[hour];
				data.ttimes[SUM][hour] += volumes[hour] * ttime;
			}
			// dataVolumes[.][nofHours] are daily (0-24) values
			if (this.count == 1) {
				data.volumes[MIN][this.nofHours] = sumVolumes;
				data.volumes[SUM][this.nofHours] = sumVolumes;
				data.volumes[MAX][this.nofHours] = sumVolumes;
			} else {
				if (sumVolumes < data.volumes[MIN][this.nofHours]) data.volumes[MIN][this.nofHours] = sumVolumes;
				data.volumes[SUM][this.nofHours] += sumVolumes;
				if (sumVolumes > data.volumes[MAX][this.nofHours]) data.volumes[MAX][this.nofHours] = sumVolumes;
			}
		}
	}

	public void reset() {
		this.linkData.clear();
		this.count = 0;
		log.info( " resetting `count' to zero.  This info is here since we want to check when this" +
				" is happening during normal simulation runs.  kai, jan'11") ;

		// initialize our data-table
		for (Link link : this.network.getLinks().values()) {
			LinkData data = new LinkData(new double[NOF_STATS][this.nofHours + 1], new double[NOF_STATS][this.nofHours]);
			this.linkData.put(link.getId(), data);
		}

	}

	public void writeFile(final String filename) {
		try (BufferedWriter out = IOUtils.getBufferedWriter(filename)) {

			// write header
			out.write("LINK\tORIG_ID\tFROM\tTO\tLENGTH\tFREESPEED\tCAPACITY");
			for (int i = 0; i < this.nofHours; i++) {
				out.write("\tHRS" + i + "-" + (i+1) + "min");
				out.write("\tHRS" + i + "-" + (i+1) + "avg");
				out.write("\tHRS" + i + "-" + (i+1) + "max");
			}
			out.write("\tHRS0-" + this.nofHours + "min");
			out.write("\tHRS0-" + this.nofHours + "avg");
			out.write("\tHRS0-" + this.nofHours + "max");
			for (int i = 0; i < this.nofHours; i++) {
				out.write("\tTRAVELTIME" + i + "-" + (i+1) + "min");
				out.write("\tTRAVELTIME" + i + "-" + (i+1) + "avg");
				out.write("\tTRAVELTIME" + i + "-" + (i+1) + "max");
			}
			out.write("\n");

			// write data
			for (Map.Entry<Id<Link>, LinkData> entry : this.linkData.entrySet()) {
				Id<Link> linkId = entry.getKey();
				LinkData data = entry.getValue();
				Link link = this.network.getLinks().get(linkId);

				out.write(linkId.toString());
				out.write("\t"); // origId, no longer supported
				out.write("\t" + link.getFromNode().getId().toString());
				out.write("\t" + link.getToNode().getId().toString());
				out.write("\t" + Double.toString(link.getLength()));
				out.write("\t" + Double.toString(link.getFreespeed()));
				out.write("\t" + Double.toString(link.getCapacity()));

				// HRS0-1, HRS1-2, ... HRS23-24
//				int[] sum = {0, 0, 0};
				for (int i = 0; i < this.nofHours; i++) {
					out.write("\t" + Double.toString(data.volumes[MIN][i]));
//					sum[MIN] = sum[MIN] + data.volumes[MIN][i];
					out.write("\t" + Double.toString((data.volumes[SUM][i]) / this.count));
//					sum[SUM] = sum[SUM] + data.volumes[SUM][i];
					out.write("\t" + Double.toString(data.volumes[MAX][i]));
//					sum[MAX] = sum[MAX] + data.volumes[MAX][i];
				}

				// HRS0-nofHours
				out.write("\t" + Double.toString(data.volumes[MIN][this.nofHours]));
				out.write("\t" + Double.toString((data.volumes[SUM][this.nofHours]) / this.count));
				out.write("\t" + Double.toString(data.volumes[MAX][this.nofHours]));

				// TRAVELTIME0-1, TRAVELTIME1-2, ... TRAVELTIME23-24
				for (int i = 0; i < this.nofHours; i++) {
					String ttimesMin = Double.toString(data.ttimes[MIN][i]);
					out.write("\t" + ttimesMin);
					if (data.volumes[SUM][i] == 0) {
						// nobody traveled along the link in this hour, so we cannot calculate an average
						// use the value available or the minimum instead (min and max should be the same, =freespeed)
						double ttsum = data.ttimes[SUM][i];
						if (ttsum != 0.0) {
							out.write("\t" + Double.toString(ttsum));
						} else {
							out.write("\t" + ttimesMin);
						}
					} else {
						double ttsum = data.ttimes[SUM][i];
						if (ttsum == 0) {
							out.write("\t" + ttimesMin);
						} else {
							out.write("\t" + Double.toString(ttsum / data.volumes[SUM][i]));
						}
					}
					out.write("\t" + Double.toString(data.ttimes[MAX][i]));
				}
				out.write("\n");
			}

		} catch (IOException e) {
			log.error("could not write linkstats", e);
		}
	}

	public void readFile(final String filename) {
		// start with a clean, empty data structure
		reset();

		try (BufferedReader reader = IOUtils.getBufferedReader(filename)) {

			// read header
			String header = reader.readLine();
			if (header == null) {
				// it seems there is no data in this file...
				return;
			}
			// ignore the header, but if there was a header, set the count to 1
			this.count = 1;

			// read lines
			String line = reader.readLine();
			while (line != null) {
				String[] parts = StringUtils.explode(line, '\t');
				if (parts.length == 154) {
					Id<Link> linkId = Id.create(parts[0], Link.class);
					LinkData data = this.linkData.get(linkId);
					if (data == null) {
						System.err.println("CalcLinkStats.readFile(); unknown link: " + linkId.toString());
					} else {
						int baseTTimes;
						for (int i = 0; i < this.nofHours; i++) {
							data.volumes[MIN][i] = Double.parseDouble(parts[7 + i*3]);
							data.volumes[MIN][i] *= this.volScaleFactor;
							data.volumes[SUM][i] = Double.parseDouble(parts[8 + i*3]);
							data.volumes[SUM][i] *= this.volScaleFactor;
							data.volumes[MAX][i] = Double.parseDouble(parts[9 + i*3]);
							data.volumes[MAX][i] *= this.volScaleFactor;
							baseTTimes = 7 + (this.nofHours+1)*3;
							data.ttimes[MIN][i] = Double.parseDouble(parts[baseTTimes + i*3]);
							if (data.volumes[SUM][i] == 0) {
								data.ttimes[SUM][i] = Double.parseDouble(parts[baseTTimes + i*3 + 1]);
							} else {
								data.ttimes[SUM][i] = Double.parseDouble(parts[baseTTimes + i*3 + 1]) * data.volumes[SUM][i];
							}
							data.ttimes[MAX][i] = Double.parseDouble(parts[baseTTimes + i*3 + 2]);
						}
						data.volumes[MIN][this.nofHours] = Double.parseDouble(parts[7 + this.nofHours*3]);
						data.volumes[MIN][this.nofHours] *= this.volScaleFactor;
						data.volumes[SUM][this.nofHours] = Double.parseDouble(parts[8 + this.nofHours*3]);
						data.volumes[SUM][this.nofHours] *= this.volScaleFactor;
						data.volumes[MAX][this.nofHours] = Double.parseDouble(parts[9 + this.nofHours*3]);
						data.volumes[MAX][this.nofHours] *= this.volScaleFactor;
					}
				}
				else if (parts.length == 153) {
					String linkId = parts[0];
					LinkData data = this.linkData.get(Id.create(linkId, Link.class));
					if (data == null) {
						System.err.println("CalcLinkStats.readFile(); unknown link: " + linkId);
					} else {
						int baseTTimes;
						for (int i = 0; i < this.nofHours; i++) {
							data.volumes[MIN][i] = Double.parseDouble(parts[6 + i*3]);
							data.volumes[MIN][i] *= this.volScaleFactor;
							data.volumes[SUM][i] = Integer.parseInt(parts[7 + i*3]);
							data.volumes[SUM][i] *= this.volScaleFactor;
							data.volumes[MAX][i] = Double.parseDouble(parts[8 + i*3]);
							data.volumes[MAX][i] *= this.volScaleFactor;
							baseTTimes = 6 + (this.nofHours+1)*3;
							data.ttimes[MIN][i] = Double.parseDouble(parts[baseTTimes + i*3]);
							if (data.volumes[SUM][i] == 0) {
								data.ttimes[SUM][i] = Double.parseDouble(parts[baseTTimes + i*3 + 1]);
							} else {
								data.ttimes[SUM][i] = Double.parseDouble(parts[baseTTimes + i*3 + 1]) * data.volumes[SUM][i];
							}
							data.ttimes[MAX][i] = Double.parseDouble(parts[baseTTimes + i*3 + 2]);
						}
						data.volumes[MIN][this.nofHours] = Double.parseDouble(parts[6 + this.nofHours*3]);
						data.volumes[MIN][this.nofHours] *= this.volScaleFactor;
						data.volumes[SUM][this.nofHours] = Double.parseDouble(parts[7 + this.nofHours*3]);
						data.volumes[SUM][this.nofHours] *= this.volScaleFactor;
						data.volumes[MAX][this.nofHours] = Double.parseDouble(parts[8 + this.nofHours*3]);
						data.volumes[MAX][this.nofHours] *= this.volScaleFactor;
					}
				}
				else {
					System.err.println("CalcLinkStats.readFile(); line cannot be parsed: " + line + " number of colums is: " + parts.length);
					break;
				}
				line = reader.readLine();
			}

		} catch (IOException e) {
			log.error("could not read linkstats.", e);
		}
	}

	/**
	 * @param linkId
	 * @return if no data is available, an array with length 0 is returned.
	 */
	public double[] getAvgLinkVolumes(final Id<Link> linkId) {
		LinkData data = this.linkData.get(linkId);
		if (data == null) {
			return new double[0];
		}
		if (this.count == 0) {
			return new double[0];
		}
		double[] volumes = new double[this.nofHours];
		for (int i = 0; i < this.nofHours; i++) {
			volumes[i] = (data.volumes[SUM][i]) / (this.count);
		}
		return volumes;
	}

	/**
	 * @param linkId
	 * @return if no data is available, an array with length 0 is returned.
	 *
	 * The method reflects the (wrong) logic of what is done when writing the output and should eventually be deleted or modified.
	 *
	 */
	@Deprecated
	protected double[] getAvgTravelTimes(final Id<Link> linkId) {
		LinkData data = this.linkData.get(linkId);
		if (data == null) {
			return new double[0];
		}
		if (this.count == 0) {
			return new double[0];
		}
		double[] ttimesMin = new double[this.nofHours];
		double[] ttimesSum = new double[this.nofHours];
		double[] volumes = new double[this.nofHours];

		double[] avgTTimes = new double[this.nofHours];

		for (int i = 0; i < this.nofHours; i++) {
			volumes[i] = (data.volumes[SUM][i]) / (this.count);
			ttimesMin[i] = (data.ttimes[MIN][i]) / (this.count);
			ttimesSum[i] = (data.ttimes[SUM][i]) / (this.count);

			if (volumes[i] == 0.) {
				avgTTimes[i] = ttimesMin[i];
			} else {
				avgTTimes[i] = ttimesSum[i] / volumes[i];
			}
		}
		return avgTTimes;
	}

}

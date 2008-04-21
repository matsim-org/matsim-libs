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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.StringUtils;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Time;

/**
 * Calculates the average link volumes and travel times over any number of iterations.
 *
 * @author mrieser
 *
 */
public class CalcLinkStats {

	private static class LinkData {
		public final int[][] volumes;
		public final double[][] ttimes;

		public LinkData(final int[][] linksVolumes, final double[][] linksTTimes) {
			this.volumes = linksVolumes;
			this.ttimes = linksTTimes;
		}
	}

	private int count = 0;
	private final Map<String, LinkData> linkData;
	private final int nofHours;
	private final NetworkLayer network;

	private static final int MIN = 0;
	private static final int MAX = 1;
	private static final int SUM = 2;
	private static final int NOF_STATS = 3;

	public CalcLinkStats(final NetworkLayer network) {
		this.network = network;
		this.linkData = new TreeMap<String, LinkData>();//((int)(1.1*this.network.getLinks().size()), 0.95f);
		this.nofHours = 24;
		reset();
	}

	public void addData(final VolumesAnalyzer analyzer, final TravelTimeI ttimes) {
		this.count++;
		// TODO verify analyzer and ttimes have hourly timeBin-Settings
		for (String linkId : this.linkData.keySet()) {
			Link link = this.network.getLinks().get(new IdImpl(linkId));
			int[] volumes = analyzer.getVolumesForLink(linkId);
			LinkData data = this.linkData.get(linkId);
			int sum = 0; // daily (0-24) sum
			if (volumes == null) {
				// nobody traveled along this link
				// use default data: volumes[i] = 0
				// the following for-loop is the same as in the else-clause below, but optimized for the case volumes[i] = 0
				for (int i = 0; i < this.nofHours; i++) {
					double ttime = ttimes.getLinkTravelTime(link, i*3600);
					if (this.count == 1) {
						data.ttimes[MIN][i] = ttime;
						data.ttimes[MAX][i] = ttime;
					} else {
						data.volumes[MIN][i] = 0;
						if (ttime < data.ttimes[MIN][i]) data.ttimes[MIN][i] = ttime;
						if (ttime > data.ttimes[MAX][i]) data.ttimes[MAX][i] = ttime;
					}
				}
			} else {
				for (int i = 0; i < this.nofHours; i++) {
					double ttime = ttimes.getLinkTravelTime(link, i*3600);
					sum += volumes[i];
					if (this.count == 1) {
						data.volumes[MIN][i] = volumes[i];
						data.volumes[MAX][i] = volumes[i];
						data.ttimes[MIN][i] = ttime;
						data.ttimes[MAX][i] = ttime;
					} else {
						if (volumes[i] < data.volumes[MIN][i]) data.volumes[MIN][i] = volumes[i];
						if (volumes[i] > data.volumes[MAX][i]) data.volumes[MAX][i] = volumes[i];
						if (ttime < data.ttimes[MIN][i]) data.ttimes[MIN][i] = ttime;
						if (ttime > data.ttimes[MAX][i]) data.ttimes[MAX][i] = ttime;
					}
					data.volumes[SUM][i] += volumes[i];
					data.ttimes[SUM][i] += volumes[i] * ttime;
				}
			}
			// dataVolumes[.][nofHours] are daily (0-24) values
			if (this.count == 1) {
				data.volumes[MIN][this.nofHours] = sum;
				data.volumes[SUM][this.nofHours] = sum;
				data.volumes[MAX][this.nofHours] = sum;
			} else {
				if (sum < data.volumes[MIN][this.nofHours]) data.volumes[MIN][this.nofHours] = sum;
				data.volumes[SUM][this.nofHours] += sum;
				if (sum > data.volumes[MAX][this.nofHours]) data.volumes[MAX][this.nofHours] = sum;
			}
		}
	}

	public void reset() {
		this.linkData.clear();
		this.count = 0;

		// initialize our data-table
		for (BasicLink link : this.network.getLinks().values()) {
			String linkId = link.getId().toString();
			LinkData data = new LinkData(new int[NOF_STATS][this.nofHours + 1], new double[NOF_STATS][this.nofHours]);
			this.linkData.put(linkId, data);
		}

	}

	public void writeFile(final String filename) {
		BufferedWriter out = null;
		try {
			out = IOUtils.getBufferedWriter(filename, true);

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
			for (String linkId : this.linkData.keySet()) {
				LinkData data = this.linkData.get(linkId);
				Link link = this.network.getLinks().get(new IdImpl(linkId));

				out.write(linkId);
				if (link.getOrigId() == null) {
					out.write("\t");
				} else {
					out.write("\t" + link.getOrigId());
				}
				out.write("\t" + link.getFromNode().getId().toString());
				out.write("\t" + link.getToNode().getId().toString());
				out.write("\t" + Double.toString(link.getLength()));
				out.write("\t" + Double.toString(link.getFreespeed(Time.UNDEFINED_TIME)));
				out.write("\t" + Double.toString(link.getCapacity()));

				// HRS0-1, HRS1-2, ... HRS23-24
//				int[] sum = {0, 0, 0};
				for (int i = 0; i < this.nofHours; i++) {
					out.write("\t" + Integer.toString(data.volumes[MIN][i]));
//					sum[MIN] = sum[MIN] + data.volumes[MIN][i];
					out.write("\t" + Integer.toString(data.volumes[SUM][i] / this.count));
//					sum[SUM] = sum[SUM] + data.volumes[SUM][i];
					out.write("\t" + Integer.toString(data.volumes[MAX][i]));
//					sum[MAX] = sum[MAX] + data.volumes[MAX][i];
				}

				// HRS0-nofHours
				out.write("\t" + Integer.toString(data.volumes[MIN][this.nofHours]));
				out.write("\t" + Integer.toString(data.volumes[SUM][this.nofHours] / this.count));
				out.write("\t" + Integer.toString(data.volumes[MAX][this.nofHours]));

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
			e.printStackTrace();
		}
		finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException ignored) {}
			}
		}
	}

	public void readFile(final String filename) {
		// start with a clean, empty data structure
		reset();

		BufferedReader reader = null;
		try {
			reader = IOUtils.getBufferedReader(filename);

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
					String linkId = parts[0];
					LinkData data = this.linkData.get(linkId);
					if (data == null) {
						System.err.println("CalcLinkStats.readFile(); unknown link: " + linkId);
					} else {
						int baseTTimes;
						for (int i = 0; i < this.nofHours; i++) {
							data.volumes[MIN][i] = Integer.parseInt(parts[7 + i*3]);
							data.volumes[SUM][i] = Integer.parseInt(parts[8 + i*3]);
							data.volumes[MAX][i] = Integer.parseInt(parts[9 + i*3]);
							baseTTimes = 7 + (this.nofHours+1)*3;
							data.ttimes[MIN][i] = Double.parseDouble(parts[baseTTimes + i*3]);
							if (data.volumes[SUM][i] == 0) {
								data.ttimes[SUM][i] = Double.parseDouble(parts[baseTTimes + i*3 + 1]);
							} else {
								data.ttimes[SUM][i] = Double.parseDouble(parts[baseTTimes + i*3 + 1]) * data.volumes[SUM][i];
							}
							data.ttimes[MAX][i] = Double.parseDouble(parts[baseTTimes + i*3 + 2]);
						}
						data.volumes[MIN][this.nofHours] = Integer.parseInt(parts[7 + this.nofHours*3]);
						data.volumes[SUM][this.nofHours] = Integer.parseInt(parts[8 + this.nofHours*3]);
						data.volumes[MAX][this.nofHours] = Integer.parseInt(parts[9 + this.nofHours*3]);
					}
				}
				else if (parts.length == 153) {
					String linkId = parts[0];
					LinkData data = this.linkData.get(linkId);
					if (data == null) {
						System.err.println("CalcLinkStats.readFile(); unknown link: " + linkId);
					} else {
						int baseTTimes;
						for (int i = 0; i < this.nofHours; i++) {
							data.volumes[MIN][i] = Integer.parseInt(parts[6 + i*3]);
							data.volumes[SUM][i] = Integer.parseInt(parts[7 + i*3]);
							data.volumes[MAX][i] = Integer.parseInt(parts[8 + i*3]);
							baseTTimes = 6 + (this.nofHours+1)*3;
							data.ttimes[MIN][i] = Double.parseDouble(parts[baseTTimes + i*3]);
							if (data.volumes[SUM][i] == 0) {
								data.ttimes[SUM][i] = Double.parseDouble(parts[baseTTimes + i*3 + 1]);
							} else {
								data.ttimes[SUM][i] = Double.parseDouble(parts[baseTTimes + i*3 + 1]) * data.volumes[SUM][i];
							}
							data.ttimes[MAX][i] = Double.parseDouble(parts[baseTTimes + i*3 + 2]);
						}
						data.volumes[MIN][this.nofHours] = Integer.parseInt(parts[6 + this.nofHours*3]);
						data.volumes[SUM][this.nofHours] = Integer.parseInt(parts[7 + this.nofHours*3]);
						data.volumes[MAX][this.nofHours] = Integer.parseInt(parts[8 + this.nofHours*3]);
					}
				}
				else {
					System.err.println("CalcLinkStats.readFile(); line cannot be parsed: " + line + " number of colums is: " + parts.length);
					break;
				}
				line = reader.readLine();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try { reader.close(); } catch (IOException ignored) {}
			}
		}
	}

	public double[] getAvgLinkVolumes(final String linkId) {
		LinkData data = this.linkData.get(linkId);
		if (data == null) {
			return null;
		}
		if (this.count == 0) {
			return null;
		}
		double[] volumes = new double[this.nofHours];
		for (int i = 0; i < this.nofHours; i++) {
			volumes[i] = ((double)data.volumes[SUM][i]) / ((double)this.count);
		}
		return volumes;
	}

}

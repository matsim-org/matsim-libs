/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.vissim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.boescpa.converters.vissim.tools.AbstractRouteConverter.Trip;
import playground.boescpa.converters.vissim.tools.InpNetworkMapper;
import playground.boescpa.converters.vissim.tools.InpRouteConverter;
import playground.boescpa.converters.vissim.tools.MsNetworkMapper;
import playground.boescpa.converters.vissim.tools.MsRouteConverter;

/**
 * Extends and implements the abstract class ConvEvents for Inp-Files.
 *
 * @author boescpa
 */
public class ConvEvents2Inp extends ConvEvents {

	public ConvEvents2Inp() {
		super();
	}

	public ConvEvents2Inp(BaseGridCreator baseGridCreator, NetworkMapper matsimNetworkMapper, NetworkMapper anmNetworkMapper, RouteConverter matsimRouteConverter, RouteConverter anmRouteConverter, TripMatcher tripMatcher) {
		super(baseGridCreator, matsimNetworkMapper, anmNetworkMapper, matsimRouteConverter, anmRouteConverter, tripMatcher);
	}

	public static void main(String[] args) {
		// path2VissimZoneShp = args[0];
		// path2MATSimNetwork = args[1];
		// path2VissimInpFile = args[2];
		// path2EventsFile = args[3];
		// path2VissimInpFile = args[4];
		// path2NewVissimInpFile = args[5];

		ConvEvents convEvents = createDefaultConvEvents();
		convEvents.convert(args);
	}

	public static ConvEvents2Inp createDefaultConvEvents() {
		return new ConvEvents2Inp(new playground.boescpa.converters.vissim.tools.BaseGridCreator(), new MsNetworkMapper(), new InpNetworkMapper(),
				new MsRouteConverter(), new InpRouteConverter(), new playground.boescpa.converters.vissim.tools.TripMatcher());
	}

	@Override
	public void writeRoutes(HashMap<Id<Trip>, Integer> demandPerVissimTrip, String path2InpFile, String path2NewInpFile) {
		try {
			final BufferedReader in = IOUtils.getBufferedReader(path2InpFile);
			final BufferedWriter out = IOUtils.getBufferedWriter(path2NewInpFile);

			final Pattern rdPattern = Pattern.compile("ROUTING_DECISION .*");
			final Pattern ipPattern = Pattern.compile("INPUT .*");
			final Pattern linkPattern = Pattern.compile(" +LINK .*");
			final Pattern rPattern = Pattern.compile(" +ROUTE .*");
			final Pattern fracPattern = Pattern.compile(" +FRACTION .*");
			final Pattern nPattern = Pattern.compile(" *");
			final Pattern numPattern = Pattern.compile("\\d+\\.\\d+");
			final Pattern totValPattern = Pattern.compile("EXACT \\d+\\.\\d+ ");
			final String delimiter = " +";

			String currentRoutingDecision = "";
			String currentRoute = "";
			long linkRoutingDecision = 0l;
			int totalPerRoutingDecision = 0;
			boolean inRoutingDecision = false;
			boolean inInput = false;

			Map<Long, Integer> totalPerLinkRoutingDecision = new HashMap<>();

			String line = in.readLine();
			while (line != null) {
				// ROUTING_DECISION:
				if (rdPattern.matcher(line).matches()) {
					String[] lineVals = line.split(delimiter);
					inRoutingDecision = true;
					currentRoutingDecision = lineVals[1];
					totalPerRoutingDecision = calcTotalPerRoutingDecision(currentRoutingDecision, demandPerVissimTrip);
				}
				if (inRoutingDecision) {
					// LINK of ROUTING_DECISION:
					if (linkPattern.matcher(line).matches()) {
						String[] lineVals = line.split(delimiter);
						linkRoutingDecision = Long.parseLong(lineVals[2]);
					}
					// ROUTE:
					if (rPattern.matcher(line).matches()) {
						String[] lineVals = line.split(delimiter);
						currentRoute = lineVals[2];
					}
					// FRACTION of route:
					if (fracPattern.matcher(line).matches()) {
						double demand = demandPerVissimTrip.get(Id.create(currentRoutingDecision + "-" + currentRoute, Trip.class));
						String fraction = "0.0";
						if (totalPerRoutingDecision > 0) {
							fraction = String.valueOf(demand/totalPerRoutingDecision);
						}
						if (fraction.length() < 5) {
							fraction = fraction + "00000000";
						}
						line = replaceSubstring(line, fraction.substring(0, 5), numPattern);
					}
					// End of Routing Decision (EMPTY LINE):
					if (nPattern.matcher(line).matches()) {
						// store totals for input-modification
						if (totalPerLinkRoutingDecision.get(linkRoutingDecision) == null) {
							totalPerLinkRoutingDecision.put(linkRoutingDecision, totalPerRoutingDecision);
						} else {
							int totalRoutingDecisionLink = totalPerLinkRoutingDecision.get(linkRoutingDecision) + totalPerRoutingDecision;
							totalPerLinkRoutingDecision.put(linkRoutingDecision, totalRoutingDecisionLink);
						}
						// reset routing decision
						currentRoutingDecision = "";
						currentRoute = "";
						linkRoutingDecision = 0l;
						totalPerRoutingDecision = 0;
						inRoutingDecision = false;
					}
				}

				// INPUT:
				if (ipPattern.matcher(line).matches()) {
					inInput = true;
				}
				if (inInput) {
					// LINK of ROUTING_DECISION:
					if (linkPattern.matcher(line).matches()) {
						String[] lineVals = line.split(delimiter);
						long linkInput = Long.parseLong(lineVals[2]);
						String replacement;
						if (totalPerLinkRoutingDecision.get(linkInput) != null) {
							replacement = "EXACT " + String.valueOf(totalPerLinkRoutingDecision.get(linkInput)) + ".000 ";
						} else {
							replacement = "EXACT 0.000 ";
						}
						line = replaceSubstring(line, replacement, totValPattern);
					}
					// End of input (EMPTY LINE):
					if (nPattern.matcher(line).matches()) {
						// reset routing decision
						inInput = false;
					}
				}

				out.write(line); out.newLine();
				line = in.readLine();
			}
			in.close();
			out.close();
		} catch (IOException e) {
			System.out.println("Access to " + path2InpFile + " or to " + path2NewInpFile + " failed.");
			e.printStackTrace();
		}
	}

	private String replaceSubstring(String line, String replacement, Pattern replacementPattern) {
		Matcher matcher = replacementPattern.matcher(line);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(sb, replacement);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private int calcTotalPerRoutingDecision(String routingDecision, HashMap<Id<Trip>, Integer> demandPerVissimTrip) {
		int totalPerRoutingDecision = 0;

		String delimiter = "-";
		for (Id<Trip> tripId : demandPerVissimTrip.keySet()) {
			String[] vals = tripId.toString().split(delimiter);
			if (vals[0].equals(routingDecision)) {
				totalPerRoutingDecision += demandPerVissimTrip.get(tripId);
			}
		}

		return totalPerRoutingDecision;
	}
}

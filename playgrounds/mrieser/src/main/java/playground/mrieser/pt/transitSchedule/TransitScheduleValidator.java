/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.pt.transitSchedule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public abstract class TransitScheduleValidator {

	public static ValidationResult validateNetworkRoutes(final TransitSchedule schedule, final Network network) {
		ValidationResult result = new ValidationResult();

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				NetworkRoute netRoute = route.getRoute();
				if (netRoute == null) {
					result.addError("Transit line " + line.getId() + ", route " + route.getId() + " has no network route.");
				} else {
					Link prevLink = network.getLinks().get(netRoute.getStartLinkId());
					for (Id linkId : netRoute.getLinkIds()) {
						Link link = network.getLinks().get(linkId);
						if (!prevLink.getToNode().equals(link.getFromNode())) {
							result.addError("Transit line " + line.getId() + ", route " + route.getId() +
									" has inconsistent network route, e.g. between link " + prevLink.getId() + " and " + linkId);
						}
						prevLink = link;
					}
				}
			}
		}

		return result;
	}

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		ScenarioImpl s = new ScenarioImpl();
		s.getConfig().scenario().setUseTransit(true);
		TransitSchedule ts = s.getTransitSchedule();
		Network net = s.getNetwork();

//		args = new String[] {"/data/projects/bvg2010/Daten/transitSchedule.oevnet.xml", "/data/projects/bvg2010/Daten/transit-network.xml"};
		args = new String[] {"/data/senozon/visumData/matsim/transitSchedule.xml", "/data/senozon/visumData/matsim/network.cleaned.xml"};

		new MatsimNetworkReader(s).parse(args[1]);
		new TransitScheduleReader(s).readFile(args[0]);

		ValidationResult v = validateNetworkRoutes(ts, net);
		if (v.isValid()) {
			System.out.println("Schedule is valid!");
		} else {
			System.out.println("Schedule is NOT valid!");
		}
		if (v.getErrors().size() > 0) {
			System.out.println("Validation errors:");
			for (String e : v.getErrors()) {
				System.out.println(e);
			}
		}
		if (v.getWarnings().size() > 0) {
			System.out.println("Validation warnings:");
			for (String w : v.getWarnings()) {
				System.out.println(w);
			}
		}
	}

	public static class ValidationResult {
		private boolean isValid = true;
		private List<String> warnings = new ArrayList<String>();
		private List<String> errors = new ArrayList<String>();

		public boolean isValid() {
			return this.isValid;
		}

		public List<String> getWarnings() {
			return Collections.unmodifiableList(this.warnings);
		}

		public List<String> getErrors() {
			return Collections.unmodifiableList(this.errors);
		}

		/*package*/ void addWarning(final String warning) {
			this.warnings.add(warning);
		}

		/*package*/ void addError(final String error) {
			this.errors.add(error);
			this.isValid = false;
		}
	}
}

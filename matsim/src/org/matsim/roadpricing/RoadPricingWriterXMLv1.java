/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingWriterXMLv1.java
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

package org.matsim.roadpricing;

import java.io.IOException;

import org.matsim.interfaces.core.v01.Link;
import org.matsim.utils.misc.Time;
import org.matsim.writer.MatsimXmlWriter;

/**
 * Writes a {@link RoadPricingScheme} to a file according to <code>roadpricing_v1.dtd</code>.
 *
 * @author mrieser
 */
public class RoadPricingWriterXMLv1 extends MatsimXmlWriter {

	private final RoadPricingScheme scheme;

	public RoadPricingWriterXMLv1(final RoadPricingScheme scheme) {
		this.scheme = scheme;
	}

	public void writeFile(final String filename) throws IOException {
		openFile(filename);
		writeXmlHead();
		writeDoctype("roadpricing", DEFAULT_DTD_LOCATION + "roadpricing_v1.dtd");
		write();
		close();
	}

	private void write() throws IOException {
		this.writer.write("<roadpricing type=\"" + this.scheme.getType() + "\" name=\"" + this.scheme.getName() + "\">\n");

		// description
		this.writer.write("\t<description>" + this.scheme.getDescription() + "</description>\n");

		// links
		this.writer.write("\t<links>\n");
		for (Link link : this.scheme.getLinks()) {
			this.writer.write("\t\t<link id=\"" + link.getId().toString() + "\" />\n");
		}
		this.writer.write("\t</links>\n");

		// cost
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(this.scheme.getType())) {
			this.writer.write("\t<!-- amount: [monetary unit] / [link length unit] -->\n");
		} else if (this.scheme.getType() == RoadPricingScheme.TOLL_TYPE_AREA) {
			this.writer.write("\t<!-- amount: [monetary unit] / [simulation] -->\n");
		} else if (this.scheme.getType() == RoadPricingScheme.TOLL_TYPE_CORDON) {
			this.writer.write("\t<!-- [monetary unit] / [travelling across a tolled link] -->\n");
		}

		for (RoadPricingScheme.Cost cost : this.scheme.getCosts()) {
			this.writer.write("\t<cost ");
			if (cost.startTime != Time.UNDEFINED_TIME) {
				this.writer.write("start_time=\"" + Time.writeTime(cost.startTime) + "\" ");
			}
			if (cost.endTime != Time.UNDEFINED_TIME) {
				this.writer.write("end_time=\"" + Time.writeTime(cost.endTime) + "\" ");
			}
			this.writer.write("amount=\"" + cost.amount + "\" />\n");
		}

		// finish
		this.writer.write("</roadpricing>");
	}

}

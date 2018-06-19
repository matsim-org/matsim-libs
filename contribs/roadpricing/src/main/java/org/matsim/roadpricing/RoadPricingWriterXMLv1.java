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
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;

/**
 * Writes a {@link RoadPricingSchemeImpl} to a file according to <code>roadpricing_v1.dtd</code>.
 *
 * @author mrieser
 */
public final class RoadPricingWriterXMLv1 extends MatsimXmlWriter {
	// needs to be public. kai, sep'14

	private final RoadPricingScheme scheme;

	public RoadPricingWriterXMLv1(final RoadPricingScheme scheme) {
		this.scheme = scheme;
	}

	public void writeFile(final String filename) throws UncheckedIOException {
		openFile(filename);
		writeXmlHead();
		writeDoctype("roadpricing", DEFAULT_DTD_LOCATION + "roadpricing_v1.dtd");
		write();
		close();
	}

	private void write() throws UncheckedIOException {
		try {
			this.writer.write("<roadpricing type=\"" + this.scheme.getType() + "\" name=\"" + this.scheme.getName() + "\">\n");

			// description
			this.writer.write("\t<description>" + this.scheme.getDescription() + "</description>\n");
	
			// links
			this.writer.write("\t<links>\n");
			for (Id<Link> linkId : this.scheme.getTypicalCostsForLink().keySet()) {
			  List<Cost> cs = this.scheme.getTypicalCostsForLink().get(linkId);
			  this.writer.write("\t\t<link id=\"" + linkId.toString() + "\"");
			  if (cs == null) {
			    this.writer.write("/>\n");
			  }
			  else {
			    this.writer.write(">\n");
			    for (Cost c : cs) {
			      this.writeCost(c);
			    }
			    this.writer.write("</link>");
			  }
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
	
			for (RoadPricingSchemeImpl.Cost cost : this.scheme.getTypicalCosts()) {
			  this.writeCost(cost);
			}
	
			// finish
			this.writer.write("</roadpricing>");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private void writeCost(Cost cost) throws IOException {
    this.writer.write("\t<cost ");
    if (!Time.isUndefinedTime(cost.startTime)) {
      this.writer.write("start_time=\"" + Time.writeTime(cost.startTime) + "\" ");
    }
    if (!Time.isUndefinedTime(cost.endTime) 
    		&& cost.endTime != Double.POSITIVE_INFINITY
    		// The toll reader converts undefined time to POSITIVE_INFINITY since otherwise it does not make sense.
    		// This, however, means that we need to deal with this here as well.  kai, aug'14
    		) {
      this.writer.write("end_time=\"" + Time.writeTime(cost.endTime) + "\" ");
    }
    this.writer.write("amount=\"" + cost.amount + "\" />\n");
	}

}

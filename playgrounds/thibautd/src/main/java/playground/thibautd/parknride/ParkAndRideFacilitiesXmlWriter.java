/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideFacilitiesXmlWriter.java
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
package playground.thibautd.parknride;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Counter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.FACILITY_TAG;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.ID_ATT;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.LINK_ID_ATT;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.NAME_ATT;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.ROOT_TAG;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.STOP_TAG;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.X_COORD_ATT;
import static playground.thibautd.parknride.ParkAndRideFacilitiesXmlSchemaNames.Y_COORD_ATT;

/**
 * Writes a {@link ParkAndRideFacilities} object to an XML file.
 * @author thibautd
 */
public class ParkAndRideFacilitiesXmlWriter extends MatsimXmlWriter {
	private final ParkAndRideFacilities facilities;
	private Counter count = null;

	public ParkAndRideFacilitiesXmlWriter(final ParkAndRideFacilities toWrite) {
		this.facilities = toWrite;
	}

	public void write(final String fileName) {
		count = new Counter( "dumping park an ride facility # " );
		this.openFile(fileName);
		this.write();
		this.close();
	}

	private void write() {
		this.writeXmlHead();
		this.writeStartTag(
				ROOT_TAG,
				Arrays.asList( 
					new Tuple<String, String>(
						NAME_ATT,
						facilities.getName() )));
		this.writeFacilities();
		this.writeEndTag( ROOT_TAG );
	}

	private void writeFacilities() {
		for (ParkAndRideFacility facility : facilities.getFacilities().values()) {
			count.incCounter();
			writeStartTag( FACILITY_TAG , getAttributes( facility ) , false );

			for (Id id : facility.getStopsFacilitiesIds()) {
				writeStartTag(
						STOP_TAG ,
						Arrays.asList( 
							new Tuple<String, String>( ID_ATT , id.toString() )),
						true);
			}

			writeEndTag( FACILITY_TAG );
		}
		count.printCounter();
	}

	private static List<Tuple<String, String>> getAttributes(
			final ParkAndRideFacility facility) {
		List< Tuple<String, String> > atts = new ArrayList< Tuple<String, String> >();

		atts.add( new Tuple<String, String>(
					X_COORD_ATT,
					""+facility.getCoord().getX()) );
		atts.add( new Tuple<String, String>(
					Y_COORD_ATT,
					""+facility.getCoord().getY()) );
		atts.add( new Tuple<String, String>(
					ID_ATT,
					facility.getId().toString()) );
		atts.add( new Tuple<String, String>(
					LINK_ID_ATT,
					facility.getLinkId().toString()) );

		return atts;
	}
}


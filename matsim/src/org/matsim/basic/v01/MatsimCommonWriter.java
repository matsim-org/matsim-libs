/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimCommonWriter
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
package org.matsim.basic.v01;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.matsim.interfaces.basic.v01.BasicLocation;
import org.matsim.interfaces.basic.v01.LocationType;
import org.matsim.utils.collections.Tuple;
import org.matsim.utils.geometry.Coord;
import org.matsim.writer.MatsimXmlWriter;


/**
 * @author dgrether
 *
 */
public class MatsimCommonWriter extends MatsimXmlWriter {

	private List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
	
	public MatsimCommonWriter(Writer writer) {
		this.writer = writer;
	}
	/**
	 * Just a convenience decorator for method writeLocation(Id, Id, coord, int)
	 * @param loc
	 * @param indentationLevel
	 * @throws IOException
	 */
	public void writeLocation(BasicLocation loc, int indentationLevel) throws IOException {
		if (loc.getLocationType() == LocationType.FACILITY){
			this.writeLocation(null, loc.getId(), loc.getCenter(), indentationLevel);
		}
		else {
			this.writeLocation(loc.getId(), null, loc.getCenter(), indentationLevel);
		}
	}
	/**
	 * Write a location to the XML file, i.e. a link OR a facility Id and a coordinate
	 * @param linkId
	 * @param facilityId
	 * @param coord
	 * @param indentationLevel
	 * @throws IOException
	 */
	public void writeLocation(Id linkId, Id facilityId, Coord coord, int indentationLevel) throws IOException {
		this.setIndentationLevel(indentationLevel);
		if ((linkId == null) && (facilityId == null) && (coord == null))
			return;
		this.writeStartTag(PopulationSchemaV5Names.LOCATION, null);
		if (coord != null) {
			this.writeCoordinate(coord);
		}
		if ((linkId != null) || (facilityId != null)) {
			this.writeStartTag(PopulationSchemaV5Names.ACTLOCATION, null);
			atts.clear();
				if ((linkId != null) && (facilityId == null)) {
					atts.add(this.createTuple(PopulationSchemaV5Names.REFID, linkId.toString()));
					this.writeStartTag(PopulationSchemaV5Names.LINKID, atts, true);
				}
				else if ((facilityId != null) && (linkId == null)) {
					atts.add(this.createTuple(PopulationSchemaV5Names.REFID, facilityId.toString()));
					this.writeStartTag(PopulationSchemaV5Names.FACILITYID, atts, true);
				}
				else {
					throw new IllegalArgumentException("An location can only contain one of the elements facilityId or linkId!");
				}
			this.writeEndTag(PopulationSchemaV5Names.ACTLOCATION);
		}
		
		this.writeEndTag(PopulationSchemaV5Names.LOCATION);
	}
	
	public void writeCoordinate(Coord coord, int indentationLevel) throws IOException {
		this.setIndentationLevel(indentationLevel);
		this.writeCoordinate(coord);
	}
	
	private void writeCoordinate(Coord coord) throws IOException {
		this.writeStartTag(PopulationSchemaV5Names.COORDINATE, null);
		this.writeStartTag(PopulationSchemaV5Names.XCOORD, null);
		this.writeContent(Double.toString(coord.getX()), false);
		this.writeEndTag(PopulationSchemaV5Names.XCOORD);
		this.writeStartTag(PopulationSchemaV5Names.YCOORD, null);
		this.writeContent(Double.toString(coord.getY()), false);
		this.writeEndTag(PopulationSchemaV5Names.YCOORD);
		this.writeEndTag(PopulationSchemaV5Names.COORDINATE);
	}
	
	
}

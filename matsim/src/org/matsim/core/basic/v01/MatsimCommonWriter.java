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
package org.matsim.core.basic.v01;

import java.io.IOException;
import java.io.Writer;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.basic.v01.population.PopulationSchemaV5Names;
import org.matsim.core.utils.io.MatsimXmlWriter;


/**
 * @author dgrether
 * @deprecated due to march refactorings however will be recovered in the future
 */
@Deprecated
public class MatsimCommonWriter extends MatsimXmlWriter {

//	private List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
	
	public MatsimCommonWriter(Writer writer) {
		this.writer = writer;
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

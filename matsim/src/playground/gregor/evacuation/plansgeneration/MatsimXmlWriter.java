/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimXmlWriter.java
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

package playground.gregor.evacuation.plansgeneration;

import java.io.IOException;


public class MatsimXmlWriter extends MatsimWriter {
	
	public MatsimXmlWriter() {
		
	}

	protected void writeXmlHead() throws IOException {
		this.writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL);
	}
	
	protected void writeDoctype(String rootTag, String dtdUrl) throws IOException {
		this.writer.write("<!DOCTYPE " + rootTag + " SYSTEM \"" + dtdUrl + "\">" + NL);
	}
	
}

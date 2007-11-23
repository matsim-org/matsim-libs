/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimWriter.java
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
import java.io.Writer;

import org.matsim.utils.io.IOUtils;

public class MatsimWriter {
	
	protected static final String NL = "\n"; 
	
	protected Writer writer = null;	
	protected boolean useCompression = true;
	
	public MatsimWriter() {
		
	}
	
	public void useCompression(boolean useCompression) {
		this.useCompression = useCompression;
	}
	
	protected void openFile(final String filename)  throws IOException {
		this.writer = IOUtils.getBufferedWriter(filename, useCompression);
	}
	
	protected void close() throws IOException {
		this.writer.close();
		this.writer = null;
	}
}

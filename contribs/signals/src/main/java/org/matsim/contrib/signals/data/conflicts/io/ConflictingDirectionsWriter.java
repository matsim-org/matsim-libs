/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.contrib.signals.data.conflicts.io;

import org.apache.log4j.Logger;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * @author tthunig
 */
public class ConflictingDirectionsWriter extends MatsimXmlWriter implements MatsimWriter {

	private static final Logger LOG = Logger.getLogger(ConflictingDirectionsWriter.class);
	
	private ConflictData conflictData;
	
	public ConflictingDirectionsWriter(ConflictData conflictData) {
		this.conflictData = conflictData;
	}
	
	@Override
	public void write(String filename) {
		LOG.info("Writing conflicting direction data to file: " + filename + "...");
		
		String dtd = null; // TODO
		ConflictingDirectionsWriterHandlerImpl handler = new ConflictingDirectionsWriterHandlerImpl();
		writeFile(dtd, handler, filename);
		
		LOG.info("done.");
	}

	private void writeFile(String dtd, ConflictingDirectionsWriterHandlerImpl handler, String filename) {
		// TODO Auto-generated method stub
		
	}

}

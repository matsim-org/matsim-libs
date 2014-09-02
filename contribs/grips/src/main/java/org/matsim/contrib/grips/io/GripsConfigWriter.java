/* *********************************************************************** *
 * project: org.matsim.*
 * GripsConfigWriter.java
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
 * *********************************************************************** */

package org.matsim.contrib.grips.io;

import java.io.IOException;

import org.matsim.contrib.grips.model.config.GripsConfigModule;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.AbstractMatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

public class GripsConfigWriter extends AbstractMatsimWriter implements MatsimWriter{

	private final GripsConfigModule gcm;

	public GripsConfigWriter(GripsConfigModule gripsConfigModule) {
		this.gcm = gripsConfigModule;
	}
	
	@Override
	public void write(String filename) {

		try {
			openFile(filename);
			writeHeaderAndStartGripsConfig();
			writeNetworkFile();
			writeMainTrafficType();
			writeEvacuationAreaFile();
			writePopulationFile();
			writeOutputDir();
			writeSampleSize();
			writeDepartureTimeDistribution();
			endGripsConfig();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			this.close();
		}
	}

	private void writeDepartureTimeDistribution() throws IOException {
		this.writer.write("\t<departureTimeDistribution>\n");
		this.writer.write("\t\t<distribution>");
		this.writer.write(this.gcm.getDepartureTimeDistribution().getDistribution());
		this.writer.write("</distribution>\n");
		this.writer.write("\t\t<sigma>");
		this.writer.write(Double.toString(this.gcm.getDepartureTimeDistribution().getSigma()));
		this.writer.write("</sigma>\n");
		this.writer.write("\t\t<mu>");
		this.writer.write(Double.toString(this.gcm.getDepartureTimeDistribution().getMu()));
		this.writer.write("</mu>\n");
		this.writer.write("\t\t<earliest>");
		this.writer.write(Double.toString(this.gcm.getDepartureTimeDistribution().getEarliest()));
		this.writer.write("</earliest>\n");		
		this.writer.write("\t\t<latest>");
		this.writer.write(Double.toString(this.gcm.getDepartureTimeDistribution().getLatest()));
		this.writer.write("</latest>\n");
		this.writer.write("\t</departureTimeDistribution>\n\n");
	}

	private void writeSampleSize() throws IOException {
		this.writer.write("\t<sampleSize>");
		this.writer.write(Double.toString(this.gcm.getSampleSize()));
		this.writer.write("</sampleSize>\n\n");
		
	}

	private void writeOutputDir() throws IOException {
		this.writer.write("\t<outputDir>\n");
		this.writer.write("\t\t<inputFile>");
		this.writer.write(this.gcm.getOutputDir());
		this.writer.write("</inputFile>\n");
		this.writer.write("\t</outputDir>\n\n");
	}

	private void writePopulationFile() throws IOException {
		this.writer.write("\t<populationFile>\n");
		this.writer.write("\t\t<inputFile>");
		this.writer.write(this.gcm.getPopulationFileName());
		this.writer.write("</inputFile>\n");
		this.writer.write("\t</populationFile>\n\n");
		
	}

	private void writeEvacuationAreaFile() throws IOException {
		this.writer.write("\t<evacuationAreaFile>\n");
		this.writer.write("\t\t<inputFile>");
		this.writer.write(this.gcm.getEvacuationAreaFileName());
		this.writer.write("</inputFile>\n");
		this.writer.write("\t</evacuationAreaFile>\n\n");
		
	}

	private void writeMainTrafficType() throws IOException {
		this.writer.write("\t<mainTrafficType>");
		this.writer.write(this.gcm.getMainTrafficType());
		this.writer.write("</mainTrafficType>\n\n");
	}

	private void writeNetworkFile() throws IOException {
		this.writer.write("\t<networkFile>\n");
		this.writer.write("\t\t<inputFile>");
		this.writer.write(this.gcm.getNetworkFileName());
		this.writer.write("</inputFile>\n");
		this.writer.write("\t</networkFile>\n\n");
	}

	private void endGripsConfig() throws IOException {
		this.writer.write("</grips_config>");
	}

	

	private void writeHeaderAndStartGripsConfig() throws IOException {
		this.writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
		this.writer.write("<grips_config xsi:noNamespaceSchemaLocation=\""+MatsimXmlWriter.DEFAULT_DTD_LOCATION+"grips_config_v0.1.xsd\" "
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n\n");
	}



}

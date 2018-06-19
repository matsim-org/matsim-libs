/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.transitSchedule.api;

import java.io.InputStream;
import java.net.URL;
import java.util.Stack;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV2;
import org.xml.sax.Attributes;

/**
 * Reads {@link TransitSchedule}s from file as long as the files are in one of the
 * supported file formats.
 *
 * @author mrieser
 */
public class TransitScheduleReader implements MatsimReader {

	private final Scenario scenario;
	private final CoordinateTransformation transformation;

	public TransitScheduleReader(
			final CoordinateTransformation transformation,
			final Scenario scenario) {
		this.transformation = transformation;
		this.scenario = scenario;
	}

	public TransitScheduleReader(final Scenario scenario) {
		this(new IdentityTransformation(), scenario);
	}

	@Override
	public void readFile(final String filename) throws UncheckedIOException {
		new XmlScheduleReader(this.transformation, this.scenario).readFile(filename);
	}

	public void readURL(final URL url) throws UncheckedIOException {
		new XmlScheduleReader(this.transformation, this.scenario).parse(url);
	}

	public void readStream(final InputStream stream) throws UncheckedIOException {
		new XmlScheduleReader(this.transformation, this.scenario).parse(stream);
	}

	private static class XmlScheduleReader extends MatsimXmlParser {

		private MatsimXmlParser delegate = null;
		private final CoordinateTransformation transformation;
		private final Scenario scenario;

		public XmlScheduleReader(CoordinateTransformation transformation, Scenario scenario) {
			this.transformation = transformation;
			this.scenario = scenario;
		}

		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			this.delegate.startTag(name, atts, context);
		}

		@Override
		public void endTag(String name, String content, Stack<String> context) {
			this.delegate.endTag(name, content, context);
		}

		@Override
		protected void setDoctype(String doctype) {
			super.setDoctype(doctype);

			if ("transitSchedule_v2.dtd".equals(doctype)) {
				this.delegate = new TransitScheduleReaderV2(this.transformation, this.scenario);
			} else if ("transitSchedule_v1.dtd".equals(doctype)) {
				this.delegate = new TransitScheduleReaderV1(this.transformation, this.scenario);
			} else {
				throw new IllegalArgumentException("Unsupported doctype: " + doctype);
			}
		}
	}

}

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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV2;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Stack;

/**
 * Reads {@link TransitSchedule}s from file as long as the files are in one of the
 * supported file formats.
 *
 * @author mrieser
 */
public class TransitScheduleReader implements MatsimReader {

	private final Scenario scenario;

	private final String externalInputCRS;
	private final String targetCRS;

	public TransitScheduleReader(
	        final String targetCRS,
			final Scenario scenario) {
	    this(null, targetCRS, scenario);
	}

	public TransitScheduleReader(
	        final String externalInputCRS,
			final String targetCRS,
			final Scenario scenario) {
		this.externalInputCRS = externalInputCRS;
		this.targetCRS = targetCRS;
		this.scenario = scenario;
    }

	public TransitScheduleReader(final Scenario scenario) {
		this(null, null, scenario);
	}

	@Override
	public void readFile(final String filename) throws UncheckedIOException {
		new XmlScheduleReader(externalInputCRS, targetCRS, this.scenario).readFile(filename);
	}
	@Override
	public void readURL( final URL url ) throws UncheckedIOException {
		new XmlScheduleReader(externalInputCRS, targetCRS, this.scenario).parse(url);
	}

	public void readStream(final InputStream stream) throws UncheckedIOException {
		new XmlScheduleReader(externalInputCRS, targetCRS, this.scenario).parse(stream);
	}

	private static class XmlScheduleReader extends MatsimXmlParser {

		private MatsimXmlParser delegate = null;
		private final String externalInputCRS;
		private final String targetCRS;
		private final Scenario scenario;

		public XmlScheduleReader(String externalInputCRS, String targetCRS, Scenario scenario) {
			super(ValidationType.DTD_ONLY);
			this.externalInputCRS = externalInputCRS;
			this.targetCRS = targetCRS;
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
				this.delegate = new TransitScheduleReaderV2(externalInputCRS, targetCRS, this.scenario);
			} else if ("transitSchedule_v1.dtd".equals(doctype)) {
				this.delegate = new TransitScheduleReaderV1(
						externalInputCRS != null ?
								TransformationFactory.getCoordinateTransformation(externalInputCRS, targetCRS) :
								new IdentityTransformation(),
						this.scenario);
			} else {
				throw new IllegalArgumentException("Unsupported doctype: " + doctype);
			}
		}

		@Override
		public void endDocument() {
			try {
				this.delegate.endDocument();
			} catch (SAXException e) {
				throw new RuntimeException(e);
			}
			if (targetCRS != null) {
				ProjectionUtils.putCRS(scenario.getTransitSchedule(), targetCRS);
			}
		}
	}

}

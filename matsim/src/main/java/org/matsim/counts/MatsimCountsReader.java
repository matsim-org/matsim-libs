/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimCountsReader.java
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

package org.matsim.counts;

import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.referencing.operation.transform.IdentityTransform;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * A reader for counts-files of MATSim. This reader recognizes the format of the counts-file and uses
 * the correct reader for the specific counts-version, without manual setting.
 *
 * @author mrieser
 */
public class MatsimCountsReader extends MatsimXmlParser {

	private final static Logger log = LogManager.getLogger(MatsimCountsReader.class);
	private final static String COUNTS_V1 = "counts_v1.xsd";
	private final static String COUNTS_V2 = "counts_v2.xsd";
	private final Counts counts;
	private final String inputCRS;
	private final String targetCRS;
	private MatsimXmlParser delegate = null;
	private final Class<? extends Identifiable<?>> idClass;


	/**
	 * Creates a new reader for MATSim counts files.
	 *
	 * @param counts The Counts-object to store the configuration settings in.
	 */
	public MatsimCountsReader(final Counts counts) {
		this( null, null, counts, Link.class );
	}

	/**
	 * Creates a new reader for MATSim counts files.
	 *
	 * @param inputCRS
	 * @param targetCRS
	 * @param counts    The Counts-object to store the configuration settings in.
	 */
	public MatsimCountsReader(
			String inputCRS, String targetCRS, final Counts counts ) {
		this( inputCRS, targetCRS, counts, Link.class );
	}

	/**
	 * Creates a new reader for MATSim counts files.
	 *
	 * @param inputCRS
	 * @param targetCRS
	 * @param counts    the counts object to store the configuration settings in
	 * @param idClass   id class of locations
	 */
	public MatsimCountsReader(
			String inputCRS, String targetCRS, final Counts counts,
			Class<? extends Identifiable<?>> idClass) {
		super(ValidationType.XSD_ONLY);
		this.inputCRS = inputCRS;
		this.targetCRS = targetCRS;
		this.counts = counts;
		this.idClass = idClass;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		this.delegate.startTag(name, atts, context);
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		this.delegate.endTag(name, content, context);
	}

	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		// Currently the only counts-type is v1
		if (COUNTS_V1.equals(doctype)) {
			CoordinateTransformation coordinateTransformation = new IdentityTransformation();
			if (inputCRS != null && targetCRS != null) {
				coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, targetCRS );
			}
			this.delegate = new CountsReaderMatsimV1( coordinateTransformation, this.counts);
			log.info("using counts_v1-reader.");
		} else if (COUNTS_V2.equals(doctype)) {
			this.delegate = new CountsReaderMatsimV2( inputCRS, targetCRS, this.counts, idClass);
			log.info("using counts_v2-reader.");

		} else {
			throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
	}

}

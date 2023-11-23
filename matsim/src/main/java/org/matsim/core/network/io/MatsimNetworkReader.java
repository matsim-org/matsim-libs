/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimNetworkReader.java
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

package org.matsim.core.network.io;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader for network-files of MATSim. This reader recognizes the format of the network-file and uses
 * the correct reader for the specific network-version, without manual setting.
 *
 * @author mrieser
 */
public final class MatsimNetworkReader extends MatsimXmlParser {

	private final static Logger log = LogManager.getLogger(MatsimNetworkReader.class);
	private final static String NETWORK_V1 = "network_v1.dtd";
	private final static String NETWORK_V2 = "network_v2.dtd";

	private MatsimXmlParser delegate = null;
	private final String inputCRS;
	private final String targetCRS;

	private final Network network;
	private Map<Class<?>, AttributeConverter<?>> converters = new HashMap<>();

	/**
	 * Creates a new reader for MATSim network files.
	 *
	 * @param network The network where to store the loaded data.
	 */
	public MatsimNetworkReader(Network network) {
		this( null , network );
	}

	/**
	 * Creates a new reader for MATSim network files, that transforms coordinates
	 *
	 * @param targetCRS the string representation of the CRS the coordinates should be transformed to (usually an EPSG code)
	 * @param network The network where to store the loaded data.
	 */
	public MatsimNetworkReader(String targetCRS, Network network) {
	    this(null, targetCRS, network);
	}

	public MatsimNetworkReader(String inputCRS, String targetCRS, Network network) {
		super(ValidationType.DTD_ONLY);
		this.inputCRS = inputCRS;
		this.targetCRS = targetCRS;
		this.network = network;
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
	public void endDocument() {
		try {
			this.delegate.endDocument();
		} catch (SAXException e) {
		    throw new RuntimeException(e);
		}
		if (targetCRS != null) {
			ProjectionUtils.putCRS(network, targetCRS);
		}
	}

	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);

		switch ( doctype ) {
			case NETWORK_V1:
				this.delegate =
						new NetworkReaderMatsimV1(
								inputCRS != null ?
										TransformationFactory.getCoordinateTransformation(inputCRS, targetCRS) :
										new IdentityTransformation(),
								this.network);
				log.info("using network_v1-reader.");
				break;
			case NETWORK_V2:
				this.delegate = new NetworkReaderMatsimV2(inputCRS, targetCRS, this.network);
				((NetworkReaderMatsimV2) delegate).putAttributeConverters( converters );
				log.info("using network_v2-reader.");
				break;
			default:
				throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
	}

	public void putAttributeConverter(Class<?> clazz, AttributeConverter<?> converter) {
		this.converters.put( clazz, converter );
	}

	public void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> attributeConverters) {
		this.converters.putAll( attributeConverters );
	}
}

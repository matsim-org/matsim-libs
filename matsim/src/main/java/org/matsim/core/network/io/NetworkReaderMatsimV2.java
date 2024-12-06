/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkReaderMatsimV1.java
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.xml.sax.Attributes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate.TAG_ATTRIBUTE;
import static org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate.TAG_ATTRIBUTES;

/**
 * A reader for network-files of MATSim according to <code>network_v1.dtd</code>.
 *
 * @author mrieser
 */
final class NetworkReaderMatsimV2 extends MatsimXmlParser {

	private final static String NETWORK = "network";
	private final static String LINKS = "links";
	private final static String NODE = "node";
	private final static String LINK = "link";
	private final Network network;

	private final AttributesXmlReaderDelegate attributesDelegate = new AttributesXmlReaderDelegate();
	private org.matsim.utils.objectattributes.attributable.Attributes currentAttributes = null;

	private final String externalInputCRS;
	private final String targetCRS;
	private CoordinateTransformation coordinateTransformation = new IdentityTransformation();

	private final static Logger log = LogManager.getLogger(NetworkReaderMatsimV2.class);

	NetworkReaderMatsimV2(
	        final String inputCRS,
			final String targetCRS,
			final Network network) {
		super(ValidationType.DTD_ONLY);
		this.externalInputCRS = inputCRS;
		this.targetCRS = targetCRS;
		if (externalInputCRS != null && targetCRS != null) {
			this.coordinateTransformation = TransformationFactory.getCoordinateTransformation(externalInputCRS, targetCRS);
			ProjectionUtils.putCRS(network, targetCRS);
		}
		this.network = network;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		switch( name ){
			case NODE -> startNode( atts );
			case LINK -> startLink( atts );
			case NETWORK -> startNetwork( atts );
			case LINKS -> startLinks( atts );
			case TAG_ATTRIBUTES, TAG_ATTRIBUTE -> attributesDelegate.startTag( name, atts, context, currentAttributes );
//			default -> throw new IllegalStateException( "Unexpected value: " + name );
			// (there is at least "nodes" which just passes through.  I found it that way.  kai, dec'24)
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		switch( name ) {
			case TAG_ATTRIBUTES:
				if (context.peek().equals(NETWORK)) {
					String inputCRS = (String) network.getAttributes().getAttribute(ProjectionUtils.INPUT_CRS_ATT);
					if (inputCRS != null && targetCRS != null) {
						if (externalInputCRS != null) {
							// warn or crash?
							log.warn("coordinate transformation defined both in config and in input file: setting from input file will be used");
						}
						coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, targetCRS);
						ProjectionUtils.putCRS(network, targetCRS);
					}
				}
				/* fall-through */
			case TAG_ATTRIBUTE:
				attributesDelegate.endTag(name, content, context);
				break;
		}
	}

	private void startNetwork(final Attributes atts) {
		if (atts.getValue("type") != null) {
			log.info("Attribute 'type' is deprecated. There's always only ONE network, where the links and nodes define, which " +
					"transportation mode is allowed to use it (for the future)");
		}
		this.network.setName(atts.getValue("name"));
		if (atts.getValue("capDivider") != null) {
			log.warn("capDivider defined. it will be used but should be gone eventually. " +
					"-- This is a weird comment, since the matsim public api tells to put this into the network rather than" +
					" into the ``links''.  kai, jun'11");
			String capperiod = atts.getValue("capDivider") + ":00:00";
			this.network.setCapacityPeriod(Time.parseTime(capperiod));
		}

		currentAttributes = network.getAttributes();
	}

	private void startLinks(final Attributes atts) {
		double capacityPeriod = 3600.0; //the default of one hour
		String capperiod = atts.getValue("capperiod");
		if (capperiod != null) {
			capacityPeriod = Time.parseTime(capperiod);
		}
		else {
			log.warn("capperiod was not defined. Using default value of " + Time.writeTime(capacityPeriod) + ".");
		}
		this.network.setCapacityPeriod(capacityPeriod);

		String effectivecellsize = atts.getValue("effectivecellsize");
		if (effectivecellsize == null){
			this.network.setEffectiveCellSize(7.5); // we use a default cell size of 7.5 meters
		} else {
			this.network.setEffectiveCellSize(Double.parseDouble(effectivecellsize));
		}

		String effectivelanewidth = atts.getValue("effectivelanewidth");
		if (effectivelanewidth == null) {
			this.network.setEffectiveLaneWidth(3.75); // the default lane width is 3.75
		} else {
			this.network.setEffectiveLaneWidth(Double.parseDouble(effectivelanewidth));
		}

		if ((atts.getValue("capPeriod") != null) || (atts.getValue("capDivider") != null) || (atts.getValue("capdivider") != null)) {
			log.warn("Found capPeriod, capDivider and/or capdivider in the links element.  They will be ignored, since they " +
					"should be set in the network element. -- This is a weird warning, since setting them in the " +
					"network element also produces a warning.");
			log.warn("At this point, it seems that, in network.xml, one sets capperiod in the `links' section, but in the " +
					"matsim api, the corresponding entry belongs into the `network' object. kai, jun'11") ;
		}
	}

	private void startNode(final Attributes atts) {
		final Node node =
				this.network.getFactory().createNode(
						Id.create(atts.getValue("id"), Node.class),
						parseCoord(atts));
		this.network.addNode(node);

		NetworkUtils.setType(node,atts.getValue("type"));
		// (did not have a null check when I found it.  kai, jul'16)

		if (atts.getValue(NetworkUtils.ORIGID) != null) {
			NetworkUtils.setOrigId( node, atts.getValue(NetworkUtils.ORIGID) ) ;
		}

		currentAttributes = node.getAttributes();
	}

	private Coord parseCoord(Attributes atts) {
		final Coord c = atts.getValue( "z" ) == null ?
				new Coord(
						Double.parseDouble(atts.getValue("x")),
						Double.parseDouble(atts.getValue("y"))) :
				new Coord(
						Double.parseDouble(atts.getValue("x")),
						Double.parseDouble(atts.getValue("y")),
						Double.parseDouble(atts.getValue("z")));
		return coordinateTransformation.transform( c );
	}

	private void startLink(final Attributes atts) {
		final String fromNodeStr = atts.getValue("from");
		Node fromNode = this.network.getNodes().get(Id.create(fromNodeStr, Node.class));
		if ( fromNode==null ) {
			throw new RuntimeException("node id given by link cannot be dereferenced; node label=" + fromNodeStr ) ;
		}
		final String toNodeStr = atts.getValue("to");
		Node toNode = this.network.getNodes().get(Id.create(toNodeStr, Node.class));
		if ( toNode==null ) {
			throw new RuntimeException("node id given by link cannot be dereferenced; node label=" + toNodeStr ) ;
		}
		Link l = this.network.getFactory().createLink(Id.create(atts.getValue("id"), Link.class), fromNode, toNode);
		l.setLength(Double.parseDouble(atts.getValue("length")));
		l.setFreespeed(Double.parseDouble(atts.getValue("freespeed")));
		l.setCapacity(Double.parseDouble(atts.getValue("capacity")));
		l.setNumberOfLanes(Double.parseDouble(atts.getValue("permlanes")));
		this.network.addLink(l);
		{
			String value = atts.getValue(NetworkUtils.ORIGID);
			if ( value != null ) {
				NetworkUtils.setOrigId( (l), value ) ; // will now put it into the attributes. kai, dec'16
			}
		}
		{
			final String value = atts.getValue(NetworkUtils.TYPE);
			if ( value != null ) {
				NetworkUtils.setType( (l), value); // will now put it into the attributes. kai, dec'16
			}
		}

		if (atts.getValue("modes") != null) {
			String[] strModes = StringUtils.explode(atts.getValue("modes"), ',');
			if ((strModes.length == 1) && strModes[0].isEmpty()) {
				l.setAllowedModes(new HashSet<String>());
			} else {
				Set<String> modes = new HashSet<>();
				for (String strMode : strModes) {
					modes.add(strMode.trim().intern());
				}
				l.setAllowedModes(modes);
			}
		}

		currentAttributes = l.getAttributes();
	}

	public void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> converters) {
		this.attributesDelegate.putAttributeConverters( converters );
	}
}

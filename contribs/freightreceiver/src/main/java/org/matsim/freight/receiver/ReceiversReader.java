/* *********************************************************************** *
 * project: org.matsim.*
 * ReceiversReader.java
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

package org.matsim.freight.receiver;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * Reader for the container of freight {@link Receivers} in MATSim XML format.
 * This reader recognises the format of the plans-file and uses the correct
 * reader for the specific plans-version, without manual setting.
 *
 * @author jwjoubert
 */
public final class ReceiversReader extends MatsimXmlParser{
	private final static String RECEIVERS_V1 = "freightReceivers_v1.dtd";
	private final static String RECEIVERS_V2 = "freightReceivers_v2.dtd";
	private MatsimXmlParser delegate = null;
	private final Receivers receivers;

	public ReceiversReader(Receivers receivers) {
		super(ValidationType.DTD_ONLY);
		this.receivers = receivers;
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
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		switch (doctype) {
			case RECEIVERS_V1 -> throw new IllegalArgumentException("There is no backward compatibility for v1. It had inconsistent file formats.");
			case RECEIVERS_V2 -> this.delegate = new ReceiversReaderV2(this.receivers);
			default -> throw new IllegalArgumentException("No receivers reader available for doctype \"" + doctype + "\".");
		}
	}

}

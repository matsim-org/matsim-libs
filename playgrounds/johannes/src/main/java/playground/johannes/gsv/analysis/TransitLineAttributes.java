/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.johannes.gsv.analysis;

import org.apache.log4j.Logger;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * @author johannes
 *
 */
public class TransitLineAttributes {

	private static final Logger logger = Logger.getLogger(TransitLineAttributes.class);
	
	public static final String TRANSPORT_SYSTEM_KEY = "transportsystem";
	
	private final ObjectAttributes attributes;
	
	public TransitLineAttributes() {
		attributes = new ObjectAttributes();
	}
	
	public void setTransportSystem(String transitLineId, String value) {
		if(attributes.putAttribute(transitLineId, TRANSPORT_SYSTEM_KEY, value) != null) {
			logger.warn("Overwriting transport system for transit line.");
		}
	}
	
	public String getTransportSystem(String transitLineId) {
		return (String) attributes.getAttribute(transitLineId, TRANSPORT_SYSTEM_KEY);
	}
	
	public static TransitLineAttributes createFromFile(String file) {
		TransitLineAttributes attributes = new TransitLineAttributes();
		ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(attributes.attributes);
		reader.parse(file);
		
		return attributes;
	
	}
	
	public static void writeToFile(TransitLineAttributes attributes, String file) {
		ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(attributes.attributes);
		writer.writeFile(file);
	}
}

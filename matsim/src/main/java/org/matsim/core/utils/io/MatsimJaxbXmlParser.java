/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimJaxbXmlParser
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
package org.matsim.core.utils.io;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.net.URL;
import java.util.Stack;


/**
 * This class serves as abstract super class for all jaxb api based xml parsers
 * within the MATSim framework. By using the validate() method of this class
 * jaxb based parsers can validate against xml schemas on- and offline. For
 * offline validation the SAXValidator capabilities of the MatsimXmlParser
 * super class are used.
 * Offline validation has to be done via SAX due to the fact that one can
 * use the jaxb validation by calling the following methods:
 * <p>
 * <code>
 * Schema s = XMLSchemaFactory.newSchema(filename of local schema); 
 * Unmarshaller u.setSchema(s);
 * </code>
 * This unfortunately works only with schemas not including other 
 * schema documents which is no option within the MATSim framework.
 * Other workarounds exist, however they result in more complicated
 * code.
 * @author dgrether
 *
 */
public abstract class MatsimJaxbXmlParser extends MatsimXmlParser {

	private static final Logger log = Logger.getLogger(MatsimJaxbXmlParser.class);
	
	private String schemaLocation;

	/**
	 * 
	 * @param schemaLocation the url where the schema is located (also called systemId)
	 */
	public MatsimJaxbXmlParser(String schemaLocation){
		this.schemaLocation = schemaLocation;
	}

	public abstract void readFile(String filename) throws JAXBException, SAXException, ParserConfigurationException, IOException;
	
	protected void validateFile(String filename, Unmarshaller u) throws SAXException, ParserConfigurationException, IOException{
		URL schemaUrl = null;
		SchemaFactory schemaFac = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = null;
		try {
			//first check if we are online 
			new URL(this.schemaLocation).openStream();
			/* If there was no exception until here, than the path is valid.
			 * Return the opened stream as a source. If we would return null, then the SAX-Parser
			 * would have to fetch the same file again, requiring two accesses to the webserver */
			schemaUrl = new URL(this.schemaLocation);
		} catch (IOException e) {
			// There was a problem getting the (remote) file, just show the error as information for the user
			log.error(e.toString() + ". May not be fatal." );
		}
		// we are online so we can use jaxb directly to validate against the online schema
		if (schemaUrl != null){
			schema = schemaFac.newSchema(schemaUrl);
			u.setSchema(schema);
			log.info("Validating file against schema online at " + this.schemaLocation);
		}
		//we are not online so we use sax parser of MatsimXmlParser to validate file first
		//afterwards it can be read by the jaxb parser
		else{
			log.info("Validating file against schema locally provided in dtd folder");
			this.parse(filename);
			log.info("File valid...");
		}
	}


	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}


	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
	}
	
	
		
}

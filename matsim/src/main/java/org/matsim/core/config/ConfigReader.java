/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimConfigReader.java
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

package org.matsim.core.config;

import java.io.File;
import java.io.InputStream;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

/**
 * A reader for config-files of MATSim. This reader recognizes the format of the config-file and uses
 * the correct reader for the specific config-version, without manual setting.
 *
 * @author mrieser
 */
public class ConfigReader extends MatsimXmlParser implements MatsimSomeReader {

	private final static Logger log = Logger.getLogger(ConfigReader.class);

	private final static String CONFIG_V1 = "config_v1.dtd";
	private final static String CONFIG_V2 = "config_v2.dtd";

	private final Config config;
	private MatsimXmlParser delegate = null;

	private String localDtd;

	/**
	 * Creates a new reader for MATSim configuration files.
	 *
	 * @param config The Config-object to store the configuration settings in. 
	 * <p/>
	 * As far as I can tell, pre-existing settings in config are preserved
	 * except if they are explicitly overwritten by entries in the config file.  The MATSim4UrbanSim initialization sequence depends on this
	 * behavior.  kn, mar'13 
	 */
	public ConfigReader(final Config config) {
		this.config = config;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		this.delegate.startTag(name, atts, context);
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		this.delegate.endTag(name, content, context);
	}

	/**
	 * Parses the specified config file. This method calls {@link #parse(String)}.
	 *
	 * @param filename The name of the file to parse.
	 * @throws UncheckedIOException e.g. if the file cannot be found
	 */
	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
	}

	/**
	 * Parses the specified config file, and uses the given dtd file as a local copy to use as dtd
	 * if the one specified in the config file cannot be found.
	 *
	 * @param filename The name of the file to parse.
	 * @param dtdFilename The name of a (local) dtd-file to be used for validating the config file.
	 * @throws UncheckedIOException e.g. if the file cannot be found
	 */
	public void readFile(final String filename, final String dtdFilename) throws UncheckedIOException {
		log.info("trying to read config from " + filename);
	  this.localDtd = dtdFilename;
		parse(filename);
		this.localDtd = null;
	}

	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		// Currently the only config-type is v1
		if (CONFIG_V1.equals(doctype)) {
			this.delegate = new ConfigReaderMatsimV1(this.config);
			log.info("using config_v1-reader.");
		}
		else if ( CONFIG_V2.equals( doctype ) ) {
			this.delegate = new ConfigReaderMatsimV2( this.config );
			log.info( "using config_v2-reader" );
		}
		else {
			throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
	}

	@Override
	public InputSource resolveEntity(final String publicId, final String systemId) {
		InputSource is = super.resolveEntity(publicId, systemId);
		if (is != null) {
			// everything is fine, we can access the dtd
			return is;
		}
		// okay, standard procedure failed... let's see if we have it locally
		if (this.localDtd != null) {
			File dtdFile = new File(this.localDtd);
			if (dtdFile.exists() && dtdFile.isFile() && dtdFile.canRead()) {
				log.info("Using the local DTD " + this.localDtd);
				return new InputSource(this.localDtd);
			}
		}
		// hmm, didn't find the local one either... maybe inside a jar somewhere?
		int index = systemId.replace('\\', '/').lastIndexOf('/');
		String shortSystemId = systemId.substring(index + 1);
		InputStream stream = this.getClass().getResourceAsStream("/dtd/" + shortSystemId);
		if (stream != null) {
			log.info("Using local DTD from jar-file " + shortSystemId);
			return new InputSource(stream);
		}
		// we fail...
		return null;
	}

}

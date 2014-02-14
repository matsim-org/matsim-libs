/* *********************************************************************** *
 * project: org.matsim.*
 * NonFlatConfigWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.config;

import static playground.thibautd.config.NonFlatConfigXmlNames.CONFIG;
import static playground.thibautd.config.NonFlatConfigXmlNames.MODULE;
import static playground.thibautd.config.NonFlatConfigXmlNames.NAME;
import static playground.thibautd.config.NonFlatConfigXmlNames.PARAMETER;
import static playground.thibautd.config.NonFlatConfigXmlNames.PARAMETER_SET;
import static playground.thibautd.config.NonFlatConfigXmlNames.TYPE;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author thibautd
 */
public class NonFlatConfigWriter extends MatsimXmlWriter {
	private final Config config;

	public NonFlatConfigWriter(final Config config) {
		this.config = config;
	}

	public void write(final String file) {
		openFile( file );

		writeXmlHead();
		writeDoctype( CONFIG , "nonflatconfig_v1.dtd" );
		writeStartTag(
				CONFIG,
				Collections.<Tuple<String,String>>emptyList() );
		writeModules();
		writeEndTag( CONFIG );

		close();
	}

	private void writeModules() {
		for ( Map.Entry<String, Module> module : config.getModules().entrySet() ) {
			writeModule(
					"",
					MODULE,
					NAME,
					module.getKey(),
					module.getValue() );
		}
	}

	public void writeModule(
			final String indent,
			final String moduleTag,
			final String moduleNameAtt,
			final String moduleName,
			final Module module) {
		Map<String, String> params = module.getParams();
		Map<String, String> comments = module.getComments();

		try {
			writer.newLine();
			writer.write( indent );
			writer.write("\t<"+moduleTag);
			writer.write(" "+moduleNameAtt+"=\"" + moduleName + "\" >");
			writer.newLine();
			
			boolean lastHadComment = false;

			for (Map.Entry<String, String> entry : params.entrySet()) {
				if (comments.get(entry.getKey()) != null) {
					writer.newLine();
					writer.write( indent );
					writer.write( "\t\t<!-- " + comments.get(entry.getKey()) + " -->");
					writer.newLine();
					lastHadComment = true;
				} else {
					if (lastHadComment) {
						writer.newLine();
					}
					lastHadComment = false;
				}
				writer.write( indent );
				writer.write("\t\t<"+PARAMETER+" name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" />");
				writer.newLine();
			}

			if ( module instanceof NonFlatModule ) {
				for ( Map.Entry<String, Collection<Module>> entry : ((NonFlatModule) module).getParameterSets().entrySet() ) {
					for ( Module pSet : entry.getValue() ) {
						writeModule(
								indent+"\t",
								PARAMETER_SET,
								TYPE,
								entry.getKey(),
								pSet );
					}
				}
			}

			writer.write( indent );
			writer.write("\t</"+moduleTag+">");
			writer.newLine();

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}


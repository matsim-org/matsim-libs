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
package org.matsim.core.config;

import static org.matsim.core.config.ConfigV2XmlNames.CONFIG;
import static org.matsim.core.config.ConfigV2XmlNames.MODULE;
import static org.matsim.core.config.ConfigV2XmlNames.NAME;
import static org.matsim.core.config.ConfigV2XmlNames.PARAMETER;
import static org.matsim.core.config.ConfigV2XmlNames.PARAMETER_SET;
import static org.matsim.core.config.ConfigV2XmlNames.TYPE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.core.config.ConfigWriter.Verbosity;
import org.matsim.core.config.groups.ChangeLegModeConfigGroup;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author thibautd
 */
class ConfigWriterHandlerImplV2 implements ConfigWriterHandler {

	private String newline = "\n";

	private final Config comparisonConfig = ConfigUtils.createConfig() ;

	private final Verbosity verbosity;
	
	ConfigWriterHandlerImplV2( Verbosity verbosity ) {
		this.verbosity = verbosity ;
	}

	private void writeModule(
			final BufferedWriter writer,
			final String indent,
			final String moduleTag,
			final String moduleNameAtt,
			final String moduleName,
			final ConfigGroup module) {
		Map<String, String> params = module.getParams();
		Map<String, String> comments = module.getComments();

		try {
//			writer.write( this.newline );
			writer.write( indent );
			writer.write("\t<"+moduleTag);
			writer.write(" "+moduleNameAtt+"=\"" + moduleName + "\" >");
			writer.write( this.newline );
			
//			boolean lastHadComment = false;

			for (Map.Entry<String, String> entry : params.entrySet()) {
				
				final String actual = entry.getValue();
				if ( verbosity==Verbosity.minimal ) {
					final String defaultValue = comparisonConfig.findParam(moduleName, entry.getKey());
					if ( actual.equals( defaultValue ) ) {
						continue ;
					}
//					if ( actual==null && defaultValue.equals("null") ) {
//						continue ;
//					}
				}
				
				if (comments.get(entry.getKey()) != null) {
//					writer.write( this.newline );
					writer.write( indent );
					writer.write( "\t\t<!-- " + comments.get(entry.getKey()) + " -->");
					writer.write( this.newline );
//					lastHadComment = true;
//				} else {
//					if (lastHadComment) {
//						writer.write( this.newline );
//					}
//					lastHadComment = false;
				}
				writer.write( indent );
				writer.write("\t\t<"+PARAMETER+" name=\"" + entry.getKey() + "\" value=\"" + actual + "\" />");
				writer.write( this.newline );
			}

			for ( Entry<String, ? extends Collection<? extends ConfigGroup>> entry : module.getParameterSets().entrySet() ) {
				for ( ConfigGroup pSet : entry.getValue() ) {
					// TODO: write comments only for the first parameter set of a given type?
					writeModule(
							writer,
							indent+"\t",
							PARAMETER_SET,
							TYPE,
							entry.getKey(),
							pSet );
				}
			}

			writer.write( indent );
			writer.write("\t</"+moduleTag+">");
			writer.write( this.newline );

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void startConfig(
			final Config config,
			final BufferedWriter out) {
		try {
			out.write("<"+CONFIG+">");
			out.write( this.newline );
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		
	}

	@Override
	public void endConfig(
			final BufferedWriter out) {
		try {
			out.write( this.newline );
			out.write("</"+CONFIG+">");
			out.write( this.newline );
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}	
	}

	@Override
	public void writeModule(
			final ConfigGroup module,
			final BufferedWriter out) {
		if ( ! (module instanceof ChangeLegModeConfigGroup) ) {
			// yyyy special case to provide error message; may be removed eventually.  kai, may'16
			
			writeModule(
				out,
				"",
				MODULE,
				NAME,
				module.getName(),
				module );
		}
	}

	@Override
	public void writeSeparator(final BufferedWriter out) {
		try {
//			out.write( this.newline );
			out.write("<!-- ====================================================================== -->");
			out.write( this.newline );
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}	
	}


	@Override
	public String setNewline(final String newline) {
		String former = this.newline;
		this.newline  = newline;
		return former;
	}

}


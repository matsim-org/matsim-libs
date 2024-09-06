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

import org.apache.logging.log4j.LogManager;
import org.matsim.core.config.ConfigWriter.Verbosity;
import org.matsim.core.config.groups.ChangeLegModeConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ScoringParameterSet;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.matsim.core.config.ConfigV2XmlNames.CONFIG;
import static org.matsim.core.config.ConfigV2XmlNames.MODULE;
import static org.matsim.core.config.ConfigV2XmlNames.NAME;
import static org.matsim.core.config.ConfigV2XmlNames.PARAMETER;
import static org.matsim.core.config.ConfigV2XmlNames.PARAMETER_SET;
import static org.matsim.core.config.ConfigV2XmlNames.TYPE;

/**
 * @author thibautd
 */
class ConfigWriterHandlerImplV2 extends ConfigWriterHandler {
	// yy I introduced "verbosity" to also write a reduced config.  In the end, this became so complicated
	// that it would probably have been better to just separate the config writers for the two execution
	// paths.  If someone feels like doing that, please go ahead.  kai, jun'18

	private String newline = "\n";

	/**
	 * So we can write only what deviates from the default.
	 * Implementation of this functionality unfortunately became quite messy.
	 * kai, may'18
	 */
	private final Verbosity verbosity;

	private final Set<String> commentsAlreadyWritten = new HashSet<>() ;

	ConfigWriterHandlerImplV2( Verbosity verbosity ) {
		this.verbosity = verbosity ;
	}

	private void writeModule(
			final BufferedWriter writer,
			final String indent,
			final String moduleTag,
			final String moduleNameAtt,
			final String moduleName,
			final ConfigGroup module,
			final ConfigGroup comparisonModule ) {
		Map<String, String> params = module.getParams();
		Map<String, String> comments = module.getComments();

		try {

			// first write the regular config entries (key,value pairs)
			boolean headerHasBeenWritten = writeRegularEntries(writer, indent, moduleTag, moduleNameAtt, moduleName, comparisonModule, params, comments);

			// can't say what this is for:
			if ( moduleName.equals("thisAintNoFlat") ) {
				LogManager.getLogger(this.getClass()).warn("here") ;
			}

			// then process the parameter sets (which will recursively call the current method again):
			headerHasBeenWritten = processParameterSets(writer, indent, moduleTag, moduleNameAtt, moduleName, module, comparisonModule, headerHasBeenWritten);

			if ( headerHasBeenWritten ) {
				writer.write(indent);
				writer.write("\t</" + moduleTag + ">");
				writer.write(this.newline);
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Boolean processParameterSets(BufferedWriter writer, String indent, String moduleTag, String moduleNameAtt, String moduleName,
							 ConfigGroup module, ConfigGroup comparisonModule, Boolean headerHasBeenWritten) throws IOException {
		for ( Entry<String, ? extends Collection<? extends ConfigGroup>> entry : module.getParameterSets().entrySet() ) {
			Collection<? extends ConfigGroup> comparisonSets = new ArrayList<>() ;
			if ( comparisonModule != null ) {
				comparisonSets = comparisonModule.getParameterSets(entry.getKey());
			}
			for ( ConfigGroup pSet : entry.getValue() ) {
				ConfigGroup comparisonPSet = null ;
				for ( ConfigGroup cg : comparisonSets ) {
					if ( sameType( pSet, cg ) ) {
						comparisonPSet = cg ;
						break ;
					}
				}
//				if ( comparisonPSet==null && !comparisonSets.isEmpty() ) {
//					// (e.g. activity type, or mode defined in config which is not in default)
//					comparisonPSet = comparisonSets.iterator().next() ;   // just an arbitrary one
//				}
				if ( verbosity== Verbosity.minimal && comparisonPSet==null ) {
					if ( pSet instanceof ScoringParameterSet) {
						comparisonPSet = ((ScoringConfigGroup) comparisonModule).getOrCreateScoringParameters(((ScoringParameterSet) pSet).getSubpopulation());
					} else if ( pSet instanceof ModeParams ) {
						comparisonPSet = ((ScoringParameterSet) comparisonModule).getOrCreateModeParams(((ModeParams) pSet).getMode());
					} else if ( pSet instanceof ActivityParams ) {
						comparisonPSet = ((ScoringParameterSet) comparisonModule).getOrCreateActivityParams(((ActivityParams) pSet).getActivityType());
					} else if ( pSet instanceof RoutingConfigGroup.TeleportedModeParams ) {
						comparisonPSet = ((RoutingConfigGroup) comparisonModule).getOrCreateModeRoutingParams(((RoutingConfigGroup.TeleportedModeParams) pSet).getMode() ) ;
					} else {
						try {
							comparisonPSet = pSet.getClass().newInstance();
						} catch (InstantiationException | IllegalAccessException e) {
//								e.printStackTrace();
							// this happens when pSet is not a parameter set, but the config group itself, _and_ it is a non-typed config group.
							// Then we don't have a default constructor.  Which is why we then try a plain config group.  kai, jun'18
							comparisonPSet = new ConfigGroup(pSet.getName()) ;
						}
					}
				}
//					LogManager.getLogger(this.getClass()).warn( "comparisonPSet=" + comparisonPSet ) ;
				// TODO: write comments only for the first parameter set of a given type?
				// I think that that is done now.  kai, jun'18
				if ( !headerHasBeenWritten ) {
					headerHasBeenWritten = true ;
					writeHeader(writer, indent, moduleTag, moduleNameAtt, moduleName, newline);
				}
				writeModule(writer, indent+"\t", PARAMETER_SET, TYPE, entry.getKey(), pSet, comparisonPSet );
			}
		}
		return headerHasBeenWritten;
	}

	private Boolean writeRegularEntries(BufferedWriter writer, String indent, String moduleTag, String moduleNameAtt,
							String moduleName, ConfigGroup comparisonModule, Map<String, String> params,
							Map<String, String> comments) throws IOException {
		boolean headerHasBeenWritten = false ;
		for (Entry<String, String> entry : params.entrySet()) {

			final String actual = entry.getValue();
			if ( verbosity== Verbosity.minimal ) {
				if ( comparisonModule!=null ) {
					String defaultValue = comparisonModule.getParams().get( entry.getKey() ) ;
					// exclude some cases manually for the time being (setting the default value to null means that
					// the actual entry will be written to file):
					switch( entry.getKey() ) {
						case ActivityParams.TYPICAL_DURATION:
							defaultValue = null ;
							break ;
						case ModeParams.MODE:
//						case ModeRoutingParams.MODE: // same string value!
							defaultValue = null ;
							break ;
						case ActivityParams.ACTIVITY_TYPE:
							defaultValue = null ;
							break ;
					}
					if (actual.equals(defaultValue)) {
						continue;
					}
//					if ( actual==null && defaultValue.equals("null") ) {
//						continue ;
//					}
				}
			}

			if ( !headerHasBeenWritten ) {
				headerHasBeenWritten = true ;
				writeHeader(writer, indent, moduleTag, moduleNameAtt, moduleName, newline);
			}

			String key = entry.getKey() + "." + moduleName ;
			if (comments.get( entry.getKey() ) != null && !commentsAlreadyWritten.contains( key )) {
				commentsAlreadyWritten.add( key ) ;

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
		return headerHasBeenWritten;
	}

	private static void writeHeader(BufferedWriter writer, String indent, String moduleTag, String moduleNameAtt, String moduleName, String newline) throws IOException {
		//			writer.write( this.newline );
		writer.write( indent );
		writer.write("\t<"+moduleTag);
		writer.write(" "+moduleNameAtt+"=\"" + moduleName + "\" >");
		writer.write( newline );
	}

	private static boolean sameType(ConfigGroup pSet, ConfigGroup cg) {
		if ( ! ( pSet.getName().equals( cg.getName() ) ) ) {
			return false;
		}
		if ( pSet instanceof RoutingConfigGroup.TeleportedModeParams ) {
			// (these are the "teleportedRouteParameters" in config.xml)
			if ( ((RoutingConfigGroup.TeleportedModeParams)pSet).getMode().equals( ((RoutingConfigGroup.TeleportedModeParams)cg).getMode() ) ) {
				return true ;
			}
		}
		if ( pSet instanceof ScoringParameterSet ) {
			return true ;
		}
		if ( pSet instanceof ModeParams ) {
			if ( ((ModeParams)pSet).getMode().equals( ((ModeParams)cg).getMode() ) ) {
				return true ;
			}
		}
		if ( pSet instanceof ActivityParams ) {
			if ( ((ActivityParams)pSet).getActivityType().equals( ((ActivityParams)cg).getActivityType() ) ) {
				return true ;
			}
		}
		if ( pSet instanceof StrategySettings ) {
			return true ;
			// yy this will not work since there is no corresponding default entry!  kai, may'18
		}
		return false ;
	}

	@Override
	 void startConfig(
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
	 void endConfig(
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
	 void writeModule(
			final ConfigGroup module,
			final BufferedWriter out) {
		if ( ! (module instanceof ChangeLegModeConfigGroup) ) {
			// yyyy special case to provide error message; may be removed eventually.  kai, may'16


			ConfigGroup comparisonConfig = null ;
			if ( verbosity==Verbosity.minimal) {
				comparisonConfig = ConfigUtils.createConfig().getModules().get(module.getName());
				// preference to generate this here multiple times to avoid having it as a field. kai, may'18
			}

			writeModule(
					out,
					"",
					MODULE,
					NAME,
					module.getName(),
					module,
					comparisonConfig
			);
		}
	}

	@Override
	 void writeSeparator(final BufferedWriter out) {
//		try {
////			out.write( this.newline );
//			out.write("<!-- ====================================================================== -->");
//			out.write( this.newline );
//
//		} catch (IOException e) {
//			throw new UncheckedIOException(e);
//		}

		// this looks ugly in the reduced config, thus disabling them for the time being.  kai, jun'18
	}


	@Override
	 String setNewline(final String newline) {
		String former = this.newline;
		this.newline  = newline;
		return former;
	}

}


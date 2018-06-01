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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigWriter.Verbosity;
import org.matsim.core.config.groups.ChangeLegModeConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.utils.io.UncheckedIOException;

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
			boolean headerHasBeenWritten=false ;
			
//			boolean lastHadComment = false;

			// first write the regular config entries (key,value pairs)
			for (Map.Entry<String, String> entry : params.entrySet()) {
				
				final String actual = entry.getValue();
				if ( verbosity==Verbosity.minimal ) {
					if ( comparisonModule!=null ) {
						final String defaultValue = comparisonModule.getParams().get( entry.getKey() ) ;
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
			
			if ( moduleName.equals("thisAintNoFlat") ) {
				Logger.getLogger(this.getClass()).warn("here") ;
			}

			// then write the parameter sets:
			for ( Entry<String, ? extends Collection<? extends ConfigGroup>> entry : module.getParameterSets().entrySet() ) {
				Collection<? extends ConfigGroup> comparisonSets = new ArrayList<>() ;
				if ( comparisonModule != null ) {
					comparisonSets = comparisonModule.getParameterSets(entry.getKey());
				};
//				for ( ConfigGroup set : comparisonSets ) {
//					Logger.getLogger(this.getClass()).warn( set ) ;
//				}
				for ( ConfigGroup pSet : entry.getValue() ) {
//					Logger.getLogger(this.getClass()).warn( comparisonSets ) ;
					ConfigGroup comparisonPSet = null ;
					for ( ConfigGroup cg : comparisonSets ) {
						if ( sameType( pSet, cg ) ) {
							comparisonPSet = cg ;
							break ;
						}
					}
					if ( comparisonPSet==null ) {
						if ( pSet instanceof ScoringParameterSet ) {
							comparisonPSet = ((PlanCalcScoreConfigGroup) comparisonModule).getOrCreateScoringParameters(((ScoringParameterSet) pSet).getSubpopulation());
						} else {
							try {
								comparisonPSet = pSet.getClass().newInstance();
							} catch (InstantiationException | IllegalAccessException e) {
//								e.printStackTrace();
								// this happens when pSet is not a parameter set, but the config group itself, _and_ it is a non-typed config group.
								// Since then we don't have a default constructor.  :-(  kai, jun'18
							}
							comparisonPSet = new ConfigGroup(pSet.getName()) ;
						}
					}
//					Logger.getLogger(this.getClass()).warn( "comparisonPSet=" + comparisonPSet ) ;
					// TODO: write comments only for the first parameter set of a given type?
					if ( !headerHasBeenWritten ) {
						headerHasBeenWritten = true ;
						writeHeader(writer, indent, moduleTag, moduleNameAtt, moduleName, newline);
					}
					writeModule(writer, indent+"\t", PARAMETER_SET, TYPE, entry.getKey(), pSet, comparisonPSet );
				}
			}

			if ( headerHasBeenWritten ) {
				writer.write(indent);
				writer.write("\t</" + moduleTag + ">");
				writer.write(this.newline);
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
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
		if ( pSet instanceof ModeRoutingParams ) {
			// (these are the "teleportedRouteParameters" in config.xml)
			if ( ((ModeRoutingParams)pSet).getMode().equals( ((ModeRoutingParams)cg).getMode() ) ) {
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
			
			final ConfigGroup comparisonConfig = ConfigUtils.createConfig().getModules().get(module.getName());
			// preference to generate this here multiple times to avoid having it as a field. kai, may'18
			
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


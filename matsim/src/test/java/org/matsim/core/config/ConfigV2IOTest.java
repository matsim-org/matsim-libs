/* *********************************************************************** *
 * project: org.matsim.*
 * NonFlatConfigIOTest.java
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

import java.util.Collection;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.utils.misc.MatsimTestUtils;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigReaderMatsimV2;

/**
 * @author thibautd
 */
public class ConfigV2IOTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testInputSameAsOutput() {
		final String file = utils.getOutputDirectory()+"/config.xml";

		final Config outConfig = createTestConfig();

		new ConfigWriter( outConfig ).writeFileV2( file );

		final Config inConfig = ConfigUtils.createConfig();
		new ConfigReaderMatsimV2( inConfig ).parse( file );

		assertTheSame( outConfig , inConfig );
	}

	private void assertTheSame(
			final Config outConfig,
			final Config inConfig) {
		Assert.assertEquals(
				"names of modules differ!",
				outConfig.getModules().keySet(),
				inConfig.getModules().keySet() );

		for ( String name : outConfig.getModules().keySet() ) {
			assertTheSame(
					outConfig.getModule( name ),
					inConfig.getModule( name ) );
		}
	}

	private void assertTheSame(
			final ConfigGroup outModule,
			final ConfigGroup inModule) {
		Assert.assertEquals(
				"wrong module class",
				outModule.getClass(),
				inModule.getClass() );

		Assert.assertEquals(
				"different parameters",
				outModule.getParams(),
				inModule.getParams() );

		Assert.assertEquals(
				"different parameterset types",
				outModule.getParameterSets().keySet(),
				inModule.getParameterSets().keySet() );

		for ( String type : outModule.getParameterSets().keySet() ) {
			final Collection<? extends ConfigGroup> outSets = outModule.getParameterSets( type );
			final Collection<? extends ConfigGroup> inSets = inModule.getParameterSets( type );

			Assert.assertEquals(
					"different number of sets for type "+type,
					outSets.size(),
					inSets.size() );

			final Iterator<? extends ConfigGroup> outIter = outSets.iterator();
			final Iterator<? extends ConfigGroup> inIter = inSets.iterator();

			while ( outIter.hasNext() ) {
				assertTheSame(
						outIter.next(),
						inIter.next() );
			}
		}
	}

	private Config createTestConfig() {
		final Config c = ConfigUtils.createConfig();

		final ConfigGroup module = new ConfigGroup( "thisAintNoFlat" );
		module.addParam( "someParam" , "someValue" );
		module.addParam( "anotherParam" , "anotherValue" );

		final ConfigGroup paramSet1 = module.createParameterSet( "oneType" );
		module.addParameterSet( paramSet1 );
		paramSet1.addParam( "something" , "gloups" );

		final ConfigGroup paramSet2 = module.createParameterSet( "oneType" );
		module.addParameterSet( paramSet2 );
		paramSet2.addParam( "something" , "gloups" );
		paramSet2.addParam( "something_else" , "glips" );

		final ConfigGroup paramSet3 = module.createParameterSet( "anotherType" );
		module.addParameterSet( paramSet3 );
		paramSet3.addParam( "niark" , "niourk" );

		c.addModule( module );

		return c;
	}
}


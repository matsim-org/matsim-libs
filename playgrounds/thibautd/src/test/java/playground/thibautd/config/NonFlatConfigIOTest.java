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
package playground.thibautd.config;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.Module;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class NonFlatConfigIOTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testInputSameAsOutput() {
		final String file = utils.getOutputDirectory()+"/config.xml";

		final Config outConfig = createTestConfig();

		new NonFlatConfigWriter( outConfig ).write( file );

		final Config inConfig = ConfigUtils.createConfig();
		// for the moment, non flat modules are non-standard,
		// so one has to set them explicitly.
		inConfig.addModule( new NonFlatModule( "thisAintNoFlat" ) );
		new NonFlatConfigReader( inConfig ).parse( file );

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
			final Module outModule,
			final Module inModule) {
		Assert.assertEquals(
				"wrong module class",
				outModule.getClass(),
				inModule.getClass() );

		Assert.assertEquals(
				"different parameters",
				outModule.getParams(),
				inModule.getParams() );

		if ( outModule instanceof NonFlatModule ) {
			final NonFlatModule outNonFlat = (NonFlatModule) outModule;
			final NonFlatModule inNonFlat = (NonFlatModule) inModule;
			Assert.assertEquals(
					"different parameterset types",
					outNonFlat.getParameterSets().keySet(),
					inNonFlat.getParameterSets().keySet() );

			for ( String type : outNonFlat.getParameterSets().keySet() ) {
				final Collection<Module> outSets = outNonFlat.getParameterSets( type );
				final Collection<Module> inSets = inNonFlat.getParameterSets( type );

				Assert.assertEquals(
						"different number of sets for type "+type,
						outSets.size(),
						inSets.size() );

				final Iterator<Module> outIter = outSets.iterator();
				final Iterator<Module> inIter = inSets.iterator();

				while ( outIter.hasNext() ) {
					assertTheSame(
							outIter.next(),
							inIter.next() );
				}
			}
		}
	}

	private Config createTestConfig() {
		final Config c = ConfigUtils.createConfig();

		final NonFlatModule module = new NonFlatModule( "thisAintNoFlat" );
		module.addParam( "someParam" , "someValue" );
		module.addParam( "anotherParam" , "anotherValue" );

		final Module paramSet1 = module.createAndAddParameterSet( "oneType" );
		paramSet1.addParam( "something" , "gloups" );

		final Module paramSet2 = module.createAndAddParameterSet( "oneType" );
		paramSet2.addParam( "something" , "gloups" );
		paramSet2.addParam( "something_else" , "glips" );

		final Module paramSet3 = module.createAndAddParameterSet( "anotherType" );
		paramSet3.addParam( "niark" , "niourk" );

		c.addModule( module );

		return c;
	}
}


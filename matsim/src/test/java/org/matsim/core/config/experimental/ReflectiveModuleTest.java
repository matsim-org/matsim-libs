/* *********************************************************************** *
 * project: org.matsim.*
 * ReflectiveModuleTest.java
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
package org.matsim.core.config.experimental;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.Module;
import org.matsim.core.config.experimental.ReflectiveModule.InconsistentModuleException;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class ReflectiveModuleTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testDumpAndRead() {
		final MyModule dumpedModule = new MyModule();
		dumpedModule.setDoubleField( 1000 );
		dumpedModule.setIdField( new IdImpl( 123 ) );
		dumpedModule.setCoordField( new CoordImpl( 265 , 463 ) );
		dumpedModule.setTestEnumField( MyEnum.VALUE2 );

		final Config dumpedConfig = new Config();
		dumpedConfig.addModule( dumpedModule );

		final String fileName = utils.getOutputDirectory() + "/dump.xml";

		new ConfigWriter( dumpedConfig ).write( fileName );
		final Config readConfig = ConfigUtils.loadConfig( fileName );
		final MyModule readModule = new MyModule();
		// as a side effect, this loads the information
		readConfig.addModule( readModule );

		assertSame( dumpedModule , readModule );
	}

	@Test
	public void testFailOnConstructingOrphanSetter() {
		try {
			new ReflectiveModule( "name" ) {
				@StringSetter( "setterWithoutGetter" )
				public void setStuff(String s) {}
			};
			// should not get here because of exception
			Assert.fail( "no exception when orphan setter" );
		}
		catch (InconsistentModuleException e) {
			// gulp! swallow exception
		}
	}

	@Test
	public void testFailOnConstructingOrphanGetter() {
		try {
			new ReflectiveModule( "name" ) {
				@StringGetter( "getterWithoutSetter" )
				public Coord getStuff() { return null;}
			};
			// should not get here because of exception
			Assert.fail( "no exception when orphan getter" );
		}
		catch (InconsistentModuleException e) {
			// gulp! swallow exception
		}
	}

	@Test
	public void testFailOnConstructingInvalidSetter() {
		try {
			new ReflectiveModule( "name" ) {
				// no arg: no good
				@StringSetter( "field" )
				public Object setStuff() { return null;}

				@StringGetter( "field" )
				public Object getStuff() { return null;}
			};
			// should not get here because of exception
			Assert.fail( "no exception when no arg on setter" );
		}
		catch (InconsistentModuleException e) {
			// gulp! swallow exception
		}

		try {
			new ReflectiveModule( "name" ) {
				// bad arg type: no good
				@StringSetter( "field" )
				public Object setStuff(Coord stuff) { return null;}

				@StringGetter( "field" )
				public Object getStuff() { return null;}
			};
			// should not get here because of exception
			Assert.fail( "no exception when bad type on setter" );
		}
		catch (InconsistentModuleException e) {
			// gulp! swallow exception
		}
	}

	@Test
	public void testFailOnConstructingInvalidGetter() {
		try {
			new ReflectiveModule( "name" ) {
				@StringSetter( "field" )
				public Object setStuff(String s) { return null;}

				@StringGetter( "field" )
				public void getStuff() { }
			};
			// should not get here because of exception
			Assert.fail( "no exception when void returning getter" );
		}
		catch (InconsistentModuleException e) {
			// gulp! swallow exception
		}

		try {
			new ReflectiveModule( "name" ) {
				@StringSetter( "field" )
				public Object setStuff(String stuff) { return null;}

				@StringGetter( "field" )
				public Object getStuff(Object someArg) { return null;}
			};
			// should not get here because of exception
			Assert.fail( "no exception when args on getter" );
		}
		catch (InconsistentModuleException e) {
			// gulp! swallow exception
		}
	}

	@Test
	public void testFailOnConstructingSeveralGetters() {
		try {
			new ReflectiveModule( "name" ) {
				@StringSetter( "field" )
				public Object setStuff(String s) { return null;}

				@StringGetter( "field" )
				public void getStuff() { }

				@StringGetter( "field" )
				public void getStuff2() { }
			};
			// should not get here because of exception
			Assert.fail( "no exception when 2 getters for one field" );
		}
		catch (InconsistentModuleException e) {
			// gulp! swallow exception
		}
	}

	@Test
	public void testFailOnConstructingSeveralSetters() {
		try {
			new ReflectiveModule( "name" ) {
				@StringSetter( "field" )
				public Object setStuff(String s) { return null;}

				@StringSetter( "field" )
				public void setStuff(Object s) {}

				@StringGetter( "field" )
				public void getStuff() { }
			};
			// should not get here because of exception
			Assert.fail( "no exception when 2 getters for one field" );
		}
		catch (InconsistentModuleException e) {
			// gulp! swallow exception
		}
	}

	@Test
	public void testBehaviorWhenAcceptingUnknownParameters() {
		final Module testee =
			new ReflectiveModule( "name" , true ) {
				@StringSetter( "field" )
				public void setStuff(String s) {}

				@StringGetter( "field" )
				public Object getStuff() { return null; }
			};

		final String param = "my unknown param";
		final String value = "my val";
		testee.addParam( param , value );
		Assert.assertEquals(
				"unexpected stored value",
				value,
				testee.getValue( param ) );
	}

	@Test
	public void testBehaviorWhenRejectingUnknownParameters() {
		final Module testee =
			new ReflectiveModule( "name" , false ) {
				@StringSetter( "field" )
				public void setStuff(String s) {}

				@StringGetter( "field" )
				public Object getStuff() { return null; }
			};

		final String param = "my unknown param";
		final String value = "my val";
		boolean gotExceptionAtAdd = false;
		boolean gotExceptionAtGet = false;
		try {
			testee.addParam( param , value );
		}
		catch ( IllegalArgumentException e ) {
			gotExceptionAtAdd = true;
		}

		try {
			testee.getValue( param );
		}
		catch ( IllegalArgumentException e ) {
			gotExceptionAtGet = true;
		}

		Assert.assertTrue(
				"did not get exception when adding unkown param",
				gotExceptionAtAdd );
		Assert.assertTrue(
				"did not get exception when getting unkown param",
				gotExceptionAtGet );
	}

	@Test
	public void testExceptionRedirection() {
		final RuntimeException thrown = new RuntimeException();
		final Module m = new ReflectiveModule( "name" ) {
				@StringSetter( "field" )
				public void setStuff(String s) {
					throw thrown;
				}

				@StringGetter( "field" )
				public String getStuff() {
					throw thrown;
				}
			};

		try {
			m.addParam( "field" , "value" );
			Assert.fail( "no transmition of exception!" );
		}
		catch (Exception e) {
			Assert.assertSame(
					"unchecked exception was not transmitted correctly",
					thrown,
					e );
		}

		try {
			m.getValue( "field" );
			Assert.fail( "no transmition of exception!" );
		}
		catch (Exception e) {
			Assert.assertSame(
					"unchecked exception was not transmitted correctly",
					thrown,
					e );
		}
	}

	private static void assertSame(
			final MyModule dumpedModule,
			final MyModule readModule) {
		Assert.assertEquals(
				"incompatible double fields",
				dumpedModule.getDoubleField(),
				readModule.getDoubleField(),
				MatsimTestUtils.EPSILON);

		Assert.assertEquals(
				"incompatible id fields",
				dumpedModule.getIdField(),
				readModule.getIdField());

		Assert.assertEquals(
				"incompatible coord fields",
				dumpedModule.getCoordField(),
				readModule.getCoordField());

		Assert.assertEquals(
				"incompatible enum fields",
				dumpedModule.getTestEnumField(),
				readModule.getTestEnumField());
	}
}


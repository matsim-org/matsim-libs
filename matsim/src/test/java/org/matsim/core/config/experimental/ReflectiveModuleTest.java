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
import org.matsim.api.core.v01.Id;
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
		final TestModule dumpedModule = new TestModule();
		dumpedModule.setDoubleField( 1000 );
		dumpedModule.setIdField( new IdImpl( 123 ) );
		dumpedModule.setCoordField( new CoordImpl( 265 , 463 ) );
		dumpedModule.setTestEnumField( TestEnum.VALUE2 );

		final Config dumpedConfig = new Config();
		dumpedConfig.addModule( dumpedModule );

		final String fileName = utils.getOutputDirectory() + "/dump.xml";

		new ConfigWriter( dumpedConfig ).write( fileName );
		final Config readConfig = ConfigUtils.loadConfig( fileName );
		final TestModule readModule = new TestModule();
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

	private static void assertSame(
			final TestModule dumpedModule,
			final TestModule readModule) {
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

	// TODO: move to tutorial?
	public static class TestModule extends ReflectiveModule {
		public static final String GROUP_NAME = "testModule";

		// TODO: test for ALL primitive types
		private double doubleField = Double.NaN;

		// Object fields:
		// Id: string representation is toString
		private Id idField = null;
		// Coord: some conversion needed
		private Coord coordField = null;
		// enum: handled especially
		private TestEnum enumField = null;

		public TestModule() {
			super( GROUP_NAME );
		}

		// /////////////////////////////////////////////////////////////////////
		// double field
		@StringGetter( "doubleField" )
		public double getDoubleField() {
			return this.doubleField;
		}

		// there should be no restriction on return type of
		// setters
		@StringSetter( "doubleField" )
		public double setDoubleField(double doubleField) {
			final double old = this.doubleField;
			this.doubleField = doubleField;
			return old;
		}
 
		// /////////////////////////////////////////////////////////////////////
		// id field
		/**
		 * string representation of Id is result of
		 * toString: just annotate getter
		 */
		@StringGetter( "idField" )
		public Id getIdField() {
			return this.idField;
		}

		public void setIdField(Id idField) {
			this.idField = idField;
		}

		/**
		 * We need to do the conversion from string to Id
		 * ourselves. We do not want the user to access that:
		 * make private.
		 */
		@StringSetter( "idField" )
		private void setIdField(String s) {
			this.idField = new IdImpl( s );
		}

		// /////////////////////////////////////////////////////////////////////
		// coord field
		public Coord getCoordField() {
			return this.coordField;
		}

		public void setCoordField(Coord coordField) {
			this.coordField = coordField;
		}

		// we have to convert both ways here
		@StringGetter( "coordField" )
		private String getCoordFieldString() {
			return this.coordField.getX()+","+this.coordField.getY();
		}

		@StringSetter( "coordField" )
		private void setCoordField(String coordField) {
			final String[] coords = coordField.split( "," );
			if ( coords.length != 2 ) throw new IllegalArgumentException( coordField );

			this.coordField = new CoordImpl(
					Double.parseDouble( coords[ 0 ] ),
					Double.parseDouble( coords[ 1 ] ) );
		}

		// /////////////////////////////////////////////////////////////////////
		// enum
		@StringGetter( "enumField" )
		public TestEnum getTestEnumField() {
			return this.enumField;
		}

		@StringSetter( "enumField" )
		public void setTestEnumField(final TestEnum enumField) {
			this.enumField = enumField;
		}
	}

	private static enum TestEnum {
		VALUE1, VALUE2;
	}
}


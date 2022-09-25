/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */
package org.matsim.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ReflectiveConfigGroup.InconsistentModuleException;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author thibautd
 */
public class ReflectiveConfigGroupTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testDumpAndRead() {
		final MyModule dumpedModule = new MyModule();
		dumpedModule.setDoubleField(1000);
		dumpedModule.setIdField(Id.create(123, Link.class));
		dumpedModule.setCoordField(new Coord(265, 463));
		dumpedModule.setTestEnumField(MyEnum.VALUE2);
		dumpedModule.floatField = 123;
		dumpedModule.longField = 234;
		dumpedModule.intField = 345;
		dumpedModule.shortField = 456;
		dumpedModule.charField = 'z';
		dumpedModule.byteField = 78;
		dumpedModule.booleanField = true;
		dumpedModule.setField = Set.of("a", "b", "c");
		dumpedModule.listField = List.of("1", "2", "3");

		final Config dumpedConfig = new Config();
		dumpedConfig.addModule(dumpedModule);

		final String fileName = utils.getOutputDirectory() + "/dump.xml";

		new ConfigWriter(dumpedConfig).write(fileName);
		final Config readConfig = ConfigUtils.loadConfig(fileName);
		final MyModule readModule = new MyModule();
		// as a side effect, this loads the information
		readConfig.addModule(readModule);

		assertThat(readModule).usingRecursiveComparison().isEqualTo(dumpedModule);
	}

	@Test
	public void testDumpAndReadNulls() {
		final MyModule dumpedModule = new MyModule();
		dumpedModule.setIdField(null);
		dumpedModule.setCoordField(null);
		dumpedModule.setTestEnumField(null);

		final Config dumpedConfig = new Config();
		dumpedConfig.addModule(dumpedModule);

		final String fileName = utils.getOutputDirectory() + "/dump.xml";

		new ConfigWriter(dumpedConfig).write(fileName);
		final Config readConfig = ConfigUtils.loadConfig(fileName);
		final MyModule readModule = new MyModule();
		// as a side effect, this loads the information
		readConfig.addModule(readModule);

		assertThat(readModule).usingRecursiveComparison().isEqualTo(dumpedModule);
	}

	@Test
	public void testComments() {
		var expectedComments = Map.of("floatField", "float",//
				"longField", "long",//
				"intField", "int",//
				"shortField", "short",//
				"charField", "char",//
				"byteField", "byte",//
				"booleanField", "boolean",//
				"enumField", "Possible values: VALUE1,VALUE2",//
				"setField", "set");

		assertThat(new MyModule().getComments()).isEqualTo(expectedComments);
	}

	@Test
	public void testFailOnConstructingOrphanSetter() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@StringSetter("setterWithoutGetter")
			public void setStuff(String s) {
			}
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	public void testFailOnConstructingOrphanGetter() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@StringGetter("setterWithoutGetter")
			public Coord getStuff() {
				return null;
			}
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	public void testFailOnConstructingInvalidSetter() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			// no arg: no good
			@StringSetter("field")
			public Object setStuff() {
				return null;
			}

			@StringGetter("field")
			public Object getStuff() {
				return null;
			}
		}).isInstanceOf(InconsistentModuleException.class);

		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			// bad arg type: no good
			@StringSetter("field")
			public Object setStuff(Coord stuff) {
				return null;
			}

			@StringGetter("field")
			public Object getStuff() {
				return null;
			}
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	public void testFailOnConstructingInvalidGetter() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@StringSetter("field")
			public Object setStuff(String s) {
				return null;
			}

			// void: not good
			@StringGetter("field")
			public void getStuff() {
			}
		}).isInstanceOf(InconsistentModuleException.class);

		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@StringSetter("field")
			public Object setStuff(String stuff) {
				return null;
			}

			// takes a parameter: not good
			@StringGetter("field")
			public Object getStuff(Object someArg) {
				return null;
			}
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	public void testFailOnConstructingSeveralGetters() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@StringSetter("field")
			public void setStuff(String s) {
			}

			@StringGetter("field")
			public Object getStuff() {
				return null;
			}

			@StringGetter("field")
			public Object getStuff2() {
				return null;
			}
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	public void testFailOnConstructingSeveralSetters() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@StringSetter("field")
			public void setStuff(String s) {
			}

			@StringSetter("field")
			public void setStuff(double s) {
			}

			@StringGetter("field")
			public Object getStuff() {
				return null;
			}
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	public void testFailOnConstructingSeveralParameters() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@Parameter
			double field;

			@Parameter("field")
			double stuff;
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	public void testFailOnMixingGettersSettersWithParameters() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@StringSetter("field")
			public void setStuff(double s) {
			}

			@StringGetter("field")
			public Object getStuff() {
				return null;
			}

			@Parameter("field")
			private double stuff;
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	public void testPreferCustomCommentToAutoGeneratedEnumComment() {
		var config = new ReflectiveConfigGroup("name") {
			@Comment("my comment")
			@Parameter("field")
			public MyEnum stuff;
		};
		assertThat(config.getComments()).isEqualTo(Map.of("field", "my comment"));
	}

	@Test
	public void testBehaviorWhenAcceptingUnknownParameters() {
		final ConfigGroup testee = new ReflectiveConfigGroup("name", true) {
			@StringSetter("field")
			public void setStuff(String s) {
			}

			@StringGetter("field")
			public Object getStuff() {
				return null;
			}
		};

		final String param = "my unknown param";
		final String value = "my val";
		testee.addParam(param, value);
		Assert.assertEquals("unexpected stored value", value, testee.getValue(param));
	}

	@Test
	public void testBehaviorWhenRejectingUnknownParameters() {
		final ConfigGroup testee = new ReflectiveConfigGroup("name", false) {
			@StringSetter("field")
			public void setStuff(String s) {
			}

			@StringGetter("field")
			public Object getStuff() {
				return null;
			}
		};

		final String param = "my unknown param";
		final String value = "my val";
		// throw exception when adding an unknown param
		assertThatThrownBy(() -> testee.addParam(param, value)).isInstanceOf(IllegalArgumentException.class);
		// throw exception when getting an unknown param
		assertThatThrownBy(() -> testee.getValue(param)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testExceptionRedirection() {
		final RuntimeException expectedException = new RuntimeException();
		final ConfigGroup m = new ReflectiveConfigGroup("name") {
			@StringSetter("field")
			public void setStuff(String s) {
				throw expectedException;
			}

			@StringGetter("field")
			public String getStuff() {
				throw expectedException;
			}
		};

		// the expected exception is propagated
		assertThatThrownBy(() -> m.addParam("field", "value")).isSameAs(expectedException);
		// the expected exception is propagated
		assertThatThrownBy(() -> m.getValue("field")).isSameAs(expectedException);
	}

	private enum MyEnum {
		VALUE1, VALUE2;
	}

	/**
	 * Demonstrate how to use ReflectiveModule to easily create typed config groups.
	 * Please do not modify this class: it is used from unit tests!
	 */
	private static class MyModule extends ReflectiveConfigGroup {
		public static final String GROUP_NAME = "testModule";

		private double doubleField;

		@Comment("float")
		@Parameter
		private float floatField;

		@Comment("long")
		@Parameter
		private long longField;

		@Comment("int")
		@Parameter
		private int intField;

		@Comment("short")
		@Parameter
		private short shortField;

		@Comment("char")
		@Parameter
		private char charField = ' ';

		@Comment("byte")
		@Parameter
		private byte byteField;

		@Comment("boolean")
		@Parameter
		private boolean booleanField;

		@Comment("set")
		@Parameter
		private Set<String> setField;

		//		@Comment("list")
		//		@Parameter
		//		private List<String> listField;

		// Object fields:
		// Id: string representation is toString
		private Id<Link> idField;
		// Coord: some conversion needed
		private Coord coordField;
		// enum: handled especially
		private MyEnum enumField;
		private List<String> listField;

		public MyModule() {
			super(GROUP_NAME);
		}

		@StringGetter("list")
		public List<String> getListField() {
			return listField;
		}

		@StringSetter("list")
		public void setListField(List<String> listField) {
			this.listField = listField;
		}

		// /////////////////////////////////////////////////////////////////////
		// primitive type field: standard getter and setter suffice
		@StringGetter("doubleField")
		public double getDoubleField() {
			return this.doubleField;
		}

		// there should be no restriction on return type of
		// setters
		@StringSetter("doubleField")
		public void setDoubleField(double doubleField) {
			this.doubleField = doubleField;
		}

		// /////////////////////////////////////////////////////////////////////
		// id field: need for a special setter, normal getter suffice

		/**
		 * string representation of Id is result of
		 * toString: just annotate getter
		 */
		@StringGetter("idField")
		public Id<Link> getIdField() {
			return this.idField;
		}

		public void setIdField(Id<Link> idField) {
			this.idField = idField;
		}

		/**
		 * We need to do the conversion from string to Id
		 * ourselves.
		 * the annotated setter can be private to avoid polluting the
		 * interface: the user just sees the "typed" setter.
		 */
		@StringSetter("idField")
		private void setIdFieldString(String s) {
			// Null handling needs to be done manually if conversion "by hand"
			this.idField = s == null ? null : Id.create(s, Link.class);
		}

		// /////////////////////////////////////////////////////////////////////
		// coord field: need for special getter and setter
		public Coord getCoordField() {
			return this.coordField;
		}

		public void setCoordField(Coord coordField) {
			this.coordField = coordField;
		}

		// we have to convert both ways here.
		// the annotated getter and setter can be private to avoid polluting the
		// interface: the user just sees the "typed" getter and setter.
		@StringGetter("coordField")
		private String getCoordFieldString() {
			// Null handling needs to be done manually if conversion "by hand"
			// Note that one *needs" to return a null pointer, not the "null"
			// String, which is reserved word.
			return this.coordField == null ? null : this.coordField.getX() + "," + this.coordField.getY();
		}

		@StringSetter("coordField")
		private void setCoordFieldString(String coordField) {
			if (coordField == null) {
				// Null handling needs to be done manually if conversion "by hand"
				this.coordField = null;
				return;
			}

			final String[] coords = coordField.split(",");
			if (coords.length != 2)
				throw new IllegalArgumentException(coordField);

			this.coordField = new Coord(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
		}

		// /////////////////////////////////////////////////////////////////////
		// enum: normal getter and setter suffice
		@StringGetter("enumField")
		public MyEnum getTestEnumField() {
			return this.enumField;
		}

		@StringSetter("enumField")
		public void setTestEnumField(final MyEnum enumField) {
			// no need to test for null: the parent class does it for us
			this.enumField = enumField;
		}
	}
}

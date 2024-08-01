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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ReflectiveConfigGroup.InconsistentModuleException;
import org.matsim.testcases.MatsimTestUtils;

import com.google.common.collect.ImmutableSet;

/**
 * @author thibautd
 */
public class ReflectiveConfigGroupTest {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testDumpAndRead() {
		MyModule dumpedModule = new MyModule();
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
		dumpedModule.localTimeField = LocalTime.of(23, 59, 59);
		dumpedModule.localDateField = LocalDate.of(2022, 12, 31);
		dumpedModule.localDateTimeField = LocalDateTime.of(2022, 12, 31, 23, 59, 59);
		dumpedModule.enumListField = List.of(MyEnum.VALUE1, MyEnum.VALUE2);
		dumpedModule.enumSetField = Set.of(MyEnum.VALUE2);
		dumpedModule.setField = ImmutableSet.of("a", "b", "c");
		dumpedModule.listField = List.of("1", "2", "3");
		dumpedModule.ints = List.of(1, 2, 3);
		assertEqualAfterDumpAndRead(dumpedModule);
	}

	@Test
	void testDumpAndReadNulls() {
		MyModule dumpedModule = new MyModule();
		assertEqualAfterDumpAndRead(dumpedModule);
	}

	@Test
	void testDumpAndReadEmptyCollections() {
		MyModule dumpedModule = new MyModule();
		dumpedModule.listField = List.of();
		dumpedModule.setField = ImmutableSet.of();
		dumpedModule.enumListField = List.of();
		dumpedModule.enumSetField = ImmutableSet.of();
		assertEqualAfterDumpAndRead(dumpedModule);
	}

	@Test
	void testDumpAndReadCollectionsWithExactlyOneEmptyString() {
		MyModule dumpedModule = new MyModule();

		//fail on list
		dumpedModule.listField = List.of("");
		dumpedModule.setField = null;
		assertThatThrownBy(() -> assertEqualAfterDumpAndRead(dumpedModule)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Collection [] contains blank elements. Only non-blank elements are supported.");

		//fail on set
		dumpedModule.listField = null;
		dumpedModule.setField = ImmutableSet.of("");
		assertThatThrownBy(() -> assertEqualAfterDumpAndRead(dumpedModule)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Collection [] contains blank elements. Only non-blank elements are supported.");
	}

	@Test
	void testDumpAndReadCollectionsIncludingEmptyString() {
		MyModule dumpedModule = new MyModule();

		//fail on list
		dumpedModule.listField = List.of("non-empty", "");
		dumpedModule.setField = ImmutableSet.of("non-empty");
		assertThatThrownBy(() -> assertEqualAfterDumpAndRead(dumpedModule)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Collection [non-empty, ] contains blank elements. Only non-blank elements are supported.");

		//fail on set
		dumpedModule.listField = List.of("non-empty");
		dumpedModule.setField = ImmutableSet.of("non-empty", "");
		assertThatThrownBy(() -> assertEqualAfterDumpAndRead(dumpedModule)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Collection [non-empty, ] contains blank elements. Only non-blank elements are supported.");
	}

	private void assertEqualAfterDumpAndRead(MyModule dumpedModule) {
		Config dumpedConfig = new Config();
		dumpedConfig.addModule(dumpedModule);

		String fileName = utils.getOutputDirectory() + "/dump.xml";

		new ConfigWriter(dumpedConfig).write(fileName);
		Config readConfig = ConfigUtils.loadConfig(fileName);
		MyModule readModule = new MyModule();
		// as a side effect, this loads the information
		readConfig.addModule(readModule);

		assertThat(readModule).usingRecursiveComparison().isEqualTo(dumpedModule);
	}

	@Test
	void testReadCollectionsIncludingEmptyString() {
		String fileName = utils.getInputDirectory() + "/config_with_blank_comma_separated_elements.xml";
		final Config readConfig = ConfigUtils.loadConfig(fileName);
		final MyModule readModule = new MyModule();
		// as a side effect, this loads the information
		assertThatThrownBy(() -> readConfig.addModule(readModule)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage(
						"String 'str1, , str2' contains comma-separated blank elements. Only non-blank elements are supported.");
	}

	@Test
	void testComments() {
		var expectedComments = new HashMap<>();
		expectedComments.put("floatField", "float");
		expectedComments.put("longField", "long");
		expectedComments.put("intField", "int");
		expectedComments.put("shortField", "short");
		expectedComments.put("charField", "char");
		expectedComments.put("byteField", "byte");
		expectedComments.put("booleanField", "boolean");
		expectedComments.put("localTimeField", "local time");
		expectedComments.put("localDateField", "local date");
		expectedComments.put("localDateTimeField", "local datetime");
		expectedComments.put("enumField", "Possible values: VALUE1,VALUE2");
		expectedComments.put("enumListField", "list of enum");
		expectedComments.put("enumSetField", "set of enum");
		expectedComments.put("setField", "set");
		expectedComments.put("ints", "list of ints");

		assertThat(new MyModule().getComments()).isEqualTo(expectedComments);
	}

	@Test
	void testAllowOnConstructingOrphanSetter() {
		// no exception
		new ReflectiveConfigGroup("name") {
			@StringSetter("setterWithoutGetter")
			public void setStuff(String s) {
			}
		};
	}

	@Test
	void testFailOnConstructingOrphanGetter() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@StringGetter("getterWithoutSetter")
			public Coord getStuff() {
				return null;
			}
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	void testFailOnConstructingInvalidSetter() {
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
	void testFailOnConstructingInvalidGetter() {
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
	void testFailOnConstructingSeveralGetters() {
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
	void testFailOnConstructingSeveralSetters() {
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
	void testFailOnConstructingSeveralParameters() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@Parameter
			double field;

			@Parameter("field")
			double stuff;
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	void testFailOnMixingGettersSettersWithParameters() {
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
	void testFailUnsupportedType_StringCollections() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@Parameter("field")
			private Collection<String> stuff;
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	void testFailUnsupportedType_NonStringList() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@Parameter("field")
			private List<Person> stuff;
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	void testFailUnsupportedType_StringHashSet() {
		assertThatThrownBy(() -> new ReflectiveConfigGroup("name") {
			@Parameter("field")
			private HashSet<String> stuff;
		}).isInstanceOf(InconsistentModuleException.class);
	}

	@Test
	void testPreferCustomCommentToAutoGeneratedEnumComment() {
		var config = new ReflectiveConfigGroup("name") {
			@Comment("my comment")
			@Parameter("field")
			public MyEnum stuff;
		};
		assertThat(config.getComments()).isEqualTo(Map.of("field", "my comment"));
	}

	@Test
	void testBehaviorWhenAcceptingUnknownParameters() {
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
		Assertions.assertEquals(value, testee.getValue(param), "unexpected stored value");
	}

	@Test
	void testBehaviorWhenRejectingUnknownParameters() {
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
	void testExceptionRedirection() {
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
		VALUE1, VALUE2
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

		@Comment("local time")
		@Parameter
		private LocalTime localTimeField;

		@Comment("local date")
		@Parameter
		private LocalDate localDateField;

		@Comment("local datetime")
		@Parameter
		private LocalDateTime localDateTimeField;

		@Comment("set")
		@Parameter
		private Set<String> setField;

		@Comment("list of enum")
		@Parameter
		private List<MyEnum> enumListField;

		@Comment("set of enum")
		@Parameter
		private Set<MyEnum> enumSetField;

		@Comment("list of ints")
		@Parameter
		private List<Integer> ints;

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

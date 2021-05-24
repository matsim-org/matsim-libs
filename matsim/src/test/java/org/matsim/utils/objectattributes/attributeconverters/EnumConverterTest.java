
/* *********************************************************************** *
 * project: org.matsim.*
 * EnumConverterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.utils.objectattributes.attributeconverters;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author thibautd
 */
public class EnumConverterTest {
	private enum MyEnum {
		SOME_CONSTANT,
		SOME_OTHER_CONSTANT;

		@Override
		public String toString() {
			return "Some random stuff that has nothing to do with the enum names. Some people might do this!";
		}
	}

	@Test
	public void testFromString() {
		final EnumConverter<MyEnum> converter = new EnumConverter<>( MyEnum.class );

		MyEnum some = converter.convert( "SOME_CONSTANT" );
		MyEnum other = converter.convert( "SOME_OTHER_CONSTANT" );

		Assert.assertEquals("unexpected enum", MyEnum.SOME_CONSTANT, some);
		Assert.assertEquals("unexpected enum", MyEnum.SOME_OTHER_CONSTANT, other);
	}

	@Test
	public void testToString() {
		final EnumConverter<MyEnum> converter = new EnumConverter<>( MyEnum.class );

		Assert.assertEquals( "unexpected String value", "SOME_CONSTANT", converter.convertToString( MyEnum.SOME_CONSTANT ) );
		Assert.assertEquals( "unexpected String value", "SOME_OTHER_CONSTANT", converter.convertToString( MyEnum.SOME_OTHER_CONSTANT ) );
	}
}

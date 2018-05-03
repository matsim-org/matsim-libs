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

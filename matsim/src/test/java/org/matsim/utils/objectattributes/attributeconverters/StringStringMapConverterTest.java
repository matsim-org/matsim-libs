package org.matsim.utils.objectattributes.attributeconverters;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringStringMapConverterTest {

  @Test
  public void test() {

    var expectedString = "{\"a\":\"value-a\",\"b\":\"value-b\"}";
    var converter = new StringStringMapConverter();

    var serializedString = converter.convertToString(converter.convert(expectedString));

    assertEquals(expectedString, serializedString);
  }
}

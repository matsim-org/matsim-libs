package org.matsim.utils.objectattributes.attributeconverters;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import org.junit.Test;

public class StringCollectionConverterTest {

  @Test
  public void test() {

    var expectedString = "[\"a\",\"b\"]";
    var converter = new StringCollectionConverter();
    Collection<String> convert = converter.convert(expectedString);
    var serializedString = converter.convertToString(convert);
    assertEquals(expectedString, serializedString);
  }
}

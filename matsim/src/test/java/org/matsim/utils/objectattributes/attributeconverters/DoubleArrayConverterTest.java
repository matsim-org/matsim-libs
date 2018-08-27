package org.matsim.utils.objectattributes.attributeconverters;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.utils.objectattributes.DoubleArrayConverter;

/**
 * @author jbischoff
 */
public class DoubleArrayConverterTest {


    @Test
    public void testFromToString() {
        final DoubleArrayConverter converter = new DoubleArrayConverter();
        String a = "-0.1,0,0.0005,17.3,5.2E22";
        double[] array = converter.convert(a);
        Assert.assertEquals(array.length, 5);
        Assert.assertEquals(array[0], -0.1, 0.00005);
        Assert.assertEquals(array[1], 0.0, 0.00005);
        Assert.assertEquals(array[2], 0.0005, 0.00005);
        Assert.assertEquals(array[3], 17.3, 0.00005);
        Assert.assertEquals(array[4], 5.2E22, 0.00005);

        String b = converter.convertToString(array);
        Assert.assertEquals("-0.1,0.0,5.0E-4,17.3,5.2E22", b);


    }

}

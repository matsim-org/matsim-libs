package playground.johannes.synpop.processing;

import playground.johannes.synpop.data.Segment;

import java.util.Random;

/**
 * Created by johannesillenberger on 06.04.17.
 */
public class RandomizeNumAttribute implements SegmentTask {

    private final String key;

    private final double range;

    private final Random random;

    public RandomizeNumAttribute(String key, double range, Random random) {
        this.key = key;
        this.range = range;
        this.random = random;
    }

    @Override
    public void apply(Segment segment) {
        String val = segment.getAttribute(key);
        if(val != null) {
            double numVal = Double.parseDouble(val);
            double offset = (random.nextDouble() - 0.5) * 2 * range;
            numVal += offset;
            segment.setAttribute(key, String.valueOf(numVal));
        }
    }
}

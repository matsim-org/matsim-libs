package playground.johannes.eut;

import static java.lang.Math.max;
import static java.lang.Math.sqrt;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A map-based helper class for storage of time variant float values with
 * default value <code>0</code>.
 * 
 * TODO: This implementation is rather slow but well-tested by now. Should be
 * replaced by an index-based approach in the future. (For state estimation this
 * is not really an issue, since there the performance bottleneck are the
 * numerics.)
 * 
 * @author gunnar
 * 
 */
public class TimevariantCostMapBased extends TimevariantCost {
	
	private static final Logger log = Logger.getLogger(TimevariantCostMapBased.class);

    // -------------------- MEMBER VARIABLES --------------------

    // use a linked has map such that order of entries is maintained
    private final Map<Object, float[]> cost = new LinkedHashMap<Object, float[]>();

    // -------------------- CONSTRUCTION --------------------

    public TimevariantCostMapBased(int startTime_s, int endTime_s, int binSize_s) {
        super(startTime_s, endTime_s, binSize_s);
    }

    // -------------------- INTERNALS --------------------

    private static Object compoundKey(Object... subKeys) {
        if (subKeys == null || subKeys.length == 0)
            throw new IllegalArgumentException(
                    "There must be at least one sub key.");
        else if (subKeys.length == 1)
            return subKeys[0];
        else
            return Arrays.asList(subKeys);
    }

    private static Object[] subKeys(Object compoundKey) {
        if (compoundKey instanceof List)
            return ((List) compoundKey).toArray();
        else
            return new Object[] { compoundKey };
    }

    private void recalculateBounds() {
        super.lowerBound = 0;
        super.upperBound = 0;
        for (float[] values : cost.values())
            if (values != null)
                for (float val : values)
                    updateBounds(val);
    }

    // -------------------- GETTERS --------------------

    protected double getBinCost(int bin, Object... keys) {
        final float[] values = cost.get(compoundKey(keys));
        return (values != null) ? getScale() * values[bin] : 0.0;
    }

    // TODO new
    // protected double getBinAvgCost(int bin, Object... keys) {
    // final int bin1 = constrBin(bin);
    // final int bin2 = constrBin(bin + 1);
    // return 0.5 * (getBinCost(bin1, keys) + getBinCost(bin2, keys));
    // }

    public int size() {
        return cost.size() * getBinCnt();
    }

    public String toString() {
        StringBuffer result = new StringBuffer(getClass().getName() + ":\n");
        result.append(super.toString() + "\n");
        result.append("\tsize      = " + size());
        return result.toString();
    }

    // -------------------- SETTERS --------------------

    protected void setBinCost(double value, int bin, Object... keys) {

        if (getScale() != 1)
            log.warn("scale is not 1.0");

        final Object key = compoundKey(keys);
        float[] values = cost.get(key);
        if (values == null) {
            if (value == 0)
                return;
            values = new float[getBinCnt()];
            Arrays.fill(values, 0);
            cost.put(key, values);
        }
        values[bin] = (float) value;
        super.updateBounds(value);
    }

    // TODO new
    private float[] dataArrayNotNull(Object key) {
        float[] result = cost.get(key);
        if (result == null) {
            result = new float[getBinCnt()];
            Arrays.fill(result, 0);
            cost.put(key, result);
        }
        return result;
    }

    // TODO new
    protected void addBinCost(double addend, int bin, Object... keys) {

        if (getScale() != 1)
            log.warn("scale is not 1.0");

        final float[] values = dataArrayNotNull(compoundKey(keys));
        values[bin] = (float) (addend + values[bin]);
        super.updateBounds(values[bin]);
    }

    // TODO new
    protected void multBinCost(double factor, int bin, Object... keys) {

        if (getScale() != 1)
            log.warn("scale is not 1.0");

        final float[] values = dataArrayNotNull(compoundKey(keys));
        values[bin] = (float) (factor * values[bin]);
        super.updateBounds(values[bin]);
    }

    // -------------------- MODIFIERS --------------------

    // TODO new
    public void makeAbsolute() {
        for (float[] array : cost.values())
            if (array != null) {
                for (int i = 0; i < array.length; i++)
                    array[i] = Math.abs(array[i]);
            }
        recalculateBounds();
    }

    // TODO new
    public void square() {
        for (float[] array : cost.values())
            if (array != null) {
                for (int i = 0; i < array.length; i++)
                    array[i] *= array[i];
            }
        recalculateBounds();
    }

    // TODO new
    public void average() {
        for (float[] array : cost.values())
            if (array != null) {
                float newValue = 0;
                for (int i = 0; i < array.length; i++)
                    newValue += array[i];
                newValue /= array.length;
                for (int i = 0; i < array.length; i++)
                    array[i] = newValue;
            }
        recalculateBounds();
    }

    @Override
    public void clear() {
        super.clear();
        cost.clear();
    }

    /**
     * Multiplies every entry in this cost object by factor. This is different
     * from scaling since it effectively <em>modifies</em> all entries.
     * 
     * @param factor
     */
    public void multiply(double factor) {
        for (float[] values : cost.values())
            if (values != null)
                for (int i = 0; i < values.length; i++)
                    values[i] *= factor;
    }

    /**
     * Multiplies <code>this</code> by factor <code>myWeight</code> and then
     * adds <code>other</code> multiplied by <code>otherWeight</code>.
     * 
     * @param myWeight
     * @param other
     * @param otherWeight
     */
    public void addW(double myWeight, TimevariantCostMapBased other,
            double otherWeight) {

        // CHECK

        if (!this.isCompatibleWith(other))
            log.warn(
                    "The passed cost object instance is not "
                            + "fully compatible with this instance!");

        if (this.getScale() != 1)
        	log.warn(
                    "Scale of this is not 1.0 -- Ignoring this setting!");

        if (other.getScale() != 1.0) {
            otherWeight *= other.getScale();
            log.warn(
                    "Addend is already scaled. Multiplying its scale of "
                            + other.getScale() + " into new otherWeight of "
                            + otherWeight + ".");
        }

        // CONTINUE

        if (myWeight == 0)
            this.clear();
        else if (myWeight != 1.0)
            this.multiply(myWeight);

        for (Map.Entry<Object, float[]> otherEntry : other.cost.entrySet()) {
            final float[] src = otherEntry.getValue();
            if (src != null) {
                float[] dst = this.cost.get(otherEntry.getKey());
                if (dst == null) {
                    dst = new float[getBinCnt()];
                    Arrays.fill(dst, 0);
                    this.cost.put(otherEntry.getKey(), dst);
                }
                for (int i = 0; i < dst.length; i++)
                    dst[i] += otherWeight * src[i];
            }
        }

        this.recalculateBounds();
    }

    /**
     * Writes a randomization of <code>this</code> into <code>target</code>.
     * Entries in <code>target</code> are distributed with expectations equal
     * to according values in <code>this</code> and variance equal to
     * <code>varianceScale</code> times the according value in
     * <code>this</code>. Parameter <code>seed</code> allows for
     * reproducible randomizations. If "arbitrary" randomness is required,
     * <code>(new Random()).nextLong()</code> can be used for
     * <code>seed</code>.
     * 
     * @param target
     * @param varianceScale
     * @param seed
     */
    public void randomize(TimevariantCostMapBased target, double varianceScale,
            long seed) {

        // CHECK

        if (!this.isCompatibleWith(target))
            throw new IllegalArgumentException(
                    "Passed instance is incompatible with this instance.");

        // CONTINUE

        final Random rnd = new Random(seed);
        target.clear();

        // The order of iteration is unique since cost is of type LinkedHashMap.
        for (Map.Entry<Object, float[]> entry : this.cost.entrySet()) {
            final float[] src = entry.getValue();
            final float[] dst = new float[src.length];
            for (int i = 0; i < src.length; i++) {
                dst[i] = max(0, src[i]
                        + (float) (sqrt(src[i] * varianceScale) * rnd
                                .nextGaussian()));
            }
            target.cost.put(entry.getKey(), dst);
        }
    }

    // SQUARE ERROR

    public double avgSqrError(TimevariantCostMapBased other, boolean abs) {

        final Set<Object> allKeys = new HashSet<Object>();
        allKeys.addAll(this.cost.keySet());
        allKeys.addAll(other.cost.keySet());

        return avgSqrError(other, allKeys, abs);
    }

    public double avgSqrError(TimevariantCostMapBased other,
            Collection<Object> keys, boolean abs) {

        double errSum = 0;
        int cnt = 0;

        // CHECK

        if (!this.isCompatibleWith(other))
            throw new IllegalArgumentException(
                    "Passed instance is incompatible with this instance.");

        // CONTINUE

        for (Object key : keys) {
            final float[] thisVals = this.cost.get(key);
            final float[] otherVals = other.cost.get(key);
            for (int bin = 0; bin < getBinCnt(); bin++) {
                final double err = thisVals[bin] - otherVals[bin];
                errSum += (abs ? Math.abs(err) : err * err);
                cnt++;
            }
        }

        return errSum / cnt;
    }

    // CORRELATION

    // TODO untested
//    public double correlation(TimevariantCostMapBased other) {
//
//        final Set<Object> allKeys = new HashSet<Object>();
//        allKeys.addAll(this.cost.keySet());
//        allKeys.addAll(other.cost.keySet());
//
//        return correlation(other, allKeys);
//    }

    // TODO untested
//    public double correlation(TimevariantCostMapBased other,
//            Collection<Object> keys) {
//        final Correlator c = new Correlator();
//
//        // CHECK
//
//        if (!this.isCompatibleWith(other))
//            throw new IllegalArgumentException(
//                    "Passed instance is incompatible with this instance.");
//
//        // CONTINUE
//
//        for (Object key : keys) {
//            final float[] thisVals = this.cost.get(key);
//            final float[] otherVals = other.cost.get(key);
//            for (int bin = 0; bin < getBinCnt(); bin++)
//                c.add(thisVals != null ? thisVals[bin] : 0.0,
//                        otherVals != null ? otherVals[bin] : 0.0);
//        }
//
//        return c.getCorr();
//    }

    // TODO untested
    public void scatterplotAgainst(TimevariantCostMapBased other,
            String filename) {
        final Set<Object> allKeys = new HashSet<Object>();
        allKeys.addAll(this.cost.keySet());
        allKeys.addAll(other.cost.keySet());
        scatterplotAgainst(other, filename, allKeys);
    }

    /**
     * Creates a file with full name <code>filename</code> containing three
     * tab-separated columns: (1) [key/time] tuple, (2) value as stored in
     * <code>this</code> instance, (3) value as stored in <code>other</code>
     * instance.
     * 
     * TODO add function that plots only for a subset of links (similar to
     * correlation function in this class)
     * 
     * @param other
     * @param filename
     */
    public void scatterplotAgainst(TimevariantCostMapBased other,
            String filename, Collection<Object> keys) {

        // CHECK

        if (!this.isCompatibleWith(other))
            throw new IllegalArgumentException(
                    "Passed instance is incompatible with this instance.");

        // CONTINUE

        try {

            final PrintWriter writer = new PrintWriter(new File(filename));

            writer.println("[key/time]\treference\tother");

            for (Object key : keys) {
                final float[] thisVals = this.cost.get(key);
                final float[] otherVals = other.cost.get(key);
                for (int bin = 0; bin < getBinCnt(); bin++)
                    writer.println("["
                            + key
                            + "/"
                            + Time.strFromSec(getStartTime_s() + bin
                                    * getBinSize_s())
                            + "]\t"
                            + (thisVals != null ? Float.toString(thisVals[bin])
                                    .replace('.', ',') : "0,0")
                            + "\t"
                            + (otherVals != null ? Float.toString(
                                    otherVals[bin]).replace('.', ',') : "0,0"));
            }

            writer.flush();
            writer.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // ==================== FILE IO ====================

    /**
     * @author gunnar
     */
    public abstract static class FileIO extends DefaultHandler {

        // -------------------- CONSTANTS --------------------

        final String OUTER_ELEMENT = "timevariantcostmapbased";

        final String SUBCLASS_ATTR = "subclass";

        final String STARTTIME_ATTR = "starttime";

        final String ENDTIME_ATTR = "endtime";

        final String BINSIZE_ATTR = "binsize";

        final String ENTRY_ELEMENT = "entry";

        final String KEY_ATTR = "key";

        final String VALUE_ATTR = "value";

        // -------------------- MEMBER VARIABLES --------------------

        private TimevariantCostMapBased result = null;

        // -------------------- CONSTRUCTION --------------------

        protected FileIO() {
        }

        // -------------------- INTERFACE DEFINTION --------------------

        protected abstract TimevariantCostMapBased newInstance(int startTime_s,
                int endTime_s, int binSize_s);

        protected abstract String keys2string(Object... keys);

        protected abstract Object[] string2keys(String string);

        // -------------------- WRITING --------------------

        private String qt(String s) {
            return "\"" + s + "\"";
        }

        private String values2string(float[] entry) {
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < entry.length; i++)
                result.append(entry[i] + " ");
            return result.toString();
        }

        public void write(String filename, TimevariantCostMapBased tvcmb) {
            try {
                PrintWriter writer = new PrintWriter(new File(filename));

                writer.println("<" + OUTER_ELEMENT + " " + STARTTIME_ATTR + "="
                        + qt("" + tvcmb.getStartTime_s()) + " " + ENDTIME_ATTR
                        + "=" + qt("" + tvcmb.getEndTime_s()) + " "
                        + BINSIZE_ATTR + "=" + qt("" + tvcmb.getBinSize_s())
                        + " " + SUBCLASS_ATTR + "="
                        + qt(this.getClass().getName()) + ">");

                for (Map.Entry<Object, float[]> entry : tvcmb.cost.entrySet())
                    if (entry.getValue() != null)
                        writer.println("  <" + ENTRY_ELEMENT + " " + KEY_ATTR
                                + "="
                                + qt(keys2string(subKeys(entry.getKey())))
                                + " " + VALUE_ATTR + "="
                                + qt(values2string(entry.getValue())) + "/>");

                writer.println("</" + OUTER_ELEMENT + ">");

                writer.flush();
                writer.close();
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            }
        }

        // -------------------- READING --------------------

        private float[] string2values(String value) {
            if (value == null)
                return null;

            final float[] entryArray = new float[this.result.getBinCnt()];
            final String[] entryStrings = value.split("\\s");

            if (entryArray.length != entryStrings.length) {
//                Gbl.getLogger().warning(
//                        "Inconsistent data dimensions. Skipping this entry.");
                return null;
            }

            for (int i = 0; i < Math
                    .min(entryArray.length, entryStrings.length); i++)
                entryArray[i] = Float.parseFloat(entryStrings[i]);
            return entryArray;
        }

        public TimevariantCostMapBased read(String file) {
            result = null;
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser parser = factory.newSAXParser();
                if(file.endsWith(".gz") || file.endsWith(".zip"))
                	parser.parse(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))), this);
    			else
    				parser.parse(file, this);
            } catch (Exception e) {
            	log.warn(
                        "Exception during file parsing: " + e.toString());
                result = null;
            }

            // todo this is new
            result.recalculateBounds();

            return result;
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) {
            if (OUTER_ELEMENT.equals(qName))
                startOuterElement(attributes);
            else if (ENTRY_ELEMENT.equals(qName))
                startEntryElement(attributes);
        }

        private void startOuterElement(Attributes attrs) {

            // CHECK

            final String subclass = attrs.getValue(SUBCLASS_ATTR);
            if (this.getClass().getName() == null
                    || !this.getClass().getName().equals(subclass))
            	log.warn(
                        "This file might have been "
                                + "written by a different class: " + subclass);

            // CONTINUE

            final int startTime_s = Integer.parseInt(attrs
                    .getValue(STARTTIME_ATTR));
            final int endTime_s = Integer
                    .parseInt(attrs.getValue(ENDTIME_ATTR));
            final int binSize_s = Integer
                    .parseInt(attrs.getValue(BINSIZE_ATTR));
            result = newInstance(startTime_s, endTime_s, binSize_s);
        }

        private void startEntryElement(Attributes attrs) {
            final Object key = compoundKey(string2keys(attrs.getValue(KEY_ATTR)));
            final float[] values = string2values(attrs.getValue(VALUE_ATTR));
            result.cost.put(key, values);
        }
    }

    // -------------------- TESTING --------------------

    public String getReport(String heading) {

        final double[] minValue = new double[getBinCnt()];
        final double[] maxValue = new double[getBinCnt()];

        for (Map.Entry<Object, float[]> entry : cost.entrySet()) {
            final float[] values = entry.getValue();
            if (values != null)
                for (int i = 0; i < entry.getValue().length; i++) {
                    minValue[i] = Math.min(minValue[i], values[i]);
                    maxValue[i] = Math.max(maxValue[i], values[i]);
                }
        }

        final StringBuffer result = new StringBuffer();

        result
                .append("\n------------------------------------------------------------\n");
        result.append("\n" + heading);
        result.append("\n\nmin\tmax\n");
        for (int i = 0; i < getBinCnt(); i++)
            result.append((minValue[i] + "\t" + maxValue[i] + "\n").replace(
                    '.', ','));
        result
                .append("\n------------------------------------------------------------\n");

        return result.toString();
    }

    @SuppressWarnings("unused")
    private static void test_addW() {
        final TimevariantCostMapBased m1 = new TimevariantCostMapBased(0, 3600,
                60);
        final TimevariantCostMapBased m2 = new TimevariantCostMapBased(0, 3600,
                60);

        final String key1 = "key1";
        final String key2 = "key2";
        final String key12 = "key12";

        m1.setBinCost(1.0, 1, key1);
        m2.setBinCost(10.0, 1, key2);
        m1.setBinCost(1.0, 1, key12);
        m2.setBinCost(10.0, 1, key12);

        m1.addW(0.1, m2, 10);

        System.out.println(m1.getBinCost(1, key1));
        System.out.println(m1.getBinCost(1, key2));
        System.out.println(m1.getBinCost(1, key12));
        System.out.println(m1.getBinCost(0, "unknown"));
    }

//    @SuppressWarnings("unused")
//    private static void test_randomization() {
//
//        final double varianceScale = 0.1;
//
//        final TimevariantCostMapBased m = new TimevariantCostMapBased(0, 600,
//                60);
//        final Correlator[] correlators = new Correlator[m.getBinCnt()];
//        for (int bin = 0; bin < m.getBinCnt(); bin++) {
//            m.setBinCost(1.0 + Math.random(), bin, "key");
//            correlators[bin] = new Correlator();
//        }
//
//        for (int rep = 0; rep < 1000 * 100; rep++) {
//
//            final TimevariantCostMapBased m2 = new TimevariantCostMapBased(0,
//                    600, 60);
//            m.randomize(m2, varianceScale, (new Random()).nextLong());
//
//            for (int bin = 0; bin < m.getBinCnt(); bin++)
//                correlators[bin].add(m.getBinCost(bin, "key"), m2.getBinCost(
//                        bin, "key"));
//        }
//
//        System.out.println("average ratio\t\tvariance ratio");
//        for (Correlator c : correlators) {
//            System.out.println(c.getXAvg() / c.getYAvg() + "\t" + varianceScale
//                    / (c.getYVar() / c.getXAvg()));
//        }
//
//        System.out.println("DONE");
//    }

    @SuppressWarnings("unused")
    private static void test_scatterplot() {
        TimevariantCostMapBased m1 = new TimevariantCostMapBased(0, 119, 60);
        TimevariantCostMapBased m2 = new TimevariantCostMapBased(0, 119, 60);

        m1.setBinCost(100, 0, "a");
        m1.setBinCost(200, 1, "c");

        m2.setBinCost(100, 0, "b");
        m2.setBinCost(175, 1, "c");

        System.out.println("bin cnt is " + m1.getBinCnt());

        m1.scatterplotAgainst(m2, "/home/gunnar/scatterplot-test.txt");
    }

    public static void main(String[] args) {
        // test_addW();
        // test_randomization();
        test_scatterplot();
        System.out.println("DONE");
    }

}

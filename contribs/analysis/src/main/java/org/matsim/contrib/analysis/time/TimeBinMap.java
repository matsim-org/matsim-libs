package org.matsim.contrib.analysis.time;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps values to time bins. Time bins have a fixed size and are relative to the chosen start time. If no start time is
 * configured, 0 is assumed as start time of the first bin. New bins are created as they are requested by
 * {@link org.matsim.contrib.analysis.time.TimeBinMap#getTimeBin(double)}. Note: only requested time bins are created, so
 * that there is no guarantee that time bins are consecutive.
 *
 * @param <T>
 */
public class TimeBinMap<T> {

    private final Map<Integer, TimeBin<T>> bins = new HashMap<>();
    private final double binSize;
    private final double startTime;
    private double endTimeOfLastBucket;

    /**
     * Creates new instance of TimeBinMap
     *
     * @param timeBinSize size of one time bin
     */
    public TimeBinMap(final double timeBinSize) {
        this(timeBinSize, 0);
    }

    /**
     * Creates a new instance of TimeBinMap
     * @param timeBinSize size of one time bin
     * @param startTimeOfFirstBin start time of first time bin. Default is 0.
     */
    public TimeBinMap(final double timeBinSize, final double startTimeOfFirstBin) {
        this.binSize = timeBinSize;
        this.startTime = startTimeOfFirstBin;
    }

    /**
     * Get a time bin for the given time. Returns an existing time bin or creates a new one if no bin is present yet.
     * @param forTime time for which a bin is requested.
     * @return Time bin which contains the requested time
     */
    public TimeBin<T> getTimeBin(double forTime) {

        if (forTime < startTime)
            throw new IllegalArgumentException("only times greater than startTime of: " + startTime + " allowed");

        int binIndex = (int) ((forTime - startTime) / binSize);
        if (!bins.containsKey(binIndex)) {
            bins.put(binIndex, new TimeBin<>(startTime + binIndex * binSize));
            adjustEndTime(binIndex);
        }
        return bins.get(binIndex);
    }

    /**
     * Retrieve the end time of the last time bin.
     *
     * @return end time of the last bin and of the whole time series stored by this map
     */
    public double getEndTimeOfLastBin() {
        return endTimeOfLastBucket;
    }

    /**
     * Retrieve all time bins. Note: There is no guarantee that time bins are consecutive. Since only requested bins are
     * created, it is possible that there are 'gaps' in the time series.
     * @return all time bins
     */
    public Collection<TimeBin<T>> getTimeBins() {
        return bins.values();
    }

    /**
     * Delete all values
     */
    public void clear() {
        bins.clear();
    }

    private void adjustEndTime(int newBinIndex) {

        if (startTime + binSize + newBinIndex * binSize > endTimeOfLastBucket)
            endTimeOfLastBucket = startTime + binSize + newBinIndex * binSize;
    }

    public static class TimeBin<T> {

        private double startTime;
        private T value;

        private TimeBin(double startTime) {
            this.startTime = startTime;
        }

        public double getStartTime() {
            return startTime;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public boolean hasValue() {
            return this.value != null;
        }
    }
}

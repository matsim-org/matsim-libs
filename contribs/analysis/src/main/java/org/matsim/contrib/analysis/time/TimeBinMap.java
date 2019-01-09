package org.matsim.contrib.analysis.time;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TimeBinMap<T> {

    private final Map<Integer, TimeBin<T>> bins = new HashMap<>();
    private final double binSize;
    private final double startTime;
    private double endTimeOfLastBucket;

    public TimeBinMap(final double timeBinSize) {
        this(timeBinSize, 0);
    }

    public TimeBinMap(final double timeBinSize, final double startTimeOfFirstBin) {
        this.binSize = timeBinSize;
        this.startTime = startTimeOfFirstBin;
    }

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

    public double getEndTimeOfLastBucket() {
        return endTimeOfLastBucket;
    }

    public Collection<TimeBin<T>> getAllTimeBins() {
        return bins.values();
    }

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

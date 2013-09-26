package pl.poznan.put.vrp.dynamic.util;

public class TimeDiscretizer
{
    public static final TimeDiscretizer TD_24H_BY_15MIN = new TimeDiscretizer(15 * 60, 4 * 24);

    private final int timeInterval;
    private final int intervalCount;
    private final boolean cyclic;


    public TimeDiscretizer(int timeInterval, int intervalCount)
    {
        this(timeInterval, intervalCount, true);
    }


    public TimeDiscretizer(int timeInterval, int intervalCount, boolean cyclic)
    {
        this.timeInterval = timeInterval;
        this.intervalCount = intervalCount;
        this.cyclic = cyclic;
    }


    public int getIdx(int time)
    {
        int idx = time / timeInterval;

        if (cyclic) {
            return idx % intervalCount;
        }

        if (idx >= intervalCount) {
            throw new IllegalStateException();
        }

        return idx;
    }


    public int getTime(int idx)
    {
        return idx * timeInterval;
    }


    public WeightedIdxPair getIdxWeightedPair(int time)
    {
        int idx0 = time / timeInterval;
        int idx1 = idx0 + 1;

        if (cyclic) {
            idx0 %= intervalCount;
            idx1 %= intervalCount;
        }
        else {
            if (idx1 >= intervalCount) {
                throw new IllegalStateException();
            }
        }

        int weight1 = (time % timeInterval);
        int weight0 = timeInterval - weight1;

        return new WeightedIdxPair(idx0, idx1, weight0, weight1);
    }


    public double interpolate(double[] vals, int time)
    {
        int idx0 = time / timeInterval;
        int idx1 = idx0 + 1;

        if (cyclic) {
            idx0 %= intervalCount;
            idx1 %= intervalCount;
        }
        else {
            if (idx1 >= intervalCount) {
                throw new IllegalStateException();
            }
        }

        int weight1 = (time % timeInterval);
        int weight0 = timeInterval - weight1;

        double weightedSum = weight0 * vals[idx0] + weight1 * vals[idx1];
        return weightedSum / timeInterval;

    }


    public int interpolate(int[] vals, int time)
    {
        int idx0 = time / timeInterval;
        int idx1 = idx0 + 1;

        if (cyclic) {
            idx0 %= intervalCount;
            idx1 %= intervalCount;
        }
        else {
            if (idx1 >= intervalCount) {
                throw new IllegalStateException();
            }
        }

        int weight1 = (time % timeInterval);
        int weight0 = timeInterval - weight1;

        double weightedSum = weight0 * vals[idx0] + weight1 * vals[idx1];// int -> double
        return (int)Math.round(weightedSum / timeInterval);// double -> int

    }


    public int getTimeInterval()
    {
        return timeInterval;
    }


    public int getIntervalCount()
    {
        return intervalCount;
    }


    public boolean isCyclic()
    {
        return cyclic;
    }


    public static class WeightedIdxPair
    {
        public final int idx0;
        public final int idx1;

        public final int weight0;
        public final int weight1;


        private WeightedIdxPair(int idx0, int idx1, int weight0, int weight1)
        {
            this.idx0 = idx0;
            this.idx1 = idx1;
            this.weight0 = weight0;
            this.weight1 = weight1;
        }
    }
}

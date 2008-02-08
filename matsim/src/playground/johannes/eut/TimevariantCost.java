package playground.johannes.eut;


/**
 * 
 * @author gunnar
 * 
 */
public abstract class TimevariantCost {

    // -------------------- MEMBER VARIABLES --------------------

    private final int startTime_s;

    private final int endTime_s;

    private final int binSize_s;

    private final int binCnt;

    private double scale = 1.0;

    protected double lowerBound = 0;

    protected double upperBound = 0;

    // -------------------- CONSTRUCTION --------------------

    /**
     * @throws IllegalArgumentException
     *             if temporal parameters make no sense
     */
    public TimevariantCost(int startTime_s, int endTime_s, int binSize_s) {

        // CHECK

        if (startTime_s < 0 || endTime_s < startTime_s || binSize_s <= 0)
            throw new IllegalArgumentException("startTime=" + startTime_s
                    + "s, endTime=" + endTime_s + "s, binSize=" + binSize_s
                    + "s.");

        // CONTINUE

        this.startTime_s = startTime_s;
        this.endTime_s = endTime_s;
        this.binSize_s = binSize_s;

        this.binCnt = 1 + (endTime_s - startTime_s) / binSize_s;
    }

    // -------------------- GETTERS --------------------

    public double getScale() {
        return scale;
    }

    public int getStartTime_s() {
        return startTime_s;
    }

    public int getEndTime_s() {
        return endTime_s;
    }

    public int getBinSize_s() {
        return binSize_s;
    }

    public int getBinCnt() {
        return binCnt;
    }

    public double getLowerBound() {
        return scale * lowerBound;
    }

    public double getUpperBound() {
        return scale * upperBound;
    }

    public int constrBin(final int bin) {
        final int constrBin = Arithm.constr(bin, 0, binCnt - 1);

        // TODO need some kind of non-terminating assert here
        // if (constrBin != bin)
        // Gbl.warn(getClass(), "constrBin(int)", "Tried to access bin " + bin
        // + ", which does not exist!");

        return constrBin;
    }

    public int getBin(int time_s) {
        return constrBin((time_s - startTime_s) / binSize_s);
    }

    public int getBinCenterTime_s(int bin) {
        bin = constrBin(bin);
        return getStartTime_s() + bin * getBinSize_s() + getBinSize_s() / 2;
    }

    // -------------------- MODIFIERS --------------------

    public void setScale(double scale) {
        this.scale = scale;
    }

    protected void updateBounds(double value) {
        lowerBound = Math.min(lowerBound, value);
        upperBound = Math.max(upperBound, value);
    }

    public void clear() {
        this.scale = 1.0;
        this.lowerBound = 0;
        this.upperBound = 0;
    }

    // -------------------- MISC --------------------

    public boolean isCompatibleWith(TimevariantCost other) {
        return (this.startTime_s == other.startTime_s
                && this.endTime_s == other.endTime_s
                && this.binSize_s == other.binSize_s && this.binCnt == other.binCnt);
    }

    public String toString() {
        StringBuffer result = new StringBuffer(getClass().getName() + ":\n");
        result.append("\tstartTime_s = " + startTime_s + "\n");
        result.append("\tendTime_s   = " + endTime_s + "\n");
        result.append("\tbinSize_s   = " + binSize_s + "\n");
        result.append("\tbinCnt      = " + binCnt + "\n");
        result.append("\tscale       = " + scale + "\n");
        return result.toString();
    }

}

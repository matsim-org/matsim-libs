package playground.clruch.export;

/**
 * Class for handling intervals
 */
class DoubleInterval {
    double start;
    double end;

    /**
     * Checks if value is in interval [start,end)
     *
     * @param value double value to be checked
     * @return boolean value
     */
    public boolean isInside(double value) {
        return start <= value && value < end;
    }

    @Override
    public String toString() {
        return start + " " + end;
    }
}

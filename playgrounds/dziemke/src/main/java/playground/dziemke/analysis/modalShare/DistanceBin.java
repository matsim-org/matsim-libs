package playground.dziemke.analysis.modalShare;

/**
 * @author gthunig on 21.03.2017.
 */
public class DistanceBin {

    private final int binNumer;
    private final int fromDistance;
    private final int toDistance;
    private int[] values;

    DistanceBin(int binNumer, int fromDistance, int toDistance, int numberOfValues) {
        this.binNumer = binNumer;
        this.fromDistance = fromDistance;
        this.toDistance = toDistance;
        reset(numberOfValues);
    }

    public int getBinNumer() {
        return binNumer;
    }

    public int[] getValues() { return values; }

    public void raiseValue(int numberOfValue) {
        values[numberOfValue]++;
    }

    public int getFromDistance() { return fromDistance; }

    public int getToDistance() { return toDistance; }

    public void reset(int numberOfValues) {
        this.values = new int[numberOfValues];

    }

    public String toString() {
        String result = "";
        result += "<DistanceBin binNumber=" + this.binNumer;
        result += " fromDistance=" + this.fromDistance;
        result += " toDistance=" + this.toDistance;
        for (int i = 0; i < values.length; i++) {
            result += " value" + i + "=" + values[i];
        }
        result += ">";

        return result;
    }
}

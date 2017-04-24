package playground.dziemke.analysis.modalShare;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gthunig on 21.03.2017.
 */
public class DistanceBinContainer {

    private final int binSize;
    private final int numberOfValues;
    private final List<DistanceBin> distanceBins;

    public DistanceBinContainer(int binSize, int numberOfValues) {
        this.binSize = binSize;
        this.numberOfValues = numberOfValues;
        this.distanceBins = new ArrayList<>();
        distanceBins.add(new DistanceBin(0, 0, binSize-1, numberOfValues));
    }

    public void enterDistance(int distance, int numberOfValue) {
        DistanceBin bin = getBinFromDistance(distance);
        if (bin == null) {
            createUpToBin(distance);
        }
        raiseBinFromDistance(distance, numberOfValue);
    }

    private void createUpToBin(int distance) {
        int binNumber = getBinNumber(distance);
        int fromDistance = getFromDistance(binNumber);
        int toDistance = getToDistance(binNumber);

        createBin(binNumber, fromDistance, toDistance);
    }

    private void createBin(int binNumber, int fromDistance, int toDistance) {
        DistanceBin previousBin = get(binNumber - 1);
        if (previousBin == null) createBin(binNumber -1, fromDistance - binSize, toDistance - binSize);
        createBin(binNumber, fromDistance, toDistance, this.numberOfValues);
    }

    private void createBin(int binNumber, int fromDistance, int toDistance, int valueCount) {
        distanceBins.add(new DistanceBin(binNumber, fromDistance, toDistance, numberOfValues));
    }

    private DistanceBin getBinFromDistance(int distance) {
        return get(getBinNumber(distance));
    }

    private int getBinNumber(int distance) {
        return distance / binSize;
    }

    private int getFromDistance(int binNumber) {
        return binNumber * binSize;
    }

    private int getToDistance(int binNumber) {
        return (binSize * (binNumber + 1)) -1;
    }

    private DistanceBin get(int binNumer) {
        if (binNumer < distanceBins.size()) return distanceBins.get(binNumer);
        else return null;
    }

    private void raiseBin(DistanceBin bin, int numberOfValue) {
        bin.raiseValue(numberOfValue);
    }

    private void raiseBinFromDistance(int distance, int numberOfValue) {
        raiseBin(getBinFromDistance(distance), numberOfValue);
    }

    public void reset() {
        for (DistanceBin bin : distanceBins) {
            bin.reset(this.numberOfValues);
        }
    }

    public List<DistanceBin> getDistanceBins() {
        return new ArrayList<>(distanceBins);
    }

    public String toString() {
        String result = "";
        result += "<DistanceBinContainer binSize=" + this.binSize;
        result += " numberOfValues=" + this.numberOfValues;
        result += ">";
        result += "\n";
        for (DistanceBin bin : distanceBins) {
            result += bin.toString();
            result += "\n";
        }
        result += "</DistanceBinContainer>";

        return result;
    }

}

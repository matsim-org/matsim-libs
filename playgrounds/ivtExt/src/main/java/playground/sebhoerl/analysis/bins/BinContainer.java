package playground.sebhoerl.analysis.bins;

import java.util.ArrayList;
import java.util.Iterator;

public class BinContainer<BinType extends Bin<Data>, Data> implements Iterable<BinType> {
    final private int numberOfBins;
    final private ArrayList<BinType> bins;
    
    public BinContainer(int numberOfBins, BinFactory<BinType, Data> factory) {
        this.numberOfBins = numberOfBins;
        
        bins = new ArrayList<BinType>(numberOfBins);

        for (int i = 0; i < numberOfBins; i++) {
            bins.add(factory.createBin(i));
        }
    }
    
    public int getNumberOfBins() {
        return numberOfBins;
    }
    
    public BinType getBinByIndex(int index) {
        return bins.get(index);
    }
    
    @Override
    public Iterator<BinType> iterator() {
        return bins.iterator();
    }
}

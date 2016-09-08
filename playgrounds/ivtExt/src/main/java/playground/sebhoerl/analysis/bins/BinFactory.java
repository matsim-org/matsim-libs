package playground.sebhoerl.analysis.bins;

public interface BinFactory<BinType extends Bin<Data>, Data> {
    BinType createBin(int index);
}

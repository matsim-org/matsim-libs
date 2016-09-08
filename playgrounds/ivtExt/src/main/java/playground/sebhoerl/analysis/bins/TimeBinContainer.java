package playground.sebhoerl.analysis.bins;

public class TimeBinContainer<Data> extends BinContainer<TimeBin<Data>, Data> {
    final private double start;
    //final private double end;
    final private double interval;
    
    public TimeBinContainer(final double start, double end, final double interval, final BinDataFactory<Data> factory) {
        super((int) Math.ceil((end - start) / interval), new BinFactory<TimeBin<Data>, Data>() {
            @Override
            public TimeBin<Data> createBin(int index) {
                return new TimeBin<Data>(start + index * interval, start + (index + 1) * interval, factory.createData());
            }
        });
        
        this.start = start;
        this.interval = interval;
    }
    
    public int getIndexByTime(double time) {
        return (int) Math.floor((time - start) / interval);
    }
    
    public TimeBin<Data> getBinByTime(double time) {
        return getBinByIndex(getIndexByTime(time));
    }
}

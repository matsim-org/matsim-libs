package playground.sebhoerl.analysis.bins;

public class TimeBin<Data> implements Bin<Data> {
    final private double start;
    final private double end;
    private Data data;
    
    public TimeBin(double start, double end, Data data) {
        this.start = start;
        this.end = end;
        this.data = data;
    }

    @Override
    public Data getData() {
        return data;
    }
    
    public void setData(Data data) {
        this.data = data;
    }
    
    public double getStart() { return start; }
    public double getEnd() { return end; }
}

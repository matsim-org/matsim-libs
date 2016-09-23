package playground.sebhoerl.analysis.bins;

public class TimeModeBin<Data> implements Bin<Data> {
    final private double start;
    final private double end;
    final private String mode;
    private Data data;
    
    public TimeModeBin(double start, double end, String mode, Data data) {
        this.start = start;
        this.mode = mode;
        this.end = end;
        this.data = data;
    }
    
    @Override
    public Data getData() {
        return this.data;
    }
    
    public void setData(Data data) {
        this.data = data;
    }
    
    public double getStart() { return start; }
    public double getEnd() { return end; } 
    public String getMode() { return mode; }
}

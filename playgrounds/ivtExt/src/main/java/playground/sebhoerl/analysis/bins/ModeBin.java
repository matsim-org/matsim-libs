package playground.sebhoerl.analysis.bins;

public class ModeBin<Data> implements Bin<Data> {
    final private String mode;
    private Data data;
    
    public ModeBin(String mode, Data data) {
        this.mode = mode;
        this.data = data;
    }

    @Override
    public Data getData() {
        return data;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setData(Data data) {
        this.data = data;
    }
}

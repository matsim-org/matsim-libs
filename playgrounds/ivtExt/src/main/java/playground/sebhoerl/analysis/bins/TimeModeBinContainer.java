package playground.sebhoerl.analysis.bins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TimeModeBinContainer<Data> extends BinContainer<TimeModeBin<Data>, Data> {    
    final private ArrayList<String> modes = new ArrayList<>();
    final private int numberOfTimeBins;
    
    final private double start;
    final private double end;
    final private double interval;
    
    public TimeModeBinContainer(final List<String> modes, final double start, final double end, final double interval, final BinDataFactory<Data> factory) {
        super((int) Math.ceil((end - start) / interval) * modes.size(), new BinFactory<TimeModeBin<Data>, Data>() {
            @Override
            public TimeModeBin<Data> createBin(int index) {
                int numberOfTimeBins = (int) Math.ceil((end - start) / interval);
                
                int modeIndex = index / numberOfTimeBins;
                int timeIndex = index - numberOfTimeBins * modeIndex;
                
                String mode = modes.get(modeIndex);
                double binStart = start + interval * timeIndex;
                
                return new TimeModeBin<Data>(binStart, binStart + interval, mode, factory.createData());
            }
        });
        
        this.modes.addAll(modes);
        this.numberOfTimeBins = (int) Math.ceil((end - start) / interval);
        
        this.start = start;
        this.end = end;
        this.interval = interval;
    }
    
    public int getIndexByIndices(int modeIndex, int timeIndex) {
        if (timeIndex < 0) return -1;
        if (modeIndex < 0) return -1;
        if (timeIndex >= numberOfTimeBins) return -1;
        
        return modeIndex * numberOfTimeBins + timeIndex;
    }
    
    public int getIndexByModeAndTime(String mode, double time) {
        return getIndexByIndices(modes.indexOf(mode), (int) Math.floor((time - start)/ interval));
    }
    
    public List<String> getModes() {
        return modes;
    }
    
    public int getNumberOfTimeBins() {
        return numberOfTimeBins;
    }
    
    public TimeModeBin<Data> getBinByModeAndTime(String mode, double time) {
        return getBinByIndex(getIndexByModeAndTime(mode, time));
    }
    
    public TimeModeBin<Data> getBinByModeAndTimeIndex(String mode, int index) {
        return getBinByIndex(getIndexByIndices(modes.indexOf(mode), index));
    }
    
    public Iterator<TimeBin<Data>> iterator(String mode) {
        final int modeIndex = modes.indexOf(mode);
        
        return new Iterator<TimeBin<Data>>() {
            private int timeIndex = 0;
            
            @Override
            public boolean hasNext() {
                return timeIndex < numberOfTimeBins;
            }

            @Override
            public TimeBin<Data> next() {
                TimeModeBin<Data> bin = getBinByIndex(getIndexByIndices(modeIndex, timeIndex));
                TimeBin<Data> timeBin = new TimeBin<Data>(bin.getStart(), bin.getEnd(), bin.getData());
                timeIndex++;
                return timeBin;
            }
        };
    }
    
    public double getInterval() {
        return interval;
    }
}

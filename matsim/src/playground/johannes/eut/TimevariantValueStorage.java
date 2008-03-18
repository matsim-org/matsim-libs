package playground.johannes.eut;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;


/**
 * 
 * @author illenberger
 * 
 */
public class TimevariantValueStorage {
	
	private static final Logger logger = Logger.getLogger(TimevariantValueStorage.class);

    // -------------------- MEMBER VARIABLES --------------------

    private final int startTime;

    private final int endTime;

    private final int binSize;

    private final int binCnt;
    
    private final Map<BasicLinkI, double[]> valueMap = new LinkedHashMap<BasicLinkI, double[]>();
//    private final Map<BasicLinkI, double[]> valueMap = new THashMap<BasicLinkI, double[]>();

    // -------------------- CONSTRUCTION --------------------

    public TimevariantValueStorage(int startTime, int endTime, int binSize) {
        if (startTime < 0 || endTime < startTime || binSize <= 0)
            throw new IllegalArgumentException("startTime=" + startTime
                    + "s, endTime=" + endTime + "s, binSize=" + binSize
                    + "s.");

        this.startTime = startTime;
        this.endTime = endTime;
        this.binSize = binSize;

        this.binCnt = (endTime - startTime) / binSize;
    }

    // -------------------- GETTERS --------------------

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public int getBinSize() {
        return binSize;
    }

    protected int getBinCnt() {
        return binCnt;
    }

    protected int getBin(int time) {
        return (time - startTime) / binSize;
    }
    
    protected double getBinValue(BasicLinkI link, int bin) {
    	double[] values = valueMap.get(link);
    	if(values == null)
    		return 0;
    	else
    		return values[bin];
    }
    
    protected void setBinValue(BasicLinkI link, int bin, double value) {
		double[] values = valueMap.get(link);
		
		if (values == null) {
			/*
			 * No value has bee stored for this link yet.
			 */
			if (value == 0) {
				/*
				 * Do not store zero values.
				 */
				return;
			} else {
				/*
				 * Initialize new value array.
				 */
				values = new double[getBinCnt()];
				Arrays.fill(values, 0);
				valueMap.put(link, values);
			}
		}

		values[bin] = value;
	}
    
    public double getValue(BasicLinkI link, int time) {
    	return getBinValue(link, getBin(time));
    }

    public void setValue(BasicLinkI link, int time, double value) {
    	setBinValue(link, getBin(time), value);
    }
    
    // -------------------- MISC --------------------

    public void accumulate(TimevariantValueStorage storage, double weight) {
    	if(isCompatibleWith(storage)) {
    		double oldWeigth = 1 - weight;
    		for(BasicLinkI link : valueMap.keySet()) {
    			double[] newValues = storage.valueMap.get(link);
    			if(newValues != null) {
    				double[] oldValues = valueMap.get(link);
    				if(oldValues == null) {
    					oldValues = new double[getBinCnt()];
    					Arrays.fill(oldValues, 0);
    					valueMap.put(link, oldValues);
    				}
    				for(int i = 0; i < getBinCnt(); i++) {
    					oldValues[i] = oldWeigth*oldValues[i] + weight*newValues[i];
    				}
    			}
    		}
    	} else {
    		logger.warn("Incompatibel storage objects!");
    	}
    }
    public boolean isCompatibleWith(TimevariantValueStorage other) {
        return (this.startTime == other.startTime
                && this.endTime == other.endTime
                && this.binSize == other.binSize && this.binCnt == other.binCnt);
    }

    public String toString() {
    	StringBuilder builder = new StringBuilder();
        for(BasicLinkI link : valueMap.keySet()) {
        	double[] values = valueMap.get(link);
        	builder.append(link.getId().toString());
        	for(int i = 420; i < 450; i++) {
        		builder.append("\t");
        		builder.append(String.valueOf(values[i]));
        	}
        	builder.append("\n");
        }
        
        return builder.toString();
    }
}

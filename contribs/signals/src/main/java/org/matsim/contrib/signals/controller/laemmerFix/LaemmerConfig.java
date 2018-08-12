package org.matsim.contrib.signals.controller.laemmerFix;

import java.util.HashMap;
import java.util.Map;

//import com.sun.istack.internal.NotNull;
//import com.sun.istack.internal.Nullable;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.Lane;

/**
 * @author nkuehnel, tthunig
 */
public final class LaemmerConfig {
	
    //Probably consider to try also a combination of simmilar outflow rates
    public enum StabilizationStrategy {USE_MAX_LANECOUNT, PRIORIZE_HIGHER_POSITIONS, COMBINE_SIMILAR_REGULATIONTIME, HEURISTIC}; 
    private StabilizationStrategy activeStabilizationStrategy = StabilizationStrategy.HEURISTIC;
    
    public enum Regime {COMBINED, OPTIMIZING, STABILIZING};
    private Regime activeRegime = Regime.COMBINED;

    private double maxCycleTime = 180;
    private double desiredCycleTime = 120;

    private double defaultIntergreenTime = 5.0;
    private double minGreenTime = 5.0;
    
    //size of timeBuckets for LaneSensor and LinkSensor
    private double timeBucketSize = Double.POSITIVE_INFINITY; //15.0; 5.0*60.0; 1.5*60.0;  
    //lookBackTime for LaneSensor and LinkSensor
    private double lookBackTime = Double.POSITIVE_INFINITY; //300.0; 15.0*60.0; //6.0*60.0;

    private Map<Id<Link>, Double> linkArrivalRates = new HashMap<>();
    private Map<Id<Link>, Map<Id<Lane>,Double>> laneArrivalRates = new HashMap<>();

    private boolean useDefaultIntergreenTime = true;
    
	/** activate the phase only if downstream links are empty. */
	private boolean checkDownstream = false;

	private boolean isRemoveSubPhases = true;
	
	/** if there exist queues but arrivalRate for this link/lane is 0, the queue will never emptied.
	 * with tMinForNotGrowingQueues setten to true it will be get at least the minium green time
	 */
	private boolean minGreenTimeForNonGrowingQueues = true;
	
	/** tIdle is determined by the highest determinedLoad of the current regulationPhase and the highest determinedLoad of the top n flows not in the regulationPhase,
	 * where n is the number of estimatedPhases minus 1. By default the flows are grouped by their signals beforehand, keeping only the flow with
	 * the highest load per signal. This prevents that similar flows on two lanes are counted twice, since you can assume that they will be signalized together.
	 * However, in some cases a flow can be signalized by two signals and you can probably not trust on the prerequisite, that they will be signalized together.
	 * An example for such a case would be right-turning flows, which can be signalized for some time alone and for some other time together with the straight flow.
	 * If your network includes such cases, you need set this to false. As a side-effect from setting it to false will be a under-estimate of tIdle in some cases.  
	 */
	private boolean determineMaxLoadForTIdleGroupedBySignals = true;

	/**
	 * if a signals showed green during intergreenTime, stabilizationtime will be reduced after intergreenTime
	 */
	private boolean shortenStabilizationTimeAfterIntergreenTime = true;

    //    @Nullable
    public Double getLaneArrivalRate(Id<Link> linkId, Id<Lane> laneId) {
        if(laneArrivalRates.containsKey(linkId)) {
            return this.laneArrivalRates.get(linkId).get(laneId);
        } else {
            return null;
        }
    }

    public double getMinGreenTime() {
        return minGreenTime;
    }

    public void setMinGreenTime(double minGreenTime) {
        this.minGreenTime = minGreenTime;
    }
    
    public StabilizationStrategy getActiveStabilizationStrategy() {
		return activeStabilizationStrategy;
	}

	public void setActiveStabilizationStrategy(StabilizationStrategy activeStabilizationStrategy) {
		this.activeStabilizationStrategy = activeStabilizationStrategy;
	}

	public Regime getActiveRegime() {
        return activeRegime;
    }

    public void setActiveRegime(Regime activeRegime) {
        this.activeRegime = activeRegime;
    }

    public void addArrivalRateForLink(Id<Link> linkId, double arrivalRate) {
        this.linkArrivalRates.put(linkId, arrivalRate);
    }

//    @Nullable
    public Double getLinkArrivalRate(Id<Link> linkId) {
        return linkArrivalRates.get(linkId);
    }

    public void addArrivalRateForLane(Id<Link> linkId, Id<Lane> laneId, double arrivalRate) {
        if(!this.laneArrivalRates.containsKey(linkId)) {
            this.laneArrivalRates.put(linkId, new HashMap<>());
        }
        this.laneArrivalRates.get(linkId).put(laneId, arrivalRate);
    }

    public double getMaxCycleTime() {
        return maxCycleTime;
    }

    public void setMaxCycleTime(double maxCycleTime) {
        this.maxCycleTime = maxCycleTime;
    }

    public double getDesiredCycleTime() {
        return desiredCycleTime;
    }

    public void setDesiredCycleTime(double desiredCycleTime) {
        this.desiredCycleTime = desiredCycleTime;
    }

    public boolean isUseDefaultIntergreenTime() {
        return useDefaultIntergreenTime;
    }

    public void setUseDefaultIntergreenTime(boolean useDefaulttIntergreenTime) {
        this.useDefaultIntergreenTime = useDefaulttIntergreenTime;
    }

    public double getDefaultIntergreenTime() {
        return defaultIntergreenTime;
    }

    public void setDefaultIntergreenTime(double intergreen) {
        this.defaultIntergreenTime = intergreen;
    }
	
	public void setCheckDownstream(boolean checkDownstream) {
		this.checkDownstream = checkDownstream;
	}
	
	public boolean isCheckDownstream() {
		return checkDownstream;
	}
	
	public double getTimeBucketSize() {
		return timeBucketSize;
	}

	public double getLookBackTime() {
		return lookBackTime;
	}

	/**
	 * If true subphases will be removed from the set of phases. Phase A is a
	 * subphase of phase B it the set of SignalGroups of A if fully covered by the
	 * set of SignalGroups of B.
	 * 
	 * @return true if subphases should removed of false if subphases should be kept
	 */
	public boolean isRemoveSubPhases() {
		return isRemoveSubPhases;
	}

	public StabilizationStrategy getStabilizationStrategy() {
		return activeStabilizationStrategy;
	}

	public boolean isMinGreenTimeForNonGrowingQueues() {
		return minGreenTimeForNonGrowingQueues;
	}

	public void setMinGreenTimeForNonGrowingQueues(boolean minGreenTimeForNonGrowingQueues) {
		this.minGreenTimeForNonGrowingQueues = minGreenTimeForNonGrowingQueues;
	}

	public boolean isDetermineMaxLoadForTIdleGroupedBySignals() {
		return determineMaxLoadForTIdleGroupedBySignals;
	}

	public void setDetermineMaxLoadForTIdleGroupedBySignals(boolean maxLoadForTIdleGroupedBySignals) {
		this.determineMaxLoadForTIdleGroupedBySignals = maxLoadForTIdleGroupedBySignals;
	}
	
	/**
	 * Configure the sensor for live arrival rates. Live arrival Rate can be calculated from the time, the first car entered the Link/Lane until now or by getting an average from lookBackTime.
	 * If using last option, lookBackTime will be splitted in buckets with timeBucketsSize. Only finished buckets will be used to calculate the average.
	 * @param lookBackTime For which duration of passed time the average should be calculated. Set to Double.POSITIVE_INFINITY to calculate from time the first car enters the link on. 
	 * @param timeBucketSize Resolution of lookBackTime. Average is calculated only with full time buckets. Set to Double.POSITIVE_INFINITY to calculate from time the first car enters the link on. 
	 */
	public void setAvgCarSensorBucketParameters(double lookBackTime, double timeBucketSize) {
		this.lookBackTime = lookBackTime;
		this.timeBucketSize = timeBucketSize;
	}
	
	/**
	 * If signals showed green during the intergreenTime, there regulation time can be shorten after intergreenTime
	 * @return true if regulationTime should be shorten, false if not.
	 */
	public boolean getShortenStabilizationAfterIntergreenTime() {
		return shortenStabilizationTimeAfterIntergreenTime;
	}
	
	public void setShortenStabilizationAfterIntergreenTime(boolean value) {
		this.shortenStabilizationTimeAfterIntergreenTime = value;
	}
	
	
}

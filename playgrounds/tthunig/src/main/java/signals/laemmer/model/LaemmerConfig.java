package signals.laemmer.model;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.lanes.data.Lane;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nkuehnel on 03.04.2017.
 */
public class LaemmerConfig {

    private double MAX_PERIOD = 120;
    private double DESIRED_PERIOD = 70;

    private Map<Id<Signal>, Double> signalArrivalRates = new HashMap<>();
    private Map<Id<Signal>, Map<Id<Lane>,Double>> laneArrivalRates = new HashMap<>();

    private boolean useBasicIntergreenTime = true;

    private double DEFAULZT_INTERGREEN = 5;

    private boolean analysisEnabled = true;

    public Map<Id<Lane>, Double> getLaneArrivalRates(Id<Signal> signalId) {
        return this.laneArrivalRates.get(signalId);
    }

    public enum Regime {COMBINED, OPTIMIZING, STABILIZING};

    public Regime getActiveRegime() {
        return activeRegime;
    }

    public void setActiveRegime(Regime activeRegime) {
        this.activeRegime = activeRegime;
    }

    private Regime activeRegime = Regime.COMBINED;

    public void addArrivalRateForSignal(Id<Signal> signalId, double arrivalRate) {
        signalArrivalRates.put(signalId, arrivalRate);
    }

    public Double getSignalArrivalRate(Id<Signal> signalId) {
        return signalArrivalRates.get(signalId);
    }

    public void addArrivalRateForSignalLane(Id<Signal> signalId, Id<Lane> laneId, double arrivalRate) {
        if(!this.laneArrivalRates.containsKey(signalId)) {
            this.laneArrivalRates.put(signalId, new HashMap<>());
        }
        this.laneArrivalRates.get(signalId).put(laneId, arrivalRate);
    }

    public double getMAX_PERIOD() {
        return MAX_PERIOD;
    }

    public void setMAX_PERIOD(double MAX_PERIOD) {
        this.MAX_PERIOD = MAX_PERIOD;
    }

    public double getDESIRED_PERIOD() {
        return DESIRED_PERIOD;
    }

    public void setDESIRED_PERIOD(double DESIRED_PERIOD) {
        this.DESIRED_PERIOD = DESIRED_PERIOD;
    }

    public boolean isUseBasicIntergreenTime() {
        return useBasicIntergreenTime;
    }

    public void setUseBasicIntergreenTime(boolean useBasicIntergreenTime) {
        this.useBasicIntergreenTime = useBasicIntergreenTime;
    }

    public double getDEFAULZT_INTERGREEN() {
        return DEFAULZT_INTERGREEN;
    }

    public void setDEFAULZT_INTERGREEN(double DEFAULZT_INTERGREEN) {
        this.DEFAULZT_INTERGREEN = DEFAULZT_INTERGREEN;
    }


    public boolean analysisEnabled() {
        return analysisEnabled;
    }


}

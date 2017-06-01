package signals.laemmer.model;

//import com.sun.istack.internal.NotNull;
//import com.sun.istack.internal.Nullable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.Lane;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nkuehnel on 03.04.2017.
 */
public class LaemmerConfig {

    private double MAX_PERIOD = 120;
    private double DESIRED_PERIOD = 70;

    private Map<Id<Link>, Double> linkArrivalRates = new HashMap<>();
    private Map<Id<Link>, Map<Id<Lane>,Double>> laneArrivalRates = new HashMap<>();

    private boolean useBasicIntergreenTime = true;

    private double DEFAULT_INTERGREEN = 5;

    private boolean analysisEnabled = true;
    private double MIN_G = 0;

    //    @Nullable
    public Double getLaneArrivalRate(Id<Link> linkId, Id<Lane> laneId) {
        if(laneArrivalRates.containsKey(linkId)) {
            return this.laneArrivalRates.get(linkId).get(laneId);
        } else {
            return null;
        }
    }

    public double getMinG() {
        return MIN_G;
    }

    public void setMinG(double minG) {
        MIN_G = minG;
    }

    public enum Regime {COMBINED, OPTIMIZING, STABILIZING};

    public Regime getActiveRegime() {
        return activeRegime;
    }

    public void setActiveRegime(Regime activeRegime) {
        this.activeRegime = activeRegime;
    }

    private Regime activeRegime = Regime.COMBINED;

    public void addArrivalRateForLink(Id<Link> linkId, double arrivalRate) {
        linkArrivalRates.put(linkId, arrivalRate);
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

    public double getDEFAULT_INTERGREEN() {
        return DEFAULT_INTERGREEN;
    }

    public void setDEFAULT_INTERGREEN(double DEFAULZT_INTERGREEN) {
        this.DEFAULT_INTERGREEN = DEFAULZT_INTERGREEN;
    }


    public boolean analysisEnabled() {
        return analysisEnabled;
    }


}

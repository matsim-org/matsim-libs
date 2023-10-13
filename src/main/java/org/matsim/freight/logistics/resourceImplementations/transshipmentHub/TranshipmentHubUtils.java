package org.matsim.freight.logistics.resourceImplementations.transshipmentHub;

import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPResourceScheduler;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class TranshipmentHubUtils {
    public static class TranshipmentHubSchedulerBuilder {
        private double capacityNeedLinear;
        private double capacityNeedFixed;

        private TranshipmentHubSchedulerBuilder() {
        }

        public static TranshipmentHubSchedulerBuilder newInstance() {
            return new TranshipmentHubSchedulerBuilder();
        }

        public TranshipmentHubSchedulerBuilder setCapacityNeedLinear(double capacityNeedLinear) {
            this.capacityNeedLinear = capacityNeedLinear;
            return this;
        }

        public TranshipmentHubSchedulerBuilder setCapacityNeedFixed(double capacityNeedFixed) {
            this.capacityNeedFixed = capacityNeedFixed;
            return this;
        }

        public TransshipmentHubScheduler build() {
            return new TransshipmentHubScheduler(this);
        }

        //--- Getters ---

        double getCapacityNeedLinear() {
            return capacityNeedLinear;
        }

        double getCapacityNeedFixed() {
            return capacityNeedFixed;
        }
    }

    public static final class TransshipmentHubBuilder {

        private final Id<LSPResource> id;
        private final Id<Link> locationLinkId;
        private final ArrayList<LogisticChainElement> clientElements;
        private TransshipmentHubScheduler transshipmentHubScheduler;
        private final Scenario scenario;

        private TransshipmentHubBuilder(Id<LSPResource> id, Id<Link> locationLinkId, Scenario scenario) {
            this.id = id;
            this.clientElements = new ArrayList<>();
            this.locationLinkId = locationLinkId;
            this.scenario = scenario;
        }

        public static TransshipmentHubBuilder newInstance(Id<LSPResource> id, Id<Link> locationLinkId, Scenario scenario) {
            return new TransshipmentHubBuilder(id, locationLinkId, scenario);
        }

        public TransshipmentHubBuilder setTransshipmentHubScheduler(LSPResourceScheduler TranshipmentHubScheduler) {
            this.transshipmentHubScheduler = (TransshipmentHubScheduler) TranshipmentHubScheduler;
            return this;
        }

        public TransshipmentHubResource build() {
            return new TransshipmentHubResource(this, scenario);
        }
        //--- Getters ---

        Id<LSPResource> getId() {
            return id;
        }

        Id<Link> getLocationLinkId() {
            return locationLinkId;
        }

        TransshipmentHubScheduler getTransshipmentHubScheduler() {
            return transshipmentHubScheduler;
        }

        ArrayList<LogisticChainElement> getClientElements() {
            return clientElements;
        }

    }
}

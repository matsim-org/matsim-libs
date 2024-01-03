package org.matsim.freight.logistics.resourceImplementations.transshipmentHub;

import java.util.ArrayList;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPResourceScheduler;
import org.matsim.freight.logistics.LogisticChainElement;

/**
 * @author Kai Martins-Turner (kturner)
 */
public class TranshipmentHubUtils {
  public static class TranshipmentHubSchedulerBuilder {
    private double capacityNeedLinear;
    private double capacityNeedFixed;

    private TranshipmentHubSchedulerBuilder() {}

    public static TranshipmentHubSchedulerBuilder newInstance() {
      return new TranshipmentHubSchedulerBuilder();
    }

    public TransshipmentHubScheduler build() {
      return new TransshipmentHubScheduler(this);
    }

    double getCapacityNeedLinear() {
      return capacityNeedLinear;
    }

    public TranshipmentHubSchedulerBuilder setCapacityNeedLinear(double capacityNeedLinear) {
      this.capacityNeedLinear = capacityNeedLinear;
      return this;
    }

    // --- Getters ---

    double getCapacityNeedFixed() {
      return capacityNeedFixed;
    }

    public TranshipmentHubSchedulerBuilder setCapacityNeedFixed(double capacityNeedFixed) {
      this.capacityNeedFixed = capacityNeedFixed;
      return this;
    }
  }

  public static final class TransshipmentHubBuilder {

    private final Id<LSPResource> id;
    private final Id<Link> locationLinkId;
    private final ArrayList<LogisticChainElement> clientElements;
    private final Scenario scenario;
    private TransshipmentHubScheduler transshipmentHubScheduler;

    private TransshipmentHubBuilder(
        Id<LSPResource> id, Id<Link> locationLinkId, Scenario scenario) {
      this.id = id;
      this.clientElements = new ArrayList<>();
      this.locationLinkId = locationLinkId;
      this.scenario = scenario;
    }

    public static TransshipmentHubBuilder newInstance(
        Id<LSPResource> id, Id<Link> locationLinkId, Scenario scenario) {
      return new TransshipmentHubBuilder(id, locationLinkId, scenario);
    }

    public TransshipmentHubResource build() {
      return new TransshipmentHubResource(this, scenario);
    }

    Id<LSPResource> getId() {
      return id;
    }

    // --- Getters ---

    Id<Link> getLocationLinkId() {
      return locationLinkId;
    }

    TransshipmentHubScheduler getTransshipmentHubScheduler() {
      return transshipmentHubScheduler;
    }

    public TransshipmentHubBuilder setTransshipmentHubScheduler(
        LSPResourceScheduler TranshipmentHubScheduler) {
      this.transshipmentHubScheduler = (TransshipmentHubScheduler) TranshipmentHubScheduler;
      return this;
    }

    ArrayList<LogisticChainElement> getClientElements() {
      return clientElements;
    }
  }
}

/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.shipment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.carriers.TimeWindow;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LogisticChainElement;

/**
 * Utility methods and builders related to {@link LspShipment} and the elements of a
 * {@link LspShipmentPlan}. The builders produce the concrete plan elements
 * ({@code ScheduledLspShipment*} and {@code LoggedLspShipment*}); the helper methods store timing
 * attributes on a shipment.
 */
public final class LspShipmentUtils {
  private LspShipmentUtils() {} // do not instantiate

  /** @return a comparator ordering shipment plan elements by start, then end time. */
  public static Comparator<LspShipmentPlanElement> createShipmentPlanElementComparator() {
    return new ShipmentPlanElementComparator();
  }

  /**
   * Returns the {@link LspShipmentPlan} of the {@link LSPPlan} that matches the given shipment id,
   * creating and adding one if it does not yet exist.
   *
   * @param lspPlan the plan in which the shipment plan is searched fo
   * @param shipmentId id of the shipment for which the shipment plan is looked up
   * @return the shipment plan, or {@code null} if it is not found
   */
  public static LspShipmentPlan getOrCreateShipmentPlan(LSPPlan lspPlan, Id<LspShipment> shipmentId) {
    // Return shipmentPlan if already existing in LspPlan
    for (LspShipmentPlan lspShipmentPlan : lspPlan.getShipmentPlans()) {
      if (lspShipmentPlan.getLspShipmentId().equals(shipmentId)) {
        return lspShipmentPlan;
      }
    }
    // ShipmentPlan does not exist in LspPlan. Will create one, add it to the LspPlan.
    LspShipmentPlan newLspShipmentPlan = new LspShipmentPlanImpl(shipmentId);
    lspPlan.addShipmentPlan(newLspShipmentPlan);
    return newLspShipmentPlan;
  }

  /**
   * Stores a time as an attribute of the {@link LspShipment}, e.g. for tracking the shipment.
   * <p>
   * This will replace the LSPShipmentWithTime class and thus reduce the complexity of the code.
   * KMT Jul'24
   *
   * @param lspShipment the shipment to store the time in
   * @param time the time to store
   */
  public static void setTimeOfLspShipment(LspShipment lspShipment, double time){
    lspShipment.getAttributes().putAttribute("time", time);
  }

  /**
   * Returns the time stored in the {@link LspShipment} by {@link #setTimeOfLspShipment(LspShipment, double)}.
   * <p>
   * This will replace the LSPShipmentWithTime class and thus reduce the complexity of the code. KMT Jul'24
   * @param lspShipment the shipment to read the time from
   * @return the stored time as a double
   */
  public static double getTimeOfLspShipment(LspShipment lspShipment) {
    return (double) lspShipment.getAttributes().getAttribute("time");
  }

  /** Builder for {@link LspShipment} instances. */
  public static final class LspShipmentBuilder {
    final Id<LspShipment> id;
    final List<LspShipmentRequirement> lspShipmentRequirements;
    Id<Link> fromLinkId;
    Id<Link> toLinkId;
    TimeWindow startTimeWindow;
    TimeWindow endTimeWindow;
    int capacityDemand;
    double deliveryServiceTime;
    double pickupServiceTime;

    private LspShipmentBuilder(Id<LspShipment> id) {
      this.lspShipmentRequirements = new ArrayList<>();
      this.id = id;
    }

    public static LspShipmentBuilder newInstance(Id<LspShipment> id) {
      return new LspShipmentBuilder(id);
    }

    public void setFromLinkId(Id<Link> fromLinkId) {
      this.fromLinkId = fromLinkId;
    }

    public void setToLinkId(Id<Link> toLinkId) {
      this.toLinkId = toLinkId;
    }

    public void setStartTimeWindow(TimeWindow startTimeWindow) {
      this.startTimeWindow = startTimeWindow;
    }

    public void setEndTimeWindow(TimeWindow endTimeWindow) {
      this.endTimeWindow = endTimeWindow;
    }

    public void setCapacityDemand(int capacityDemand) {
      this.capacityDemand = capacityDemand;
    }

    public void setDeliveryServiceTime(double serviceTime) {
      this.deliveryServiceTime = serviceTime;
    }

    public LspShipmentBuilder setPickupServiceTime(double serviceTime) {
      this.pickupServiceTime = serviceTime;
      return this;
    }

    public void addRequirement(LspShipmentRequirement requirement) {
      lspShipmentRequirements.add(requirement);
    }

    public LspShipment build() {
      return new LspShipmentImpl(this);
    }
  }

  //-----------------------------
  // LoggedShipment<..> builders
  // Consider changing this Logged<...> ShipmentPlanElement to MATSim's experienced plans.
  //This would be closer to MATSim and makes clear that this is what happened in the simulation. kmt/kn jan'25
  //-----------------------------

  /** Builds {@code LoggedLspShipmentLoad} instances from a shipment's logged data. */
  @SuppressWarnings("ClassEscapesDefinedScope")
  public static final class LoggedShipmentLoadBuilder {
    private double startTime;
    private double endTime;
    private LogisticChainElement element;
    private Id<LSPResource> resourceId;
    private Id<Carrier> carrierId;
    private Id<Link> linkId;

    private LoggedShipmentLoadBuilder() {}

    public static LoggedShipmentLoadBuilder newInstance() {
      return new LoggedShipmentLoadBuilder();
    }

    public void setLogisticsChainElement(LogisticChainElement element) {
      this.element = element;
    }

    public LoggedLspShipmentLoad build() {
      return new LoggedLspShipmentLoad(this);
    }

    public double getStartTime() {
      return startTime;
    }

    public void setStartTime(double startTime) {
      this.startTime = startTime;
    }

    public double getEndTime() {
      return endTime;
    }

    public void setEndTime(double endTime) {
      this.endTime = endTime;
    }

    public LogisticChainElement getElement() {
      return element;
    }

    // --- Getters --- //

    public Id<LSPResource> getResourceId() {
      return resourceId;
    }

    public void setResourceId(Id<LSPResource> resourceId) {
      this.resourceId = resourceId;
    }

    public Id<Carrier> getCarrierId() {
      return carrierId;
    }

    public void setCarrierId(Id<Carrier> carrierId) {
      this.carrierId = carrierId;
    }

    public Id<Link> getLinkId() {
      return linkId;
    }

    public void setLinkId(Id<Link> linkId) {
      this.linkId = linkId;
    }
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  public static final class LoggedShipmentTransportBuilder {
    private double startTime;
    private LogisticChainElement element;
    private Id<LSPResource> resourceId;
    private Id<Link> fromLinkId;
    private Id<Link> toLinkId;
    private Id<Carrier> carrierId;

    private LoggedShipmentTransportBuilder() {}

    public static LoggedShipmentTransportBuilder newInstance() {
      return new LoggedShipmentTransportBuilder();
    }

    public void setLogisticChainElement(LogisticChainElement element) {
      this.element = element;
    }

    public LoggedLspShipmentTransport build() {
      return new LoggedLspShipmentTransport(this);
    }

    // --- Getters --- //
    public double getStartTime() {
      return startTime;
    }

    public void setStartTime(double startTime) {
      this.startTime = startTime;
    }

    public LogisticChainElement getElement() {
      return element;
    }

    public Id<LSPResource> getResourceId() {
      return resourceId;
    }

    public void setResourceId(Id<LSPResource> resourceId) {
      this.resourceId = resourceId;
    }

    public Id<Link> getFromLinkId() {
      return fromLinkId;
    }

    public void setFromLinkId(Id<Link> fromLinkId) {
      this.fromLinkId = fromLinkId;
    }

    public Id<Link> getToLinkId() {
      return toLinkId;
    }

    public void setToLinkId(Id<Link> toLinkId) {
      this.toLinkId = toLinkId;
    }

    public Id<Carrier> getCarrierId() {
      return carrierId;
    }

    public void setCarrierId(Id<Carrier> carrierId) {
      this.carrierId = carrierId;
    }
  }

  /** Builds {@code LoggedLspShipmentUnload} instances from a shipment's logged data. */
  @SuppressWarnings("ClassEscapesDefinedScope")
  public static final class LoggedShipmentUnloadBuilder {
    double startTime;
    double endTime;
    LogisticChainElement element;
    Id<LSPResource> resourceId;
    Id<Carrier> carrierId;
    Id<Link> linkId;

    private LoggedShipmentUnloadBuilder() {}

    public static LoggedShipmentUnloadBuilder newInstance() {
      return new LoggedShipmentUnloadBuilder();
    }

    public void setStartTime(double startTime) {
      this.startTime = startTime;
    }

    public void setEndTime(double endTime) {
      this.endTime = endTime;
    }

    public void setLogisticChainElement(LogisticChainElement element) {
      this.element = element;
    }

    public void setResourceId(Id<LSPResource> resourceId) {
      this.resourceId = resourceId;
    }

    public void setLinkId(Id<Link> linkId) {
      this.linkId = linkId;
    }

    public void setCarrierId(Id<Carrier> carrierId) {
      this.carrierId = carrierId;
    }

    public LoggedLspShipmentUnload build() {
      return new LoggedLspShipmentUnload(this);
    }
  }

  /** Builds {@code LoggedLspShipmentHandling} instances from a shipment's logged data. */
  public static final class LoggedShipmentHandleBuilder {
    double startTime;
    double endTime;
    LogisticChainElement element;
    Id<LSPResource> resourceId;
    Id<Link> linkId;

    private LoggedShipmentHandleBuilder() {}

    public static LoggedShipmentHandleBuilder newInstance() {
      return new LoggedShipmentHandleBuilder();
    }

    public LoggedShipmentHandleBuilder setStartTime(double startTime) {
      this.startTime = startTime;
      return this;
    }

    public LoggedShipmentHandleBuilder setEndTime(double endTime) {
      this.endTime = endTime;
      return this;
    }

    public LoggedShipmentHandleBuilder setLogisticsChainElement(LogisticChainElement element) {
      this.element = element;
      return this;
    }

    public LoggedShipmentHandleBuilder setResourceId(Id<LSPResource> resourceId) {
      this.resourceId = resourceId;
      return this;
    }

    public LoggedShipmentHandleBuilder setLinkId(Id<Link> linkId) {
      this.linkId = linkId;
      return this;
    }

    public LspShipmentPlanElement build() {
      return new LoggedLspShipmentHandling(this);
    }
  }

  // ---- ScheduledShipment<..> builders ----
  // These produce the elements recorded in the pre-simulation shipment plan.

  /** Builds {@code ScheduledLspShipmentLoad} instances from a shipment's pre-simulation load plan. */
  @SuppressWarnings("ClassEscapesDefinedScope")
  public static final class ScheduledShipmentLoadBuilder {
    double startTime;
    double endTime;
    LogisticChainElement element;
    Id<LSPResource> resourceId;
	Id<Link> linkId;

    private ScheduledShipmentLoadBuilder() {}

    public static ScheduledShipmentLoadBuilder newInstance() {
      return new ScheduledShipmentLoadBuilder();
    }

    public void setStartTime(double startTime) {
      this.startTime = startTime;
    }

    public void setEndTime(double endTime) {
      this.endTime = endTime;
    }

	public void setLinkId(Id<Link> linkId) {this.linkId = linkId;}

    public void setLogisticChainElement(LogisticChainElement element) {
      this.element = element;
    }

    public void setResourceId(Id<LSPResource> resourceId) {
      this.resourceId = resourceId;
    }

    public ScheduledLspShipmentLoad build() {
      return new ScheduledLspShipmentLoad(this);
    }
  }

  /** Builds {@code ScheduledLspShipmentTransport} instances from a pre-simulation transport plan. */
  @SuppressWarnings("ClassEscapesDefinedScope")
  public static final class ScheduledShipmentTransportBuilder {
    double startTime;
    double endTime;
    LogisticChainElement element;
    Id<LSPResource> resourceId;
    Id<Carrier> carrierId;
    Id<Link> fromLinkId;
    Id<Link> toLinkId;
    CarrierService carrierService;
    CarrierShipment carrierShipment; //TODO: Put CarrierShipment and CarrieTask behind one interface and use that here (CarrierTask...)

    private ScheduledShipmentTransportBuilder() {}

    public static ScheduledShipmentTransportBuilder newInstance() {
      return new ScheduledShipmentTransportBuilder();
    }

    public void setStartTime(double startTime) {
      this.startTime = startTime;
    }

    public void setEndTime(double endTime) {
      this.endTime = endTime;
    }

    public void setLogisticChainElement(LogisticChainElement element) {
      this.element = element;
    }

    public void setResourceId(Id<LSPResource> resourceId) {
      this.resourceId = resourceId;
    }

    public void setCarrierId(Id<Carrier> carrierId) {
      this.carrierId = carrierId;
    }

    public void setFromLinkId(Id<Link> fromLinkId) {
      this.fromLinkId = fromLinkId;
    }

    public void setToLinkId(Id<Link> toLinkId) {
      this.toLinkId = toLinkId;
    }

    public void setCarrierService(CarrierService carrierService) {
      this.carrierService = carrierService;
    }

    public void setCarrierShipment(CarrierShipment carrierShipment) {
      this.carrierShipment = carrierShipment;
    }

    public ScheduledLspShipmentTransport build() {
      return new ScheduledLspShipmentTransport(this);
    }
  }

  /** Builds {@code ScheduledLspShipmentUnload} instances from a pre-simulation unload plan. */
  @SuppressWarnings("ClassEscapesDefinedScope")
  public static final class ScheduledShipmentUnloadBuilder {
    double startTime;
    double endTime;
    LogisticChainElement element;
    Id<LSPResource> resourceId;
	Id<Link> linkId;

    private ScheduledShipmentUnloadBuilder() {}

    public static ScheduledShipmentUnloadBuilder newInstance() {
      return new ScheduledShipmentUnloadBuilder();
    }

    public void setStartTime(double startTime) {
      this.startTime = startTime;
    }

    public void setEndTime(double endTime) {
      this.endTime = endTime;
    }

    public void setLogisticsChainElement(LogisticChainElement element) {
      this.element = element;
    }

    public void setResourceId(Id<LSPResource> resourceId) {
      this.resourceId = resourceId;
    }

	public void setLinkId(Id<Link> linkId) {
		  this.linkId = linkId;
	  }

    public ScheduledLspShipmentUnload build() {
      return new ScheduledLspShipmentUnload(this);
    }
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  public static final class ScheduledShipmentHandleBuilder {
    double startTime;
    double endTime;
    LogisticChainElement element;
    Id<LSPResource> resourceId;
	Id<Link> linkId;

    private ScheduledShipmentHandleBuilder() {}

    public static ScheduledShipmentHandleBuilder newInstance() {
      return new ScheduledShipmentHandleBuilder();
    }

    public void setStartTime(double startTime) {
      this.startTime = startTime;
    }

    public void setEndTime(double endTime) {
      this.endTime = endTime;
    }

    public void setLogisticsChainElement(LogisticChainElement element) {
      this.element = element;
    }

    public void setResourceId(Id<LSPResource> resourceId) {
      this.resourceId = resourceId;
    }

	public void setLinkId(Id<Link> linkId) {
		  this.linkId = linkId;
	  }

    public ScheduledLspShipmentHandling build() {
      return new ScheduledLspShipmentHandling(this);
    }
  }

}

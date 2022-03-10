package lsp.shipment;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.core.events.handler.EventHandler;

import lsp.LogisticsSolution;
import lsp.functions.LSPInfo;

public class LSPShipmentImpl implements LSPShipment {

	private final Id<LSPShipment> id;
	private final Id<Link> fromLinkId;
	private final Id<Link> toLinkId;
	private final TimeWindow startTimeWindow;
	private final TimeWindow endTimeWindow;
	private final int capacityDemand;
	private final double deliveryServiceTime;
	private final double pickupServiceTime;
	private final ShipmentPlan schedule;
	private final ShipmentPlan log;
	private final ArrayList<EventHandler> eventHandlers;
	private final ArrayList<Requirement> requirements;
	private final ArrayList<LSPInfo> infos;
	private Id<LogisticsSolution> solutionId;

	public static class LSPShipmentBuilder{
		private final Id<LSPShipment> id;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private TimeWindow startTimeWindow;
		private TimeWindow endTimeWindow;
		private int capacityDemand;
		private double deliveryServiceTime;
		private double pickupServiceTime;
		private final ArrayList<Requirement> requirements;
		private final ArrayList<LSPInfo> infos;

		public static LSPShipmentBuilder newInstance( Id<LSPShipment> id ){
			return new LSPShipmentBuilder(id);
		}

		private LSPShipmentBuilder( Id<LSPShipment> id ){
			this.requirements = new ArrayList<>();
			this.infos = new ArrayList<>();
			this.id = id;
		}

		public void setFromLinkId(Id<Link> fromLinkId ){
			this.fromLinkId = fromLinkId;
		}

		public void setToLinkId(Id<Link> toLinkId ){
			this.toLinkId = toLinkId;
		}

		public void setStartTimeWindow(TimeWindow startTimeWindow ){
			this.startTimeWindow = startTimeWindow;
		}

		public void setEndTimeWindow(TimeWindow endTimeWindow ){
			this.endTimeWindow = endTimeWindow;
		}

		public void setCapacityDemand(int capacityDemand ){
			this.capacityDemand = capacityDemand;
		}

		public void setDeliveryServiceTime(double serviceTime ){
			this.deliveryServiceTime = serviceTime;
		}
		public LSPShipmentBuilder setPickupServiceTime( double serviceTime ){
			this.pickupServiceTime = serviceTime;
			return this;
		}

		public void addRequirement(Requirement requirement ) {
			requirements.add(requirement);
		}

		public void addInfo(LSPInfo info ) {
			infos.add(info);
		}

		public LSPShipment build(){
			return new LSPShipmentImpl(this);
		}
	}

	private LSPShipmentImpl( LSPShipmentBuilder builder ){
		this.id = builder.id;
		this.fromLinkId = builder.fromLinkId;
		this.toLinkId = builder.toLinkId;
		this.startTimeWindow = builder.startTimeWindow;
		this.endTimeWindow = builder.endTimeWindow;
		this.capacityDemand = builder.capacityDemand;
		this.deliveryServiceTime = builder.deliveryServiceTime;
		this.pickupServiceTime = builder.pickupServiceTime;
		this.schedule = new ShipmentPlanImpl(this);
		this.log = new ShipmentPlanImpl(this);
		this.eventHandlers = new ArrayList<>();
		this.requirements = new ArrayList<>();
		this.requirements.addAll( builder.requirements );
		this.infos = new ArrayList<>();
		this.infos.addAll( builder.infos );
	}


	@Override
	public Id<LSPShipment> getId() {
		return id;
	}

	@Override
	public Id<Link> getFrom() {
		return fromLinkId;
	}

	@Override
	public Id<Link> getTo() {
		return toLinkId;
	}

	@Override
	public TimeWindow getPickupTimeWindow() {
		return startTimeWindow;
	}

	@Override
	public TimeWindow getDeliveryTimeWindow() {
		return endTimeWindow;
	}

	@Override
	public ShipmentPlan getShipmentPlan() {
		return schedule;
	}

	@Override
	public ShipmentPlan getLog() {
		return log;
	}

	@Override
	public int getSize() {
		return capacityDemand;
	}

	@Override
	public double getDeliveryServiceTime() {
		return deliveryServiceTime;
	}

	@Override
	public Collection<EventHandler> getEventHandlers() {
		return eventHandlers;
	}


	@Override
	public Collection<Requirement> getRequirements() {
		return requirements;
	}

	@Override
	public Collection<LSPInfo> getInfos() {
		return infos;
	}

	@Override public Id<LogisticsSolution> getSolutionId() {
		return solutionId;
	}

	@Override public double getPickupServiceTime(){
		return pickupServiceTime;
	}
	
}

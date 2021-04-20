package lsp.shipment;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.core.events.handler.EventHandler;

import demand.UtilityFunction;
import lsp.LogisticsSolution;
import lsp.functions.LSPInfo;

/* package-private */ class LSPShipmentImpl implements LSPShipment {

	private final Id<LSPShipment> id;
	private final Id<Link> fromLinkId;
	private final Id<Link> toLinkId;
	private final TimeWindow startTimeWindow;
	private final TimeWindow endTimeWindow;
	private final int capacityDemand;
	private final double serviceTime;
	private final ShipmentPlan schedule;
	private final ShipmentPlan log;
	private final ArrayList<EventHandler> eventHandlers;
	private final ArrayList<Requirement> requirements;
//	private final ArrayList<UtilityFunction> utilityFunctions;
	private final ArrayList<LSPInfo> infos;
	private Id<LogisticsSolution> solutionId;

	LSPShipmentImpl( ShipmentUtils.LSPShipmentBuilder builder ){
		this.id = builder.getId();
		this.fromLinkId = builder.getFromLinkId();
		this.toLinkId = builder.getToLinkId();
		this.startTimeWindow = builder.getStartTimeWindow();
		this.endTimeWindow = builder.getEndTimeWindow();
		this.capacityDemand = builder.getCapacityDemand();
		this.serviceTime = builder.getServiceTime();
		this.schedule = new Schedule(this);
		this.log = new Log(this);
		this.eventHandlers = new ArrayList<>();
		this.requirements = new ArrayList<>();
		this.requirements.addAll( builder.getRequirements() );
//		this.utilityFunctions = new ArrayList<>();
//		for(UtilityFunction utilityFunction : builder.getUtilityFunctions()) {
//			this.utilityFunctions.add(utilityFunction);
//		}
		this.infos = new ArrayList<>();
		this.infos.addAll( builder.getInfos() );
	}
	
	
	@Override
	public Id<LSPShipment> getId() {
		return id;
	}

	@Override
	public Id<Link> getFromLinkId() {
		return fromLinkId;
	}

	@Override
	public Id<Link> getToLinkId() {
		return toLinkId;
	}

	@Override
	public TimeWindow getStartTimeWindow() {
		return startTimeWindow;
	}

	@Override
	public TimeWindow getEndTimeWindow() {
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
	public int getCapacityDemand() {
		return capacityDemand;
	}

	@Override
	public double getServiceDuration() {
		return serviceTime;
	}

	@Override
	public Collection<EventHandler> getEventHandlers() {
		return eventHandlers;
	}


	@Override
	public Collection<Requirement> getRequirements() {
		return requirements;
	}


//	@Override
//	public Collection<UtilityFunction> getUtilityFunctions() {
//		return utilityFunctions;
//	}


	@Override
	public Collection<LSPInfo> getInfos() {
		return infos;
	}


	@Override public Id<LogisticsSolution> getSolutionId() {
		return solutionId;
	}


//	@Override public void setSolutionId( Id<LogisticsSolution> solutionId ) {
//		this.solutionId = solutionId;
//	}

	
	
}

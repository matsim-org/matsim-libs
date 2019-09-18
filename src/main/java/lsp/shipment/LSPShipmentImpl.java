package lsp.shipment;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.core.events.handler.EventHandler;

import demand.utilityFunctions.UtilityFunction;
import lsp.LogisticsSolution;
import lsp.functions.Info;

/* package-private */ class LSPShipmentImpl implements LSPShipment {

	private Id<LSPShipment> id;
	private Id<Link> fromLinkId;
	private Id<Link> toLinkId;
	private TimeWindow startTimeWindow;
	private TimeWindow endTimeWindow;
	private int capacityDemand;
	private double serviceTime;
	private AbstractShipmentPlan schedule;
	private AbstractShipmentPlan log;
	private ArrayList<EventHandler> eventHandlers;
	private ArrayList<Requirement> requirements;
	private ArrayList<UtilityFunction> utilityFunctions;
	private ArrayList<Info> infos;
	private Id<LogisticsSolution> solutionId;

	LSPShipmentImpl( ShipmentUtils.LSPShipmentBuilder builder ){
		this.id = builder.id;
		this.fromLinkId = builder.fromLinkId;
		this.toLinkId = builder.toLinkId;
		this.startTimeWindow = builder.startTimeWindow;
		this.endTimeWindow = builder.endTimeWindow;
		this.capacityDemand = builder.capacityDemand;
		this.serviceTime = builder.serviceTime;
		this.schedule = new Schedule(this);
		this.log = new Log(this);
		this.eventHandlers = new ArrayList<>();
		this.requirements = new ArrayList<>();
		for(Requirement requirement : builder.requirements) {
			this.requirements.add(requirement);
		}
		this.utilityFunctions = new ArrayList<>();
		for(UtilityFunction utilityFunction : builder.utilityFunctions) {
			this.utilityFunctions.add(utilityFunction);
		}
		this.infos = new ArrayList<>();
		for(Info info : builder.infos) {
			this.infos.add(info);
		}
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
	public AbstractShipmentPlan getSchedule() {
		return schedule;
	}

	@Override
	public AbstractShipmentPlan getLog() {
		return log;
	}

	@Override
	public int getCapacityDemand() {
		return capacityDemand;
	}

	@Override
	public double getServiceTime() {
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


	@Override
	public Collection<UtilityFunction> getUtilityFunctions() {
		return utilityFunctions;
	}


	@Override
	public Collection<Info> getInfos() {
		return infos;
	}


	public Id<LogisticsSolution> getSolutionId() {
		return solutionId;
	}


	public void setSolutionId(Id<LogisticsSolution> solutionId) {
		this.solutionId = solutionId;
	}

	
	
}

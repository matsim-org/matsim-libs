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

public class LSPShipmentImpl implements LSPShipment {

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
	
	public static class Builder {	

		private Id<LSPShipment> id;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private TimeWindow startTimeWindow;
		private TimeWindow endTimeWindow;
		private int capacityDemand;
		private double serviceTime;
		private ArrayList<Requirement> requirements;
		private ArrayList<UtilityFunction> utilityFunctions;
		private ArrayList<Info> infos;
	
		public static Builder newInstance(Id<LSPShipment> id){
			return new Builder(id);
		}
	
	private Builder(Id<LSPShipment> id){
		this.requirements = new ArrayList<Requirement>();
		this.utilityFunctions = new ArrayList<UtilityFunction>();
		this.infos = new ArrayList<Info>();
		this.id = id;
	}
	
	public Builder setFromLinkId(Id<Link> fromLinkId){
		this.fromLinkId = fromLinkId;
		return this;
	}
	
	public Builder setToLinkId(Id<Link> toLinkId){
		this.toLinkId = toLinkId;
		return this;
	}
	
	public Builder setStartTimeWindow(TimeWindow startTimeWindow){
		this.startTimeWindow = startTimeWindow;
		return this;
	}
	
	public Builder setEndTimeWindow(TimeWindow endTimeWindow){
		this.endTimeWindow = endTimeWindow;
		return this;
	}
	
	public Builder setCapacityDemand(int capacityDemand){
		this.capacityDemand = capacityDemand;
		return this;
	}
	
	public Builder setServiceTime(double serviceTime){
		this.serviceTime = serviceTime;
		return this;
	}
	
	public Builder addRequirement(Requirement requirement) {
		requirements.add(requirement);
		return this;
	}
	
	public Builder addUtilityFunction(UtilityFunction utilityFunction) {
		utilityFunctions.add(utilityFunction);
		return this;
	}
	
	public Builder addInfo(Info info) {
		infos.add(info);
		return this;
	}
	
	public LSPShipmentImpl build(){
		return new LSPShipmentImpl(this);
	}
	
	
	}	
	
	private LSPShipmentImpl(LSPShipmentImpl.Builder builder){
		this.id = builder.id;
		this.fromLinkId = builder.fromLinkId;
		this.toLinkId = builder.toLinkId;
		this.startTimeWindow = builder.startTimeWindow;
		this.endTimeWindow = builder.endTimeWindow;
		this.capacityDemand = builder.capacityDemand;
		this.serviceTime = builder.serviceTime;
		this.schedule = new Schedule(this);
		this.log = new Log(this);
		this.eventHandlers = new ArrayList<EventHandler>();
		this.requirements = new ArrayList<Requirement>();
		for(Requirement requirement : builder.requirements) {
			this.requirements.add(requirement);
		}
		this.utilityFunctions = new ArrayList<UtilityFunction>();
		for(UtilityFunction utilityFunction : builder.utilityFunctions) {
			this.utilityFunctions.add(utilityFunction);
		}
		this.infos = new ArrayList<Info>();
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

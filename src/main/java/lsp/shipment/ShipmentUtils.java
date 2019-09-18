package lsp.shipment;

import demand.utilityFunctions.UtilityFunction;
import lsp.functions.Info;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.TimeWindow;

import java.util.ArrayList;

public class ShipmentUtils{
	private ShipmentUtils(){} // do not instantiate
	public static class LSPShipmentBuilder{

		Id<LSPShipment> id;
		Id<Link> fromLinkId;
		Id<Link> toLinkId;
		TimeWindow startTimeWindow;
		TimeWindow endTimeWindow;
		int capacityDemand;
		double serviceTime;
		ArrayList<Requirement> requirements;
		ArrayList<UtilityFunction> utilityFunctions;
		ArrayList<Info> infos;

		public static LSPShipmentBuilder newInstance( Id<LSPShipment> id ){
			return new LSPShipmentBuilder(id);
		}

	private LSPShipmentBuilder( Id<LSPShipment> id ){
		this.requirements = new ArrayList<Requirement>();
		this.utilityFunctions = new ArrayList<UtilityFunction>();
		this.infos = new ArrayList<Info>();
		this.id = id;
	}

	public LSPShipmentBuilder setFromLinkId( Id<Link> fromLinkId ){
		this.fromLinkId = fromLinkId;
		return this;
	}

	public LSPShipmentBuilder setToLinkId( Id<Link> toLinkId ){
		this.toLinkId = toLinkId;
		return this;
	}

	public LSPShipmentBuilder setStartTimeWindow( TimeWindow startTimeWindow ){
		this.startTimeWindow = startTimeWindow;
		return this;
	}

	public LSPShipmentBuilder setEndTimeWindow( TimeWindow endTimeWindow ){
		this.endTimeWindow = endTimeWindow;
		return this;
	}

	public LSPShipmentBuilder setCapacityDemand( int capacityDemand ){
		this.capacityDemand = capacityDemand;
		return this;
	}

	public LSPShipmentBuilder setServiceTime( double serviceTime ){
		this.serviceTime = serviceTime;
		return this;
	}

	public LSPShipmentBuilder addRequirement( Requirement requirement ) {
		requirements.add(requirement);
		return this;
	}

	public LSPShipmentBuilder addUtilityFunction( UtilityFunction utilityFunction ) {
		utilityFunctions.add(utilityFunction);
		return this;
	}

	public LSPShipmentBuilder addInfo( Info info ) {
		infos.add(info);
		return this;
	}

	public LSPShipment build(){
		return new LSPShipmentImpl(this);
	}


	}
}

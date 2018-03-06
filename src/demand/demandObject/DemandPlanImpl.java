package demand.demandObject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.BasicPlan;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LSPWithOffers;
import lsp.LogisticsSolution;


public class DemandPlanImpl implements DemandPlan{

	private double score;
	private ShipperShipment shipment;
	private LSPDecorator lsp;
	private Id<LogisticsSolution> solutionId;
	private DemandObject demandObject;
	
	public static class Builder{
		private ShipperShipment shipment;
		private LSPDecorator lsp;
		private Id<LogisticsSolution> solutionId;
		private DemandObject demandObject;
		
		public static Builder newInstance(){
			return new Builder();
		}
	
		private Builder(){
		}
		
		public Builder setShipperShipment(ShipperShipment shipment){
			this.shipment = shipment;
			return this;
		}
		
		public Builder setLsp(LSPDecorator  lsp){
			this.lsp = lsp;
			return this;
		}
		
		public Builder setDemandObject(DemandObject  demandObject){
			this.demandObject = demandObject;
			return this;
		}
		
		public Builder setLogisticsSolutionId(Id<LogisticsSolution> solutionId){
			this.solutionId = solutionId;
			return this;
		}
		
		public DemandPlanImpl build() {
			return new DemandPlanImpl(this);
		}
		
	}
	
	private DemandPlanImpl(Builder builder) {
		this.shipment = builder.shipment;
		this.lsp = builder.lsp;
		this.solutionId = builder.solutionId;
		this.demandObject = builder.demandObject;
	}
	
	@Override
	public Double getScore() {
		return score;
	}

	@Override
	public void setScore(Double arg0) {
		this.score = arg0;		
	}

	public ShipperShipment getShipment() {
		return shipment;
	}

	public LSPDecorator getLsp() {
		return lsp;
	}

	public Id<LogisticsSolution> getSolutionId() {
		return solutionId;
	}

	public DemandObject getDemandObject() {
		return demandObject;
	}

	public void setDemandObject(DemandObject demandObject) {
		this.demandObject = demandObject;
	}

}

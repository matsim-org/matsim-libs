package demand.demandObject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.BasicPlan;

import demand.decoratedLSP.LSPWithOffers;
import lsp.LogisticsSolution;


public class DemandPlan implements BasicPlan{

	private double score;
	private ShipperShipment shipment;
	private LSPWithOffers lsp;
	private Id<LogisticsSolution> solutionId;
	
	public static class Builder{
		private ShipperShipment shipment;
		private LSPWithOffers lsp;
		private Id<LogisticsSolution> solutionId;
		
		public static Builder getInstance(){
			return new Builder();
		}
	
		private Builder(){
		}
		
		public Builder setShipperShipment(ShipperShipment shipment){
			this.shipment = shipment;
			return this;
		}
		
		public Builder setLsp(LSPWithOffers  lsp){
			this.lsp = lsp;
			return this;
		}
		
		public Builder setLogisticsSolutionId(Id<LogisticsSolution> solutionId){
			this.solutionId = solutionId;
			return this;
		}
		
		public DemandPlan build() {
			return new DemandPlan(this);
		}
		
	}
	
	private DemandPlan(Builder builder) {
		this.shipment = builder.shipment;
		this.lsp = builder.lsp;
		this.solutionId = builder.solutionId;
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

	public LSPWithOffers getLsp() {
		return lsp;
	}

	public Id<LogisticsSolution> getSolutionId() {
		return solutionId;
	}

}

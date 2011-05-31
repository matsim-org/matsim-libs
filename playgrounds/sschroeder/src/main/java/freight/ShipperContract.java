package freight;

public class ShipperContract {
	
	private CommodityFlow commodityFlow;

	public ShipperContract(CommodityFlow commodityFlow) {
		super();
		this.commodityFlow = commodityFlow;
	}

	public CommodityFlow getCommodityFlow() {
		return commodityFlow;
	}
	
	

}

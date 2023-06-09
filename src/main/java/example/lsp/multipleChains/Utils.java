package example.lsp.multipleChains;

public class Utils {
	public static RandomLogisticChainShipmentAssigner createRandomLogisticChainShipmentAssigner() {
		return new RandomLogisticChainShipmentAssigner();
	}

	public static RoundRobinLogisticChainShipmentAssigner createRoundRobinLogisticChainShipmentAssigner() {
		return new RoundRobinLogisticChainShipmentAssigner();
	}

	public static PrimaryLogisticChainShipmentAssigner createPrimaryLogisticChainShipmentAssigner() {
		return new PrimaryLogisticChainShipmentAssigner();
	}
}

package example.lsp.multipleChains;

public class Utils {
	public static RandomLogisticChainShipmentAssigner createRandomLogisticChainShipmentAssigner() {
		return new RandomLogisticChainShipmentAssigner();
	}

	public static ConsecutiveLogisticChainShipmentAssigner createConsecutiveLogisticChainShipmentAssigner() {
		return new ConsecutiveLogisticChainShipmentAssigner();
	}

	public static PrimaryLogisticChainShipmentAssigner createPrimaryLogisticChainShipmentAssigner() {
		return new PrimaryLogisticChainShipmentAssigner();
	}
}

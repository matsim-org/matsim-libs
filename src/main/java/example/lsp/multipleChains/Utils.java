package example.lsp.multipleChains;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

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

	public enum LspPlanTypes {
		SINGLE_ONE_ECHELON_CHAIN("singleOneEchelonChain"),
		SINGLE_TWO_ECHELON_CHAIN("singleTwoEchelonChain"),
		MULTIPLE_ONE_ECHELON_CHAINS("multipleOneEchelonChains"),
		MULTIPLE_TWO_ECHELON_CHAINS("multipleTwoEchelonChains"),
		MULTIPLE_MIXED_ECHELON_CHAINS("multipleMixedEchelonChains");

		private final String label;
		LspPlanTypes(String label) {this.label = label;}

		@Override
		public String toString() {
			return label;
		}

		private static final Map<String, LspPlanTypes > stringToEnum =
				Stream.of(values()).collect(toMap(Object::toString, e -> e));

		public static LspPlanTypes fromString(String label) {
			return stringToEnum.get(label);

		}
	}
}

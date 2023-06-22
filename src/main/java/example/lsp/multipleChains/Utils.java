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
		ONE_ECHELON_SINGLE_CHAIN("oneEchelonSingleChain"),
		TWO_ECHELON_SINGLE_CHAIN("twoEchelonSingleChain"),
		ONE_ECHELON_MULTIPLE_CHAINS("oneEchelonMultipleChains"),
		TWO_ECHELON_MULTIPLE_CHAINS("twoEchelonMultipleChains"),
		HYBRID_PLAN ("hybridPlan");

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

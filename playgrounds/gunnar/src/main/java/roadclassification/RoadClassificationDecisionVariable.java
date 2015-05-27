package roadclassification;

import optdyts.DecisionVariable;

import org.matsim.core.utils.io.OsmNetworkReader;

class RoadClassificationDecisionVariable implements DecisionVariable {

	private final OsmNetworkReader reader;

	public RoadClassificationDecisionVariable(final OsmNetworkReader reader) {
		this.reader = reader;
	}

	@Override
	public void implementInSimulation() {
		throw new UnsupportedOperationException();
	}

}

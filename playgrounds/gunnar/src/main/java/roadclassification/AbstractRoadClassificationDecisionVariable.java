package roadclassification;

import optdyts.DecisionVariable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

abstract class AbstractRoadClassificationDecisionVariable implements DecisionVariable {

	private Network network;

	public AbstractRoadClassificationDecisionVariable(Network network) {
		this.network = network;
	}

	/**
	 * This is where concrete implementations configure the osmNetworkReader.
	 *
	 */
	abstract void doSetHighwayDefaults(OsmNetworkReader osmNetworkReader);

	@Override
	public final void implementInSimulation() {
		for (Id<Link> linkId : new ArrayList<>(network.getLinks().keySet())) {
			network.removeLink(linkId);
		}
		for (Id<Node> nodeId : new ArrayList<>(network.getNodes().keySet())) {
			network.removeNode(nodeId);
		}
		OsmNetworkReader osmNetworkReader = new OsmNetworkReader(network, DownloadExampleData.COORDINATE_TRANSFORMATION);
		doSetHighwayDefaults(osmNetworkReader);
		try (InputStream is = new FileInputStream(DownloadExampleData.SIOUX_FALLS)) {
			osmNetworkReader.parse(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}

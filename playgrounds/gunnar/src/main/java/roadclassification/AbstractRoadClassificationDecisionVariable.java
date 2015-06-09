package roadclassification;

import com.google.inject.Inject;
import optdyts.DecisionVariable;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

abstract class AbstractRoadClassificationDecisionVariable implements DecisionVariable {

	@Inject
	Network network;

	abstract void doSetHighwayDefaults(OsmNetworkReader osmNetworkReader);

	@Override
	public final void implementInSimulation() {
		OsmNetworkReader osmNetworkReader = new OsmNetworkReader(network, TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:3359"));
		doSetHighwayDefaults(osmNetworkReader);
		try (InputStream is = new FileInputStream(DownloadExampleData.SIOUX_FALLS)) {
			osmNetworkReader.parse(is);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}

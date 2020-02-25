package org.matsim.contrib.osm.networkReader;

import org.matsim.api.core.v01.Coord;

import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class OsmSignalsReader extends SupersonicOsmNetworkReader {


	OsmSignalsReader(OsmNetworkParser parser, Predicate<Long> preserveNodeWithId, BiPredicate<Coord, Integer> includeLinkAtCoordWithHierarchy, AfterLinkCreated afterLinkCreated) {
		super(parser, preserveNodeWithId, includeLinkAtCoordWithHierarchy, afterLinkCreated);
	}

	public static class Builder extends AbstractBuilder<OsmSignalsReader> {

		@Override
		OsmSignalsReader createInstance() {

			OsmNetworkParser parser = new OsmNetworkParser(coordinateTransformation,
					linkProperties, includeLinkAtCoordWithHierarchy, Executors.newWorkStealingPool());

			return new OsmSignalsReader(parser, preserveNodeWithId, includeLinkAtCoordWithHierarchy, afterLinkCreated);
		}
	}
}

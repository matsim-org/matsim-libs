package org.matsim.osmNetworkReader;

import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
@Getter
class ProcessedOsmWay {

	private final long id;
	private final TLongArrayList nodeIds;
	private final Map<String, String> tags;
	private final LinkProperties linkProperties;

	static ProcessedOsmWay create(OsmWay osmWay, Map<String, String> tags, LinkProperties linkProperties) {

		return new ProcessedOsmWay(
				osmWay.getId(),
				copyNodeIds(osmWay),
				tags,
				linkProperties
		);
	}

	private static TLongArrayList copyNodeIds(OsmWay osmWay) {
		TLongArrayList result = new TLongArrayList();
		for (int i = 0; i < osmWay.getNumberOfNodes(); i++) {
			result.add(osmWay.getNodeId(i));
		}
		return result;
	}

	long getEndNodeId() {
		return nodeIds.get(nodeIds.size() - 1);
	}
}

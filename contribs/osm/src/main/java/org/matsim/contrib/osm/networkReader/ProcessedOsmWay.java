package org.matsim.contrib.osm.networkReader;

import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;
import de.topobyte.osm4j.core.model.iface.OsmWay;

import java.util.Map;

class ProcessedOsmWay {

	private final long id;
	private final TLongArrayList nodeIds;
	private final Map<String, String> tags;
	private final LinkProperties linkProperties;

	public ProcessedOsmWay(long id, TLongArrayList nodeIds, Map<String, String> tags, LinkProperties linkProperties) {
		this.id = id;
		this.nodeIds = nodeIds;
		this.tags = tags;
		this.linkProperties = linkProperties;
	}

	public long getId() {
		return id;
	}

	public TLongArrayList getNodeIds() {
		return nodeIds;
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public LinkProperties getLinkProperties() {
		return linkProperties;
	}

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

	long getStartNode() {
		return nodeIds.get(0);
	}
}

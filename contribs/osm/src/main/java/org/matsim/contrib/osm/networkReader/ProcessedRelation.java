package org.matsim.contrib.osm.networkReader;

public class ProcessedRelation {

	private final long nodeId;
	private final long fromWayId;
	private final long toWayId;
	private final Type type;

	public ProcessedRelation(long nodeId, long fromWayId, long toWayId, Type type) {
		this.nodeId = nodeId;
		this.fromWayId = fromWayId;
		this.toWayId = toWayId;
		this.type = type;
	}

	public long getNodeId() {
		return nodeId;
	}

	public long getFromWayId() {
		return fromWayId;
	}

	public long getToWayId() {
		return toWayId;
	}

	public Type getType() {
		return type;
	}

	enum Type {PROHIBITIVE, IMPERATIVE}
}

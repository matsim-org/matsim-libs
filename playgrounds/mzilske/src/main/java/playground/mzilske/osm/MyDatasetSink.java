package playground.mzilske.osm;



// public class MyDatasetSink implements DatasetSink {

//	@Override
//	public void process(Dataset dataset) {

//		this.network.setCapacityPeriod(3600);

//		DatasetContext datasetContext = dataset.createReader();
//		ReleasableIterator<Way> it =  datasetContext.getWayManager().iterate();
//		while (it.hasNext()) {
//			Way entry = it.next();
//			for (WayNode wayNode : entry.getWayNodes()) {
//				if (!datasetContext.getNodeManager().exists(wayNode.getNodeId())) {
//					throw new RuntimeException("Way with non-existing node.");
//				}
//			}
//		}
//		it.release();

		// check which nodes are used
//		for (OsmWay way : this.ways.values()) {
//			String highway = way.tags.get("highway");
//			if ((highway != null) && (this.highwayDefaults.containsKey(highway))) {
//				// check to which level a way belongs
//				way.hierarchy = this.highwayDefaults.get(highway).hierarchy;
//
//				// first and last are counted twice, so they are kept in all cases
//				this.nodes.get(way.nodes.get(0)).ways++;
//				this.nodes.get(way.nodes.get(way.nodes.size()-1)).ways++;
//
//				for (String nodeId : way.nodes) {
//					OsmNode node = this.nodes.get(nodeId);
//					if(this.hierarchyLayers.isEmpty()){
//						node.used = true;
//						node.ways++;
//					} else {
//						for (OsmFilter osmFilter : this.hierarchyLayers) {
//							if(osmFilter.coordInFilter(node.coord, way.hierarchy)){
//								node.used = true;
//								node.ways++;
//								break;
//							}
//						}
//					}
//				}
//			}
//		}
//
//		if (!this.keepPaths) {
//			// marked nodes as unused where only one way leads through
//			for (OsmNode node : this.nodes.values()) {
//				if ((node.ways == 1) && (!this.keepPaths)) {
//					node.used = false;
//				}
//			}
//			// verify we did not mark nodes as unused that build a loop
//			for (OsmWay way : this.ways.values()) {
//				String highway = way.tags.get("highway");
//				if ((highway != null) && (this.highwayDefaults.containsKey(highway))) {
//					int prevRealNodeIndex = 0;
//					OsmNode prevRealNode = this.nodes.get(way.nodes.get(prevRealNodeIndex));
//
//					for (int i = 1; i < way.nodes.size(); i++) {
//						OsmNode node = this.nodes.get(way.nodes.get(i));
//						if (node.used) {
//							if (prevRealNode == node) {
//								/* We detected a loop between to "real" nodes.
//								 * Set some nodes between the start/end-loop-node to "used" again.
//								 * But don't set all of them to "used", as we still want to do some network-thinning.
//								 * I decided to use sqrt(.)-many nodes in between...
//								 */
//								double increment = Math.sqrt(i - prevRealNodeIndex);
//								double nextNodeToKeep = prevRealNodeIndex + increment;
//								for (double j = nextNodeToKeep; j < i; j += increment) {
//									int index = (int) Math.floor(j);
//									OsmNode intermediaryNode = this.nodes.get(way.nodes.get(index));
//									intermediaryNode.used = true;
//								}
//							}
//							prevRealNodeIndex = i;
//							prevRealNode = node;
//						}
//					}
//				}
//			}
//
//		}
//
//		// create the required nodes
//		for (OsmNode node : this.nodes.values()) {
//			if (node.used) {
//				this.network.createAndAddNode(node.id, node.coord);
//			}
//		}
//
//		// create the links
//		this.id = 1;
//		for (OsmWay way : this.ways.values()) {
//			String highway = way.tags.get("highway");
//			if (highway != null) {
//				OsmNode fromNode = this.nodes.get(way.nodes.get(0));
//				double length = 0.0;
//				OsmNode lastToNode = fromNode;
//				if (fromNode.used) {
//					for (int i = 1, n = way.nodes.size(); i < n; i++) {
//						OsmNode toNode = this.nodes.get(way.nodes.get(i));
//						if (toNode != lastToNode) {
//							length += CoordUtils.calcDistance(lastToNode.coord, toNode.coord);
//							if (toNode.used) {
//
//								if(this.hierarchyLayers.isEmpty()){
//									createLink(this.network, way, fromNode, toNode, length);
//								} else {
//									for (OsmFilter osmFilter : this.hierarchyLayers) {
//										if(osmFilter.coordInFilter(fromNode.coord, way.hierarchy)){
//											createLink(this.network, way, fromNode, toNode, length);
//											break;
//										}
//										if(osmFilter.coordInFilter(toNode.coord, way.hierarchy)){
//											createLink(this.network, way, fromNode, toNode, length);
//											break;
//										}
//									}
//								}
//
//								fromNode = toNode;
//								length = 0.0;
//							}
//							lastToNode = toNode;
//						}
//					}
//				}
//			}
//		}
//	}
//
//
//	@Override
//	public void release() {
//		// TODO Auto-generated method stub
//
//	}
//
//
//
//}

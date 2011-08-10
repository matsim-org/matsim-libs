package playground.gregor.grips.helper;

import java.util.HashMap;
import java.util.Map;

public class ProtoNetwork {


	Map<String,ProtoLink> protoLinks = new HashMap<String, ProtoLink>();
	Map<String,ProtoNode> protoNodes = new HashMap<String, ProtoNode>();


	public Map<String, ProtoLink> getProtoLinks() {
		return this.protoLinks;
	}
	public Map<String, ProtoNode> getProtoNodes() {
		return this.protoNodes;
	}



}

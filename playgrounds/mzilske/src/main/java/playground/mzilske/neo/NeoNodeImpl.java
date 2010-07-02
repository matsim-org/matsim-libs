package playground.mzilske.neo;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class NeoNodeImpl implements Node {

	static final String KEY_COORD_X = "x";
	static final String KEY_COORD_Y = "y";
	static final String KEY_ID = "node_id";
	private org.neo4j.graphdb.Node underlyingNode;
	
	public NeoNodeImpl(org.neo4j.graphdb.Node nextNode) {
		this.underlyingNode = nextNode;
	}
	
	@Override
	public boolean addInLink(Link link) {
		throw new RuntimeException();
	}
	
	@Override
	public boolean addOutLink(Link link) {
		throw new RuntimeException();
	}
	
	@Override
	public Map<Id, ? extends Link> getInLinks() {
		throw new RuntimeException();
	}
	
	@Override
	public Map<Id, ? extends Link> getOutLinks() {
		throw new RuntimeException();
	}
	
	@Override
	public Id getId() {
		return new IdImpl((String) this.underlyingNode.getProperty(KEY_ID));
	}
	
	@Override
	public Coord getCoord() {
		return new CoordImpl((Double) underlyingNode.getProperty(KEY_COORD_X), (Double) underlyingNode.getProperty(KEY_COORD_Y));
	}

	@Override
	public boolean equals(Object obj) {
		return this.underlyingNode.equals( ((NeoNodeImpl) obj).underlyingNode);
	}
	
	

}

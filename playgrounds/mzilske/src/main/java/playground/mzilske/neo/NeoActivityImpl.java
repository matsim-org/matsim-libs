package playground.mzilske.neo;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class NeoActivityImpl implements Activity {

	static final String KEY_TYPE = "type";
	
	final private Node underlyingNode;

	public NeoActivityImpl(Node endNode) {
		this.underlyingNode = endNode;
	}

	@Override
	public Coord getCoord() {
		return null;
	}

	@Override
	public double getEndTime() {
		throw new RuntimeException();
	}

	@Override
	public Id getFacilityId() {
		throw new RuntimeException();
	}

	@Override
	public Id getLinkId() {
		Relationship r = this.underlyingNode.getSingleRelationship(RelationshipTypes.TAKES_PLACE_AT, Direction.OUTGOING);
		if (r != null) {
			Link link = new NeoLinkImpl(r.getEndNode());
			return link.getId();
		} else {
			return null;
		}
	}

	@Override
	public double getStartTime() {
		throw new RuntimeException();
	}

	@Override
	public String getType() {
		return (String) underlyingNode.getProperty(KEY_TYPE);
	}

	@Override
	public void setEndTime(double seconds) {
		throw new RuntimeException();
	}

	@Override
	public void setStartTime(double seconds) {
		throw new RuntimeException();
	}

	@Override
	public void setType(String type) {
		throw new RuntimeException();
	}

	public Node getUnderlyingNode() {
		return underlyingNode;
	}

	public void setCoord(Coord coord) {
		throw new RuntimeException();
	}

	@Override
	public double getMaximumDuration() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public void setMaximumDuration(double seconds) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

}

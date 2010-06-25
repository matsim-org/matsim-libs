package playground.mzilske.neo;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.neo4j.graphdb.Node;

public class NeoActivityImpl implements Activity {

	static final String KEY_COORD = "KEY_COORD";
	
	final private Node underlyingNode;

	public NeoActivityImpl(Node endNode) {
		this.underlyingNode = endNode;
	}

	@Override
	public Coord getCoord() {
		return (Coord) underlyingNode.getProperty(KEY_COORD);
	}

	@Override
	public double getEndTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Id getFacilityId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Id getLinkId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getStartTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEndTime(double seconds) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStartTime(double seconds) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setType(String type) {
		// TODO Auto-generated method stub

	}

	public Node getUnderlyingNode() {
		return underlyingNode;
	}

	public void setCoord(Coord coord) {
		underlyingNode.setProperty(KEY_COORD, coord);
	}

}

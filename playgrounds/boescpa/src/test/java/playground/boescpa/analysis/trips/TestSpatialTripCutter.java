package playground.boescpa.analysis.trips;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import playground.boescpa.analysis.spatialCutters.CirclePointCutter;
import playground.boescpa.analysis.spatialCutters.NoCutter;

import java.util.Map;
import java.util.Set;

/**
 * Tests the correct spatial classification of trips.
 *
 * @author boescpa
 */
public class TestSpatialTripCutter {

    private Network network;
    private SpatialTripCutter cutter;

    @Before
    public void prepareTests() {
        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        network = scenario.getNetwork();
        // Coordinates Bellevue: 683518.0,246836.0
        Node belNode = new DummyNode(new Coord(683518.0, 246836.0), Id.create("Bellevue", Node.class));
        Node nulNode = new DummyNode(new Coord((double) 0, (double) 0), Id.create("Null", Node.class));
        Link l1 = new DummyLink(Id.create(1, Link.class), nulNode, nulNode);
        Link l2 = new DummyLink(Id.create(2, Link.class), belNode, belNode);
        network.addNode(nulNode);
        network.addNode(belNode);
        network.addLink(l1);
        network.addLink(l2);
    }

    @Test
    public void testNoCutting() {
        cutter = new SpatialTripCutter(new NoCutter(), network);

        Trip trip = new Trip(null, 0, Id.create(1, Link.class), 0, 0, 0, Id.create(2, Link.class), 0, 0, "", "", 0, 0);

        Assert.assertTrue("No cutting strategy doesn't return TRUE.",
                cutter.spatiallyConsideringTrip(trip));
    }

    @Test
    public void testCirclePointCutting() {
        cutter = new SpatialTripCutter(new CirclePointCutter(10, 683518.0, 246836.0), network);

        Trip trip = new Trip(null, 0, Id.create(1, Link.class), 0, 0, 0, Id.create(1, Link.class), 0, 0, "", "", 0, 0);
        Assert.assertTrue("CircleBellevueCutting does not recognize link outside circle.",
                !cutter.spatiallyConsideringTrip(trip));
        trip = new Trip(null, 0, Id.create(1, Link.class), 0, 0, 0, Id.create(2, Link.class), 0, 0, "", "", 0, 0);
        Assert.assertTrue("CircleBellevueCutting does not recognize path going into the circle.",
                cutter.spatiallyConsideringTrip(trip));
        trip = new Trip(null, 0, Id.create(2, Link.class), 0, 0, 0, Id.create(1, Link.class), 0, 0, "", "", 0, 0);
        Assert.assertTrue("CircleBellevueCutting does not recognize path going out of circle.",
                cutter.spatiallyConsideringTrip(trip));
        trip = new Trip(null, 0, Id.create(2, Link.class), 0, 0, 0, Id.create(2, Link.class), 0, 0, "", "", 0, 0);
        Assert.assertTrue("CircleBellevueCutting does not recognize path within circle.",
                cutter.spatiallyConsideringTrip(trip));
    }


    private class DummyNode implements Node {

        private final Coord coord;
        private final Id<Node> id;

        public DummyNode(Coord coord, Id<Node> id) {
            this.coord = coord;
            this.id = id;
        }

        @Override
        public Coord getCoord() {
            return this.coord;
        }

        @Override
        public Id<Node> getId() {
            return this.id;
        }

        @Override
        public boolean addInLink(Link link) {
            return false;
        }

        @Override
        public boolean addOutLink(Link link) {
            return false;
        }

        @Override
        public Map<Id<Link>, ? extends Link> getInLinks() {
            return null;
        }

        @Override
        public Map<Id<Link>, ? extends Link> getOutLinks() {
            return null;
        }

	@Override
	public Link removeInLink(Id<Link> linkId) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Link removeOutLink(Id<Link> outLinkId) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setCoord(Coord coord) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

        @Override
        public Attributes getAttributes() {
        	throw new UnsupportedOperationException();
        }
    }
    private class DummyLink implements Link {
        private final Node to;
        private final Node from;
        private final Id<Link> id;

        public DummyLink(Id<Link> id, Node from, Node to) {
            this.to = to;
            this.from = from;
            this.id = id;
        }

        @Override
        public Coord getCoord() {
            // as in org.matsim.core.network.LinkImpl
            Coord from = getFromNode().getCoord();
            Coord to = getToNode().getCoord();
            return new Coord((from.getX() + to.getX()) / 2.0, (from.getY() + to.getY()) / 2.0);
        }

        @Override
        public Id<Link> getId() {
            return id;
        }

        @Override
        public boolean setFromNode(Node node) {
            return false;
        }

        @Override
        public boolean setToNode(Node node) {
            return false;
        }

        @Override
        public Node getToNode() {
            return this.to;
        }

        @Override
        public Node getFromNode() {
            return this.from;
        }

        @Override
        public double getLength() {
            return 0;
        }

        @Override
        public double getNumberOfLanes() {
            return 0;
        }

        @Override
        public double getNumberOfLanes(double time) {
            return 0;
        }

        @Override
        public double getFreespeed() {
            return 0;
        }

        @Override
        public double getFreespeed(double time) {
            return 0;
        }

        @Override
        public double getCapacity() {
            return 0;
        }

        @Override
        public double getCapacity(double time) {
            return 0;
        }

        @Override
        public void setFreespeed(double freespeed) {
        }

        @Override
        public void setLength(double length) {
        }

        @Override
        public void setNumberOfLanes(double lanes) {
        }

        @Override
        public void setCapacity(double capacity) {
        }

        @Override
        public void setAllowedModes(Set<String> modes) {
        }

        @Override
        public Set<String> getAllowedModes() {
            return null;
        }

	@Override
	public double getFlowCapacityPerSec() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public double getFlowCapacityPerSec(double time) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

        @Override
        public Attributes getAttributes() {
            throw new UnsupportedOperationException();
        }
    }

}

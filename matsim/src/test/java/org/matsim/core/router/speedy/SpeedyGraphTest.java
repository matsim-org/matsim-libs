package org.matsim.core.router.speedy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.speedy.SpeedyGraph.LinkIterator;

/**
 * @author mrieser
 */
public class SpeedyGraphTest {

	@Test
	void testConstruction() {
        Id.resetCaches();

        Fixture f = new Fixture();
        Network network = f.network;

        SpeedyGraph graph = new SpeedyGraph(network);

        // test out-links

        LinkIterator li = graph.getOutLinkIterator();

        // test out-links node 1

        li.reset(f.node1.getId().index());
        Assertions.assertTrue(li.next());
        assertLink(li, f.link12);
        Assertions.assertTrue(li.next());
        assertLink(li, f.link13);
        Assertions.assertTrue(li.next());
        assertLink(li, f.link14);
        Assertions.assertFalse(li.next());
        Assertions.assertFalse(li.next());

        // test out-links node 2

        li.reset(f.node2.getId().index());
        Assertions.assertTrue(li.next());
        assertLink(li, f.link21);
        Assertions.assertFalse(li.next());
        Assertions.assertFalse(li.next());

        // test out-links node 3

        li.reset(f.node3.getId().index());
        Assertions.assertTrue(li.next());
        assertLink(li, f.link34);
        Assertions.assertTrue(li.next());
        assertLink(li, f.link35);
        Assertions.assertFalse(li.next());
        Assertions.assertFalse(li.next());

        // test out-links node 4

        li.reset(f.node4.getId().index());
        Assertions.assertTrue(li.next());
        assertLink(li, f.link46);
        Assertions.assertFalse(li.next());
        Assertions.assertFalse(li.next());

        // test out-links node 5

        li.reset(f.node5.getId().index());
        Assertions.assertTrue(li.next());
        assertLink(li, f.link56);
        Assertions.assertFalse(li.next());
        Assertions.assertFalse(li.next());

        // test out-links node 6

        li.reset(f.node6.getId().index());
        Assertions.assertTrue(li.next());
        assertLink(li, f.link65);
        Assertions.assertTrue(li.next());
        assertLink(li, f.link62);
        Assertions.assertFalse(li.next());
        Assertions.assertFalse(li.next());

        // test in-links

        li = graph.getInLinkIterator();

        // test in-links node 1

        li.reset(f.node1.getId().index());
        Assertions.assertTrue(li.next());
        assertLink(li, f.link21);
        Assertions.assertFalse(li.next());
        Assertions.assertFalse(li.next());

        // test in-links node 2

        li.reset(f.node2.getId().index());
        Assertions.assertTrue(li.next());
        assertLink(li, f.link12);
        Assertions.assertTrue(li.next());
        assertLink(li, f.link62);
        Assertions.assertFalse(li.next());
        Assertions.assertFalse(li.next());

        // test in-links node 3

        li.reset(f.node3.getId().index());
        Assertions.assertTrue(li.next());
        assertLink(li, f.link13);
        Assertions.assertFalse(li.next());
        Assertions.assertFalse(li.next());

        // test in-links node 4

        li.reset(f.node4.getId().index());
        Assertions.assertTrue(li.next());
        assertLink(li, f.link14);
        Assertions.assertTrue(li.next());
        assertLink(li, f.link34);
        Assertions.assertFalse(li.next());
        Assertions.assertFalse(li.next());

        // test in-links node 5

        li.reset(f.node5.getId().index());
        Assertions.assertTrue(li.next());
        assertLink(li, f.link35);
        Assertions.assertTrue(li.next());
        assertLink(li, f.link65);
        Assertions.assertFalse(li.next());
        Assertions.assertFalse(li.next());

        // test in-links node 6

        li.reset(f.node6.getId().index());
        Assertions.assertTrue(li.next());
        assertLink(li, f.link46);
        Assertions.assertTrue(li.next());
        assertLink(li, f.link56);
        Assertions.assertFalse(li.next());
        Assertions.assertFalse(li.next());
    }

    private void assertLink(LinkIterator li, Link link) {
        Assertions.assertEquals(link.getId().index(), li.getLinkIndex());
        Assertions.assertEquals(link.getFromNode().getId().index(), li.getFromNodeIndex());
        Assertions.assertEquals(link.getToNode().getId().index(), li.getToNodeIndex());
        Assertions.assertEquals(link.getLength(), li.getLength(), 1e-2);
        Assertions.assertEquals(link.getLength() / link.getFreespeed(), li.getFreespeedTravelTime(), 1e-2);
    }

    private static class Fixture {

        Node node1;
        Node node2;
        Node node3;
        Node node4;
        Node node5;
        Node node6;

        Network network;

        Link link12;
        Link link21;
        Link link13;
        Link link14;
        Link link34;
        Link link35;
        Link link46;
        Link link56;
        Link link65;
        Link link62;

        public Fixture() {

            /*
             *   (1)==================(2)
             *    | \_____             ^
             *    v       \            |
             *   (3)-------(4)________ |
             *    v                   \^
             *   (5)==================(6)
             */

            this.network = NetworkUtils.createNetwork();
            NetworkFactory nf = this.network.getFactory();

            this.node1 = nf.createNode(Id.create("1", Node.class), new Coord(0, 1000));
            this.node2 = nf.createNode(Id.create("2", Node.class), new Coord(5000, 1000));
            this.node3 = nf.createNode(Id.create("3", Node.class), new Coord(0, 300));
            this.node4 = nf.createNode(Id.create("4", Node.class), new Coord(2000, 300));
            this.node5 = nf.createNode(Id.create("5", Node.class), new Coord(0, 0));
            this.node6 = nf.createNode(Id.create("6", Node.class), new Coord(5000, 0));

            this.network.addNode(this.node1);
            this.network.addNode(this.node2);
            this.network.addNode(this.node3);
            this.network.addNode(this.node4);
            this.network.addNode(this.node5);
            this.network.addNode(this.node6);

            this.link12 = createLink(nf, "12", this.node1, this.node2, 5000, 80 / 3.6);
            this.link21 = createLink(nf, "21", this.node2, this.node1, 5000, 80 / 3.6);
            this.link13 = createLink(nf, "13", this.node1, this.node3, 900, 60 / 3.6);
            this.link14 = createLink(nf, "14", this.node1, this.node4, 3500, 50 / 3.6);
            this.link34 = createLink(nf, "34", this.node3, this.node4, 2500, 50 / 3.6);
            this.link35 = createLink(nf, "35", this.node3, this.node5, 300, 60 / 3.6);
            this.link46 = createLink(nf, "46", this.node4, this.node6, 3000, 40 / 3.6);
            this.link56 = createLink(nf, "56", this.node5, this.node6, 5200, 80 / 3.6);
            this.link65 = createLink(nf, "65", this.node6, this.node5, 5200, 80 / 3.6);
            this.link62 = createLink(nf, "62", this.node6, this.node2, 1200, 60 / 3.6);

            this.network.addLink(this.link12);
            this.network.addLink(this.link21);
            this.network.addLink(this.link13);
            this.network.addLink(this.link14);
            this.network.addLink(this.link34);
            this.network.addLink(this.link35);
            this.network.addLink(this.link46);
            this.network.addLink(this.link56);
            this.network.addLink(this.link65);
            this.network.addLink(this.link62);
        }

        private Link createLink(NetworkFactory nf, String id, Node fromNode, Node toNode, double length, double freespeed) {
            Link link = nf.createLink(Id.create(id, Link.class), fromNode, toNode);
            link.setLength(length);
            link.setFreespeed(freespeed);
            link.setCapacity(2000);
            return link;
        }
    }

}
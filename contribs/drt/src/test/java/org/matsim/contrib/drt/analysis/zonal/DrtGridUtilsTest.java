package org.matsim.contrib.drt.analysis.zonal;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public class DrtGridUtilsTest {

	@Test
	public void test() {
		Network network = createNetwork();
		Map<String,Geometry> grid = DrtGridUtils.createGridFromNetwork(network, 100);
		Assert.assertEquals(100,grid.size());
		Geometry cell100 = grid.get("100");
		Point p = cell100.getCentroid();
		Assert.assertEquals(950, p.getX(),0.00001);
		Assert.assertEquals(950, p.getY(),0.00001);
	}
	
	private Network createNetwork(){
		Network network = NetworkUtils.createNetwork();
		Node a = network.getFactory().createNode(Id.createNodeId("a"), new Coord(0,0));
		Node b = network.getFactory().createNode(Id.createNodeId("b"), new Coord(0,1000));
		Node c = network.getFactory().createNode(Id.createNodeId("c"), new Coord(1000,1000));
		Node d = network.getFactory().createNode(Id.createNodeId("d"), new Coord(1000,0));
		network.addNode(a);
		network.addNode(b);
		network.addNode(c);
		network.addNode(d);
		
		Link ab = network.getFactory().createLink(Id.createLinkId("ab"), a, b);
		Link bc = network.getFactory().createLink(Id.createLinkId("bc"), b, c);
		Link cd = network.getFactory().createLink(Id.createLinkId("cd"), c, d);
		Link da = network.getFactory().createLink(Id.createLinkId("da"), d, a);
		network.addLink(ab);
		network.addLink(bc);
		network.addLink(cd);
		network.addLink(da);
		return network;
	}

}

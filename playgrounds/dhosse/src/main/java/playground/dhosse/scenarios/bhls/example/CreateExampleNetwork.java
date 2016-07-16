package playground.dhosse.scenarios.bhls.example;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

public class CreateExampleNetwork {
	
	public static void createYShapedNetwork(Scenario scenario){
		
		Set<String> modes = new HashSet<>();
		modes.add(TransportMode.car);
		modes.add("bus");
		
		Network network = scenario.getNetwork();
		
		Node n0 = network.getFactory().createNode(Id.createNodeId("0"), new Coord(0, 20000 + 10000 * Math.sqrt(2) / 2));
		network.addNode(n0);
		Node n1 = network.getFactory().createNode(Id.createNodeId("1"), new Coord(10000 * Math.sqrt(2) / 2, 20000));
		network.addNode(n1);
		Node n2 = network.getFactory().createNode(Id.createNodeId("2"), new Coord(10000 * Math.sqrt(2), 20000 + 10000 * Math.sqrt(2) / 2));
		network.addNode(n2);
		Node n3 = network.getFactory().createNode(Id.createNodeId("3"), new Coord(10000 * Math.sqrt(2) / 2, 10000));
		network.addNode(n3);
		Node n4 = network.getFactory().createNode(Id.createNodeId("4"), new Coord(10000 * Math.sqrt(2) / 2, 0));
		network.addNode(n4);
		
		Node a = network.getFactory().createNode(Id.createNodeId("a"), new Coord(0, 20000 + 10000 * Math.sqrt(2) / 2));
		network.addNode(a);
		Link a0 = network.getFactory().createLink(Id.createLinkId("a0"), a, n0);
		a0.setAllowedModes(modes);
		a0.setFreespeed(30/3.6);
		a0.setCapacity(2000);
		a0.setLength(0);
		network.addLink(a0);
		Link a01 = network.getFactory().createLink(Id.createLinkId("0a"), n0, a);
		a01.setAllowedModes(modes);
		a01.setFreespeed(30/3.6);
		a01.setCapacity(2000);
		a01.setLength(0);
		network.addLink(a01);
		
		Node b = network.getFactory().createNode(Id.createNodeId("b"), new Coord(10000 * Math.sqrt(2), 20000 + 10000 * Math.sqrt(2) / 2));
		network.addNode(b);
		Link b0 = network.getFactory().createLink(Id.createLinkId("b2"), b, n2);
		b0.setAllowedModes(modes);
		b0.setFreespeed(30/3.6);
		b0.setCapacity(2000);
		b0.setLength(0);
		network.addLink(b0);
		Link b01 = network.getFactory().createLink(Id.createLinkId("2b"), n2, b);
		b01.setAllowedModes(modes);
		b01.setFreespeed(30/3.6);
		b01.setCapacity(2000);
		b01.setLength(0);
		network.addLink(b01);
		
		Node z = network.getFactory().createNode(Id.createNodeId("z"), new Coord(10000 * Math.sqrt(2) / 2, 0));
		network.addNode(z);
		Link z0 = network.getFactory().createLink(Id.createLinkId("z4"), z, n4);
		z0.setAllowedModes(modes);
		z0.setFreespeed(30/3.6);
		z0.setCapacity(2000);
		z0.setLength(0);
		network.addLink(z0);
		Link z01 = network.getFactory().createLink(Id.createLinkId("4z"), n4, z);
		z01.setAllowedModes(modes);
		z01.setFreespeed(30/3.6);
		z01.setCapacity(2000);
		z01.setLength(0);
		network.addLink(z01);
		
		Link l01 = network.getFactory().createLink(Id.createLinkId("01"), n0, n1);
		setLinkProperties(l01, modes);
		network.addLink(l01);
		Link l10 = network.getFactory().createLink(Id.createLinkId("10"), n1, n0);
		setLinkProperties(l10, modes);
		network.addLink(l10);
		Link l21 = network.getFactory().createLink(Id.createLinkId("21"), n2, n1);
		setLinkProperties(l21, modes);
		network.addLink(l21);
		Link l12 = network.getFactory().createLink(Id.createLinkId("12"), n1, n2);
		setLinkProperties(l12, modes);
		network.addLink(l12);
		Link l13 = network.getFactory().createLink(Id.createLinkId("13"), n1, n3);
		setLinkProperties(l13, modes);
		network.addLink(l13);
		Link l31 = network.getFactory().createLink(Id.createLinkId("31"), n3, n1);
		setLinkProperties(l31, modes);
		network.addLink(l31);
		Link l34 = network.getFactory().createLink(Id.createLinkId("34"), n3, n4);
		setLinkProperties(l34, modes);
		network.addLink(l34);
		Link l43 = network.getFactory().createLink(Id.createLinkId("43"), n4, n3);
		setLinkProperties(l43, modes);
		network.addLink(l43);
		
		
	}
	
	private static void setLinkProperties(Link link, Set<String> modes){
		
		link.setAllowedModes(modes);
		link.setCapacity(1200);
		link.setFreespeed(50/3.6);
		link.setLength(20000);
		link.setNumberOfLanes(2);
		
	}
	
}

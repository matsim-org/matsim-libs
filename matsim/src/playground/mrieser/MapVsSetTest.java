/* *********************************************************************** *
 * project: org.matsim.*
 * MapVsSetTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.mrieser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapVsSetTest {

	public static final int NOF_LOOPS = 3600;

	static private class Link {
		int id;
		public Link(final int id) { this.id = id; }
	}

	static private class Node_map {
		int id;
		Map<Integer, Link> links = new LinkedHashMap<Integer, Link>();
		Link[] linksArray = null;
		public Node_map(final int id) { this.id = id; }
		public void addLink(final Link link) { this.links.put(link.id, link); }
	}

	static public class Node_list {
		int id;
		List<Link> links = new ArrayList<Link>();
		Link[] linksArray = null;
		public Node_list(final int id) { this.id = id; }
		public void addLink(final Link link) { this.links.add(link); }
	}

	public void run() {
		ArrayList<Node_map> nodes_map = new ArrayList<Node_map>();
		for (int i = 0; i < 10000; i++) {
			Node_map n = new Node_map(i);
			n.addLink(new Link(i));
			n.addLink(new Link(i + 10000));
			n.addLink(new Link(i + 20000));
			n.addLink(new Link(i + 30000));
			nodes_map.add(n);
//			if (i == 0) {
//				IObjectProfileNode profile = ObjectProfiler.profile(n.links);
//				System.out.println("Node_map " + profile.dump());
//			}
		}

		ArrayList<Node_list> nodes_list = new ArrayList<Node_list>();
		for (int i = 0; i < 10000; i++) {
			Node_list n = new Node_list(i);
			n.addLink(new Link(i));
			n.addLink(new Link(i + 10000));
			n.addLink(new Link(i + 20000));
			n.addLink(new Link(i + 30000));
			nodes_list.add(n);
//			if (i == 0) {
//				IObjectProfileNode profile = ObjectProfiler.profile(n.links);
//				System.out.println("Node_list " + profile.dump());
//			}
		}

		long startTime = System.currentTimeMillis();
		long sum = 0;
		for (int a = 0; a < NOF_LOOPS; a++) {
			for (Node_map node : nodes_map) {
				for (Link link : node.links.values()) {
					sum += link.id;
				}
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println(sum);
		System.out.println("Node_map iterator " + (endTime - startTime));

		startTime = System.currentTimeMillis();
		sum = 0;
		for (int a = 0; a < NOF_LOOPS; a++) {
			for (Node_map node : nodes_map) {
				Object[] links = node.links.values().toArray();
				for (int i = 0, max = links.length; i < max; i++) {
					sum += ((Link) links[i]).id;
				}
			}
		}
		endTime = System.currentTimeMillis();
		System.out.println(sum);
		System.out.println("Node_map as array " + (endTime - startTime));

		startTime = System.currentTimeMillis();
		for (Node_map node : nodes_map) {
			Link[] links = new Link[node.links.size()];
			node.linksArray = node.links.values().toArray(links);
		}
		endTime = System.currentTimeMillis();
		System.out.println("prepare Node_map linksArray " + (endTime - startTime));
//		IObjectProfileNode profile = ObjectProfiler.profile(nodes_map.get(0).linksArray);
//		System.out.println("Node_map.linksArray " + profile.dump());


		startTime = System.currentTimeMillis();
		sum = 0;
		for (int a = 0; a < NOF_LOOPS; a++) {
			for (Node_map node : nodes_map) {
				for (int i = 0, max = node.linksArray.length; i < max; i++) {
					sum += (node.linksArray[i]).id;
				}
			}
		}
		endTime = System.currentTimeMillis();
		System.out.println(sum);
		System.out.println("Node_map with linksArray " + (endTime - startTime));

		sum = 0;
		startTime = System.currentTimeMillis();
		for (int a = 0; a < NOF_LOOPS; a++) {
			for (Node_list node : nodes_list) {
				for (Link link : node.links) {
					sum += link.id;
				}
			}
		}
		endTime = System.currentTimeMillis();
		System.out.println(sum);
		System.out.println("Node_list iterator " + (endTime - startTime));

		sum = 0;
		startTime = System.currentTimeMillis();
		for (int a = 0; a < NOF_LOOPS; a++) {
			for (Node_list node : nodes_list) {
				Link[] links = new Link[node.links.size()];
				links = node.links.toArray(links);
				for (Link link : links) {
					sum += link.id;
				}
			}
		}
		endTime = System.currentTimeMillis();
		System.out.println(sum);
		System.out.println("Node_list Link[] " + (endTime - startTime));

		sum = 0;
		startTime = System.currentTimeMillis();
		for (int a = 0; a < NOF_LOOPS; a++) {
			for (Node_list node : nodes_list) {
				Object[] links = node.links.toArray();
				for (Object link : links) {
					sum += ((Link) link).id;
				}
			}
		}
		endTime = System.currentTimeMillis();
		System.out.println(sum);
		System.out.println("Node_list Object[] " + (endTime - startTime));

		sum = 0;
		startTime = System.currentTimeMillis();
		for (int a = 0; a < NOF_LOOPS; a++) {
			for (Node_list node : nodes_list) {
				for (int i = 0, max = node.links.size(); i < max; i++) {
					sum += node.links.get(i).id;
				}
			}
		}
		endTime = System.currentTimeMillis();
		System.out.println(sum);
		System.out.println("Node_list get(i) " + (endTime - startTime));

		startTime = System.currentTimeMillis();
		for (Node_list node : nodes_list) {
			Link[] links = new Link[node.links.size()];
			node.linksArray = node.links.toArray(links);
		}
		endTime = System.currentTimeMillis();
		System.out.println("prepare Node_list linksArray " + (endTime - startTime));

		sum = 0;
		startTime = System.currentTimeMillis();
		for (int a = 0; a < NOF_LOOPS; a++) {
			for (Node_list node : nodes_list) {
				for (Link link : node.linksArray) {
					sum += link.id;
				}
			}
		}
		endTime = System.currentTimeMillis();
		System.out.println(sum);
		System.out.println("Node_list with linksArray for() " + (endTime - startTime));

		sum = 0;
		startTime = System.currentTimeMillis();
		for (int a = 0; a < NOF_LOOPS; a++) {
			for (Node_list node : nodes_list) {
				for (int i = 0, max = node.linksArray.length; i < max; i++) {
					sum += node.linksArray[i].id;
				}
			}
		}
		endTime = System.currentTimeMillis();
		System.out.println(sum);
		System.out.println("Node_list with linksArray " + (endTime - startTime));

	}

	public static void main(final String[] args) {
		MapVsSetTest app = new MapVsSetTest();
		app.run();
	}
}

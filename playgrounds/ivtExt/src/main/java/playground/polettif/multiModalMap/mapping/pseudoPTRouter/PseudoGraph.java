/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.multiModalMap.mapping.pseudoPTRouter;


import java.util.ArrayList;
import java.util.List;

public class PseudoGraph {

	private final List<LinkCandidate> nodes;
	private final List<LinkCandidatePath> edges;

	private final LinkCandidate source;
	private final LinkCandidate destination;

	public PseudoGraph() {
		this.nodes = new ArrayList<>();
		this.edges = new ArrayList<>();

		this.source = new LinkCandidate("source");
		this.destination = new LinkCandidate("destination");

	}

	public List<LinkCandidate> getNodes() {
		return nodes;
	}

	public List<LinkCandidatePath> getEdges() {
		return edges;
	}

	public void addPath(LinkCandidatePath linkCandidatePath) {
		edges.add(linkCandidatePath);
	}

	public void addDummyBefore(List<LinkCandidate> linkCandidates) {
		for(LinkCandidate linkCandidate : linkCandidates) {
			edges.add(new LinkCandidatePath(source, linkCandidate, 1.0));
		}
	}

	public void addDummyAfter(List<LinkCandidate> linkCandidates) {
		for(LinkCandidate linkCandidate : linkCandidates) {
			edges.add(new LinkCandidatePath(linkCandidate, destination, 1.0));
		}
	}

	public LinkCandidate getSource() {
		return source;
	}

	public LinkCandidate getDestination() {
		return destination;
	}
}



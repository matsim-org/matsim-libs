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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PseudoGraph {

	private final List<PseudoRouteStop> nodes;
	private final Set<PseudoRoutePath> edges;

	private final PseudoRouteStop source;
	private final PseudoRouteStop destination;

	public PseudoGraph() {
		this.nodes = new ArrayList<>();
		this.edges = new HashSet<>();

		this.source = new PseudoRouteStop("source");
		this.destination = new PseudoRouteStop("destination");
	}

	public Set<PseudoRoutePath> getEdges() {
		return edges;
	}

	public void addPath(PseudoRoutePath pseudoRoutePath, boolean firstStop, boolean lastStop) {
		edges.add(pseudoRoutePath);

		// add dummy paths before and after route
		if(firstStop) {
//			PseudoRoutePath dummyPath = new PseudoRoutePath(source, pseudoRoutePath.getFromPseudoStop(), 1.0, true);
//			if(!edges.contains(dummyPath)) { edges.add(dummyPath); }
			edges.add(new PseudoRoutePath(source, pseudoRoutePath.getFromPseudoStop(), 1.0, true));
		}
		if(lastStop) {
//			PseudoRoutePath dummyPath = new PseudoRoutePath(pseudoRoutePath.getToPseudoStop(), destination, 1.0, true);
//			if(!edges.contains(dummyPath)) { edges.add(dummyPath); }
			edges.add(new PseudoRoutePath(pseudoRoutePath.getToPseudoStop(), destination, 1.0, true));
		}
	}

	public PseudoRouteStop getSource() {
		return source;
	}

	public PseudoRouteStop getDestination() {
		return destination;
	}
}



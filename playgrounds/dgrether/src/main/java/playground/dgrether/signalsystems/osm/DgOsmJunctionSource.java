/* *********************************************************************** *
 * project: org.matsim.*
 * DgOsmJunctionSource
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.osm;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.network.Node;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;


/**
 * @author dgrether
 *
 */
public class DgOsmJunctionSource implements RunnableSource {

	private Sink sink;
	private List<Set<Node>> junctionNodes;

	public DgOsmJunctionSource(List<Set<Node>> junctionNodes) {
		this.junctionNodes = junctionNodes;
	}

	@Override
	public void setSink(Sink s) {
		this.sink = s;
	}

	@Override
	public void run() {
		OsmUser user = new OsmUser(1, "dgrether");
		Date date = new Date();
		long id = 987654321;
		long changesetId = 324651;
		int vspId = 0;
		for (Set<Node> junction : this.junctionNodes){
			id++;
			vspId++;
			Relation relation = new Relation(id, 1, date, user, changesetId);
			relation.getTags().add(new Tag("type", "junction"));
			relation.getTags().add(new Tag("vsp_id", Integer.toString(vspId)));
			for (Node node : junction){
				RelationMember member = new RelationMember(Long.parseLong(node.getId().toString()), EntityType.Relation, "");
				relation.getMembers().add(member);
			}
			RelationContainer container = new RelationContainer(relation);
			this.sink.process(container);
		}
	}

}

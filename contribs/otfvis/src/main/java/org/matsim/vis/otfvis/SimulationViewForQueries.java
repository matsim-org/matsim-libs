package org.matsim.vis.otfvis;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.snapshotwriters.VisData;
import org.matsim.vis.snapshotwriters.VisNetwork;

/**
 * This shall evolve into a query interface for a running simulation, live or not.
 * It's not good yet.
 *  
 * @author michaz
 *
 */
public interface SimulationViewForQueries {
	
	Collection<Id<Person>> getAllAgentIds();
	
	Plan getPlan(Id<Person> agentId);

	Activity getCurrentActivity(Id<Person> agentId);
	
	Network getNetwork();

	EventsManager getEvents();

	VisNetwork getVisNetwork();

	OTFServerQuadTree getNetworkQuadTree();
	
	VisData getNonNetwokAgentSnapshots();

	double getTime();

}

package org.matsim.vis.otfvis2;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.interfaces.OTFQueryRemote;
import org.matsim.vis.otfvis.opengl.queries.AbstractQuery;

public final class QueryServer {

	private Collection<AbstractQuery> activeQueries = new ArrayList<AbstractQuery>();
	
	private EventsManager events;

	private SimulationViewForQueries queueModel;

	private Scenario scenario;

	public void removeQueries() {
		activeQueries.clear();
	}

	public QueryServer(Scenario scenario, EventsManager events, SimulationViewForQueries queueModel) {
		super();
		this.scenario = scenario;
		this.events = events;
		this.queueModel = queueModel;
	}

	public OTFQueryRemote answerQuery(AbstractQuery query) {
		query.installQuery(queueModel);
		activeQueries.add(query);
		return query;
	}

}

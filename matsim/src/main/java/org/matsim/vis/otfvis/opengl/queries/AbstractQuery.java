package org.matsim.vis.otfvis.opengl.queries;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryRemote;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.snapshots.writers.VisMobsimFeature;

public abstract class AbstractQuery implements OTFQuery, OTFQueryRemote {

	private static final long serialVersionUID = 1L;

	@Override
	public abstract Type getType();

	@Override
	public abstract void installQuery(VisMobsimFeature otfVisQueueSimFeature, EventsManager events, OTFServerQuad2 quad);

	@Override
	public abstract void setId(String id);

	@Override
	public abstract OTFQueryResult query();

	@Override
	public void uninstall() {
		// Default implementation doesn't do anything.
	}

	public void installQuery(SimulationViewForQueries queueModel) {
		
	}

}

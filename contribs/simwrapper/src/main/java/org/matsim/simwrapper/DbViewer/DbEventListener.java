package org.matsim.simwrapper.DbViewer;

import com.google.inject.Inject;
import org.mapdb.*;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

public class DbEventListener implements IterationEndsListener, IterationStartsListener {


	@Inject
	private EventsManager eventsManager;
	private final DB db;
	private final AgentState agentState;
	private DbEventHandler DbEventHandler;


	public DbEventListener(AgentState agentState) {

		this.agentState = agentState;
		this.db = DBMaker.memoryDB().make();

		HTreeMap<String, Long> linkMap = this.db.hashMap("links", Serializer.STRING, Serializer.LONG)
			.createOrOpen();

		HTreeMap<String, Long> agentMap = this.db.hashMap("Agents", Serializer.STRING, Serializer.LONG)
			.createOrOpen();

	}


	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.isLastIteration()) {
			//  PersonMoneyEventsCollector might consume many resources, run only at last iteration
			eventsManager.addHandler(DbEventHandler);
		}
	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		if (event.isLastIteration()) {
			DbWriter dbWriter = new DbWriter(DbEventHandler, db, agentState);
		}
	}

}

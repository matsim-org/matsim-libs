package org.matsim.simwrapper.DbViewer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.mapdb.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

public class DbEventListener implements IterationEndsListener, IterationStartsListener {

	private final EventsManager eventsManager;
	private final DbEventHandler dbEventHandler;
	private final DB db;

	@Inject
	public DbEventListener(EventsManager eventsManager, DbEventHandler dbEventHandler,
						   @DbOutputPath String outputDirectory) {
		this.eventsManager = eventsManager;
		this.dbEventHandler = dbEventHandler;
		this.db = DBMaker.fileDB(outputDirectory + "/agents.db")
			.fileMmapEnable()
			.checksumHeaderBypass()
			.make();
	}


	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.isLastIteration()) {
			eventsManager.addHandler(dbEventHandler);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.isLastIteration()) {
			DbWriter dbWriter = new DbWriter(dbEventHandler, db);
			dbWriter.write();
			db.close();
		}
	}
}

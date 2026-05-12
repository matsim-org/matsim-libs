package org.matsim.simwrapper.DbViewer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.mapdb.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.sql.SQLException;

public class DbEventListener implements IterationEndsListener, IterationStartsListener {

	private final EventsManager eventsManager;
	private final DbEventHandler dbEventHandler;
	private final DB db;
	private final Scenario scenario;
	private final String outputDirectory;

	@Inject
	public DbEventListener(EventsManager eventsManager, DbEventHandler dbEventHandler,
						   @DbOutputPath String outputDirectory, Scenario scenario) {
		this.eventsManager = eventsManager;
		this.dbEventHandler = dbEventHandler;
		this.db = DBMaker.fileDB(outputDirectory + "/agents.db")
			.fileMmapEnable()
			.checksumHeaderBypass()
			.make();
		this.scenario = scenario;
		this.outputDirectory = outputDirectory;
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
			DbWriter dbWriter = new DbWriter(dbEventHandler, db, scenario, outputDirectory);
//			dbWriter.write();
			try {
				dbWriter.writeAgentTable();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
			db.close();
		}
	}
}

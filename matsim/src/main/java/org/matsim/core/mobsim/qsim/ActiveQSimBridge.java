package org.matsim.core.mobsim.qsim;

import com.google.inject.Singleton;

/**
 * This class is part of the refactoring of the QSim. For now, OTFVis is rather
 * tightly coupled with the QSim and therefore counteracts making the QSim more
 * dependency injection-friendly.
 * 
 * The idea is the following: ActiveQSimBridge lives in the outer controller
 * scope. Whenever a new QSim is created, it registers here and any other
 * component of MATSim that needs a handle to the current QSim can obtain it
 * from here.
 * 
 * This mostly replaces cases where people used e.g. MobsimInitializedEvent to
 * get a handle of the QSim. This happens only 2-3 times in the whole code base
 * and would be mostly avoidable by further cleaning up the injection tree.
 * 
 * Eventually, these issues should be resolved and this class should be deleted.
 * 
 * @author shoerl
 *
 */
@Singleton
public class ActiveQSimBridge {
	private QSim activeQSim;

	public void setActiveQSim(QSim activeQSim) {
		this.activeQSim = activeQSim;
	}
	
	public boolean hasActiveQSim() {
		return activeQSim != null;
	}

	public QSim getActiveQSim() {
		if (activeQSim == null) {
			throw new IllegalStateException("There is no active QSim at the moment.");
		}

		return activeQSim;
	}
}

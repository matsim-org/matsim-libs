package playground.gregor.sim2d_v2.simulation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;

public class HybridQ2DMobsimFactory implements MobsimFactory {

	private final QSimFactory qSimFactory = new QSimFactory();


	@Override
	public Simulation createMobsim(Scenario sc, EventsManager eventsManager) {
		QSim qsim = (QSim)this.qSimFactory.createMobsim(sc, eventsManager);
		Sim2DEngine e = new Sim2DEngine(qsim);
		qsim.addMobsimEngine(e);
		Sim2DDepartureHandler d = new Sim2DDepartureHandler(e);
		qsim.addDepartureHandler(d);
		return qsim;
	}

}

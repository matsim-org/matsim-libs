package playground.gregor.casim.simulation;

import org.matsim.core.mobsim.qsim.QSim;

public final class CANetsimEngineModule {

	public static void configure(QSim qSim) {
        CANetsimEngine cae = new CANetsimEngine(qSim);
        qSim.addMobsimEngine(cae);
        qSim.addDepartureHandler(cae.getDepartureHandler());
		
	}

}

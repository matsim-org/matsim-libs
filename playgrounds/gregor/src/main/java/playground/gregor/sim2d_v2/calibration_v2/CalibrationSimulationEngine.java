package playground.gregor.sim2d_v2.calibration_v2;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;

import playground.gregor.sim2d_v2.calibration_v2.floor.PhantomFloor;
import playground.gregor.sim2d_v2.calibration_v2.scenario.PhantomEvents;
import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;

public class CalibrationSimulationEngine {

	private final Scenario2DImpl sc;
	private final EventsManager em;
	private final PhantomEvents pe;
	private final double timeIncr;
	private final Validator v;

	public CalibrationSimulationEngine(Scenario2DImpl  sc, PhantomEvents pe, EventsManager em, Validator v) {
		this.sc = sc;
		this.em = em;
		this.pe = pe;
		this.timeIncr = ((Sim2DConfigGroup)sc.getConfig().getModule("sim2d")).getTimeStepSize();
		this.v = v;
	}


	public void doOneIteration(Id id) {

		for (int i = 0; i < 20; i ++) {
			id = new IdImpl(i);
			PhantomFloor pf = new PhantomFloor(this.pe,id, this.sc.getNetwork().getLinks().values(), this.sc, this.v, this.em);

			double time = this.pe.getTimesArray()[0];
			double endTime = this.pe.getTimesArray()[this.pe.getTimesArray().length-1];

			pf.init();
			while (time <= endTime) {
				pf.move(time);
				time += this.timeIncr;
			}

		}
	}

}

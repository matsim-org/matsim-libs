package playground.gregor.sim2d_v2.calibration_v2;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.gregor.sim2d_v2.calibration_v2.floor.PhantomFloor;
import playground.gregor.sim2d_v2.calibration_v2.scenario.PhantomEvents;
import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;

public class CalibrationSimulationEngine {

	private final Scenario sc;
	private final EventsManager em;
	private final PhantomEvents pe;
	private final double timeIncr;
	private final LLCalculator llCalc;

	public CalibrationSimulationEngine(Scenario  sc, PhantomEvents pe, LLCalculator llCalc) {
		this.sc = sc;
		this.em = EventsUtils.createEventsManager();
		this.pe = pe;
		this.timeIncr = ((Sim2DConfigGroup)sc.getConfig().getModule("sim2d")).getTimeStepSize();
		this.llCalc = llCalc;

	}


	public void doOneIteration(List<Id> ids) {

		for (Id id : ids) {
			PhantomFloor pf = new PhantomFloor(this.pe,id, this.sc.getNetwork().getLinks().values(), this.sc, this.llCalc, this.em);

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

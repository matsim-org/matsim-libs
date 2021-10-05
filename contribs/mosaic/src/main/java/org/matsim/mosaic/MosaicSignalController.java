package org.matsim.mosaic;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.controller.SignalController;
import org.matsim.contrib.signals.controller.SignalControllerFactory;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.sensor.DownstreamSensor;
import org.matsim.contrib.signals.sensor.LinkSensorManager;

/**
 * Delegates signal controlling to mosaic logic.
 */
public class MosaicSignalController implements SignalController {

	// TODO: LinkEnterEventHandler, LinkLeaveEventHandler to count vehicles ?
	// sensor manager probably not needed

	public static final String IDENTIFIER = "MosaicSignalController";
	private final SignalSystem system;

	private final LinkSensorManager sensorManager;
	private final DownstreamSensor downstreamSensor;
	private final Scenario scenario;

	public MosaicSignalController(SignalSystem system, LinkSensorManager sensorManager, DownstreamSensor downstreamSensor, Scenario scenario) {
		this.system = system;
		this.sensorManager = sensorManager;
		this.downstreamSensor = downstreamSensor;
		this.scenario = scenario;
	}


	@Override
	public void updateState(double timeSeconds) {
		System.out.println("update state " + timeSeconds);
		// TODO
	}

	@Override
	public void addPlan(SignalPlan plan) {
		System.out.println("plan");
		System.out.println(plan);
	}

	@Override
	public void simulationInitialized(double simStartTimeSeconds) {

		System.out.println("init");
	}

	@Override
	public void setSignalSystem(SignalSystem signalSystem) {
		throw new UnsupportedOperationException("Signal system can not be changed.");
	}

	/**
	 * Factory for creating controllers.
	 */
	public static class Factory implements SignalControllerFactory {

		@Inject
		private LinkSensorManager sensorManager;
		@Inject
		private DownstreamSensor downstreamSensor;
		@Inject
		private Scenario scenario;


		@Override
		public SignalController createSignalSystemController(SignalSystem signalSystem) {
			return new MosaicSignalController(signalSystem, sensorManager, downstreamSensor, scenario);
		}
	}
}

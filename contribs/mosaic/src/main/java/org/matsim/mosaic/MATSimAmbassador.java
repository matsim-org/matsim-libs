package org.matsim.mosaic;

import org.agrona.concurrent.BackoffIdleStrategy;
import org.eclipse.mosaic.lib.util.objects.ObjectInstantiation;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;
import org.eclipse.mosaic.rti.api.*;
import org.eclipse.mosaic.rti.api.parameters.AmbassadorParameter;
import org.eclipse.mosaic.rti.config.CLocalHost;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ambassador connecting mosaic with MATSim.
 */
public final class MATSimAmbassador extends AbstractFederateAmbassador implements EventProcessor,
		ControlerListener, IterationStartsListener, IterationEndsListener,
		MobsimBeforeSimStepListener, MobsimAfterSimStepListener {

	/**
	 * MATSim mosaic config.
	 */
	private final CMATSimMosaic matsimConfig;

	/**
	 * Executor for the MATSim scenario.
	 */
	private MATSimFederateExecutor executor;

	/**
	 * Next requested time step.
	 */
	private final AtomicLong nextTimeStep = new AtomicLong();

	/**
	 * Current MATSim time.
	 */
	private final AtomicLong matsimTime = new AtomicLong();

	private final BackoffIdleStrategy mosaicIdle = new BackoffIdleStrategy();
	private final BackoffIdleStrategy matsimIdle = new BackoffIdleStrategy();

	public MATSimAmbassador(AmbassadorParameter ambassadorParameter) throws InstantiationException {
		super(ambassadorParameter);

		matsimConfig = new ObjectInstantiation<>(CMATSimMosaic.class, log)
				.readFile(ambassadorParameter.configuration);

		if (matsimConfig.scenario == null)
			throw new IllegalArgumentException("MATSim 'scenario' not configured!");

	}


	/**
	 * Returns the rti ambassador, so it can be used within MATSim to send interactions directly.
	 */
	public RtiAmbassador getRti() {
		return rti;
	}

	@Override
	public boolean isTimeConstrained() {
		return true;
	}

	@Override
	public boolean isTimeRegulating() {
		return true;
	}

	@Override
	public boolean canProcessEvent() {
		return true;
	}

	@Override
	public void initialize(long startTime, long endTime) throws InternalFederateException {
		super.initialize(startTime, endTime);

		matsimTime.set(-1);
		nextTimeStep.set(startTime);

		executor.startLocalFederate(this);

		try {
			rti.requestAdvanceTime(0);
		} catch (IllegalValueException e) {
			throw new InternalFederateException("Error requesting first time", e);
		}
	}

	@Nonnull
	@Override
	public FederateExecutor createFederateExecutor(String host, int port, CLocalHost.OperatingSystem os) {
		executor = new MATSimFederateExecutor(matsimConfig);
		return executor;
	}

	@Override
	public void processEvent(Event event) throws Exception {
		System.out.println(event);
	}

	@Override
	protected void processInteraction(Interaction interaction) throws InternalFederateException {
		System.out.println(interaction);
	}

	@Override
	protected void processTimeAdvanceGrant(long time) throws InternalFederateException {

		nextTimeStep.set(time);

		// Wait for MATSim to simulate until "time"
		while (matsimTime.get() < time) {
			//System.out.println("process " + time + " vs. " + matsimTime.get());
			executor.checkError();
			mosaicIdle.idle();
		}

		// TODO: collect and send out interactions

		matsimIdle.reset();

	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		try {
			rti.requestAdvanceTime(toLong(e.getSimulationTime()));
		} catch (IllegalValueException ex) {
			ex.printStackTrace();
		}
	}


	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

		long time = toLong(e.getSimulationTime());

		matsimTime.set(time);
		// Wait for Mosaic to request simulation time advance
		while (time >= nextTimeStep.get()) {
			//System.out.println("wait for " + e.getSimulationTime());
			matsimIdle.idle();
		}
		matsimIdle.reset();

	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {


	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		matsimTime.set(endTime);
		try {
			rti.requestAdvanceTime(endTime);
		} catch (IllegalValueException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Converts matsim time to mosaic representation.
	 */
	private static long toLong(double time) {
		return ((long) (time * 1_000)) * 1_000_000;
	}
}

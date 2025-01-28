package org.matsim.modechoice;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;


/**
 * Annealer for mode choice weights.
 */
public final class ModeChoiceWeightScheduler implements StartupListener, IterationStartsListener {

	private static final Logger log = LogManager.getLogger(ModeChoiceWeightScheduler.class);

	/**
	 * Starting beta = t_0
	 */
	private double startBeta;
	private double currentBeta;

	/**
	 * Number of iterations
	 */
	private int n;

	private InformedModeChoiceConfigGroup.Schedule anneal;

	@Inject
	public ModeChoiceWeightScheduler(Config config) {
		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		startBeta = currentBeta = imc.getInvBeta();
		anneal = imc.getAnneal();
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		Config config = event.getServices().getConfig();

		// The first iteration does not do any replanning
		n = config.controller().getLastIteration() - 1;
		double disableInnovation = config.replanning().getFractionOfIterationsToDisableInnovation();
		if (disableInnovation > 0 && disableInnovation < 1)
			n *= disableInnovation;

		if (anneal != InformedModeChoiceConfigGroup.Schedule.off)
			log.info("Annealing over {} iterations", n);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {

		if (anneal == InformedModeChoiceConfigGroup.Schedule.off || event.getIteration() == 0 || currentBeta == Double.POSITIVE_INFINITY)
			return;

		// anneal target is 0, iterations are offset by 1 because first iteration does not do replanning
		currentBeta = anneal(anneal, startBeta, n, event.getIteration() - 1);

		// Never fall below 0
		currentBeta = currentBeta < 0 ? 0 : currentBeta;

		log.info("Setting invBeta parameter to {} in iteration {}", currentBeta, event.getIteration());
	}

	public double getInvBeta() {
		return currentBeta;
	}

	// https://nathanrooy.github.io/posts/2020-05-14/simulated-annealing-with-python/

	/**
	 * Anneal temperature t_0.
	 */
	public static double anneal(InformedModeChoiceConfigGroup.Schedule schedule, double t_0, double n, double k) {
		return switch (schedule) {
			case linear -> linear(t_0, n, k);
			case quadratic ->  quadratic(t_0, n, k);
			case cubic -> cubic(t_0, n, k);
			case exponential -> exponential(t_0, n, k);
			case trigonometric -> trigonometric(t_0, n, k);
			default -> throw new IllegalStateException("Unknown annealing schedule");
		};
	}

	private static double linear(double t_0, double n, double k) {
		return t_0 * ((n - k) / n);
	}

	private static double quadratic(double t_0, double n, double k) {
		return t_0 * Math.pow((n - k) / n, 2);
	}

	private static double cubic(double t_0, double n, double k) {
		return t_0 * Math.pow((n - k) / n, 3);
	}

	private static double exponential(double t_0, double n, double k) {
		return t_0 * Math.pow(0.95, k);
	}

	private static double fast(double t_0, double n, double k) {
		return t_0 / (k + 1);
	}

	private static double trigonometric(double t_0, double n, double k) {
		return 0.5 * t_0 * (1 + Math.cos(k * Math.PI / n));
	}

}

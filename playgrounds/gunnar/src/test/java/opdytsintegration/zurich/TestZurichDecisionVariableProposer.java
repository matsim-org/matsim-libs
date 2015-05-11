package opdytsintegration.zurich;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import optdyts.DecisionVariableProposer;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class TestZurichDecisionVariableProposer implements
		DecisionVariableProposer<TestZurichState, TestZurichDecisionVariable>,
		StartupListener {

	// CONSTANTS

	private final double scale;

	private final Random rnd;

	// MEMBERS

	private Map<Link, Double> link2freespeed = null;

	private Map<Link, Double> link2capacity = null;

	// CONSTRUCTION

	TestZurichDecisionVariableProposer(final double range, final Random rnd) {
		this.scale = range;
		this.rnd = rnd;
	}

	// IMPLEMENTATION OF StartupListener

	@Override
	public void notifyStartup(final StartupEvent event) {
		this.link2freespeed = new LinkedHashMap<Link, Double>();
		this.link2capacity = new LinkedHashMap<Link, Double>();
		for (Link link : event.getControler().getScenario().getNetwork()
				.getLinks().values()) {
			this.link2freespeed.put(link, link.getFreespeed());
			this.link2capacity.put(link, link.getCapacity());
		}
		this.link2freespeed = Collections.unmodifiableMap(this.link2freespeed);
		this.link2capacity = Collections.unmodifiableMap(this.link2capacity);
	}

	// IMPLEMENTATION OF SurrogateSolutionDecisionVariableProposer

	@Override
	public TestZurichDecisionVariable nextDecisionVariable() {
		final double betaPay = this.rnd.nextDouble();
		final double betaAlloc = this.rnd.nextDouble();
		return new TestZurichDecisionVariable(betaPay, betaAlloc,
				this.link2freespeed, this.link2capacity);
	}

	// TODO continue here.

	@Override
	public TestZurichDecisionVariable nextDecisionVariable(
			final TestZurichDecisionVariable referenceDecisionVariable) {
		final double betaPay = Math.max(
				0,
				Math.min(1, referenceDecisionVariable.betaPay() + this.scale
						* this.rnd.nextGaussian()));
		final double betaAlloc = Math.max(
				0,
				Math.min(1, referenceDecisionVariable.betaAlloc() + this.scale
						* this.rnd.nextGaussian()));
		return new TestZurichDecisionVariable(betaPay, betaAlloc,
				this.link2freespeed, this.link2capacity);
	}
}

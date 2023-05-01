package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import ch.sbb.matsim.contrib.railsim.prototype.RailsimLinkSpeedCalculator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

public final class QRailsimSignalsNetworkFactory implements QNetworkFactory {
	private final QNetworkFactory delegate;
	private RailsimLinkSpeedCalculator linkSpeedCalculator;
	private final Scenario scenario;
	private final EventsManager events;
	private NetsimEngineContext context;
	private NetsimInternalInterface netsimEngine;

	@Inject
	public QRailsimSignalsNetworkFactory(Scenario scenario, EventsManager events) {
		this.scenario = scenario;
		this.events = events;

		// TODO throw runtime exception if lanes is switched on...
		this.delegate = new QSignalsNetworkFactory(scenario, events);
	}

	@Override
	public void initializeFactory(AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface simEngine1) {
		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis(scenario.getConfig().qsim().getLinkWidthForVis());
		linkWidthCalculator.setLaneWidth(scenario.getNetwork().getEffectiveLaneWidth());

		AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = AbstractQNetsimEngine.createAgentSnapshotInfoBuilder(scenario, linkWidthCalculator);

		this.netsimEngine = simEngine1;
		this.context = new NetsimEngineContext(events, scenario.getNetwork().getEffectiveCellSize(), agentCounter, agentSnapshotInfoBuilder, scenario.getConfig().qsim(), mobsimTimer, linkWidthCalculator);

		delegate.initializeFactory(agentCounter, mobsimTimer, simEngine1);
	}

	@Override
	public QNodeI createNetsimNode(Node node) {
		return delegate.createNetsimNode(node);
	}

	@Override
	public QLinkI createNetsimLink(Link link, QNodeI queueNode) {
		QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, netsimEngine);
		linkBuilder.setLinkSpeedCalculator(this.linkSpeedCalculator);
		return linkBuilder.build(link, queueNode);
	}

	/**
	 * @param calculator
	 */
	public void setLinkSpeedCalculator(RailsimLinkSpeedCalculator calculator) {
		this.linkSpeedCalculator = calculator;
	}

}

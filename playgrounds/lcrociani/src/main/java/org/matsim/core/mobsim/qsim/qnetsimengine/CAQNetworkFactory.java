package org.matsim.core.mobsim.qsim.qnetsimengine;

import matsimConnector.engine.CAAgentFactory;
import matsimConnector.engine.CAEngine;
import matsimConnector.environment.TransitionArea;
import matsimConnector.scenario.CAEnvironment;
import matsimConnector.scenario.CAScenario;
import matsimConnector.utility.Constants;
import matsimConnector.utility.IdUtility;
import matsimConnector.utility.LinkUtility;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import pedCA.environment.markers.TacticalDestination;

public class CAQNetworkFactory implements NetsimNetworkFactory<QNode, QLinkInternalI> {
	
	private CAEngine engineCA;
	private CAAgentFactory agentFactoryCA;
	private CAScenario scenarioCA;

	public CAQNetworkFactory(CAEngine engineCA, Scenario scenario, CAAgentFactory agentFactoryCA) {
		this.engineCA = engineCA;
		this.agentFactoryCA = agentFactoryCA;
		this.scenarioCA = (CAScenario) scenario.getScenarioElement(Constants.CASCENARIO_NAME);
	}
	
	@Override
	public QNode createNetsimNode(Node node, QNetwork network) {
		return new QNode(node, network);
	}

	@Override
	public QLinkInternalI createNetsimLink(Link link, QNetwork network,	QNode toQueueNode) {
		QLinkInternalI qLink = null;
		qLink = new QLinkImpl(link, network, toQueueNode);

		boolean isCAQLink = link.getAllowedModes().contains(Constants.TO_Q_LINK_MODE);
		boolean isQCALink = link.getAllowedModes().contains(Constants.TO_CA_LINK_MODE);
		
		if (isQCALink) {
			QCALink hiResLink = createQCALink(link, network, qLink);
			return hiResLink;
		} 
		if (isCAQLink) {
			createCAQLink(qLink);
			return qLink;
		}
		
		return qLink;
	}

	protected void createCAQLink(QLinkInternalI qLink) {
		CAQLink lowResLink = new CAQLink(qLink);
		this.engineCA.registerLowResLink(lowResLink);
	}

	private QCALink createQCALink(Link link, QNetwork network, QLinkInternalI qLink) {
		CAEnvironment environmentCA = this.scenarioCA.getCAEnvironment(link);
		int rows = LinkUtility.getTransitionAreaWidth(link, environmentCA);
		int columns = Constants.TRANSITION_AREA_COLUMNS;
		int destinationId = IdUtility.linkIdToDestinationId(link.getId());
		TacticalDestination destination = environmentCA.getDestination(destinationId);
		TransitionArea transitionArea = new TransitionArea(rows,columns,destination.getRotation(),destination.getEnvironmentRef());
		QCALink linkQCA = new QCALink(link, network, qLink, environmentCA, this.agentFactoryCA, transitionArea);
		engineCA.registerHiResLink(linkQCA);
		environmentCA.addTransitionArea(linkQCA.getLinkId(), transitionArea);
		return linkQCA;
	}
}

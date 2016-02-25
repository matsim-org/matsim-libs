package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.Map;

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

import pedCA.environment.markers.FinalDestination;

public class CAQNetworkFactory implements NetsimNetworkFactory {
	
	private CAEngine engineCA;
	private CAAgentFactory agentFactoryCA;
	private CAScenario scenarioCA;
	private Map<Node, TransitionArea> nodeToTransitionArea = new HashMap<Node, TransitionArea>();

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
	public QLinkI createNetsimLink(Link link, QNetwork network,	QNode toQueueNode) {
		QLinkI qLink = null;
		qLink = new QLinkImpl(link, network, toQueueNode);

		boolean isCAQLink = link.getAllowedModes().contains(Constants.TO_Q_LINK_MODE);
		boolean isQCALink = link.getAllowedModes().contains(Constants.TO_CA_LINK_MODE);
		boolean isCALink = link.getAllowedModes().contains(Constants.CA_LINK_MODE);
		
		if (isQCALink) {
			QCALink hiResLink = createQCALink(link, network, qLink);
			return hiResLink;
		} 
		else if (isCAQLink) {
			createCAQLink(network, qLink);
			return qLink;
		}
		else if (isCALink){
			createCALink(network, qLink);
			return qLink;
		}
		return qLink;
	}

	private CALink createCALink(QNetwork network, QLinkI qLink) {
		CALink linkCA = new CALink(network, qLink);
		this.engineCA.registerCALink(linkCA);
		return linkCA;
	}

	protected CAQLink createCAQLink(QNetwork network, QLinkI qLink) {
		CAEnvironment environmentCA = this.scenarioCA.getCAEnvironment(qLink.getLink());
		Node borderNode = qLink.getLink().getFromNode();
		TransitionArea transitionArea;
		if (nodeToTransitionArea.containsKey(borderNode))
			transitionArea = nodeToTransitionArea.get(borderNode);
		else
			transitionArea = createTransitionArea(borderNode, environmentCA);
		CAQLink lowResLink = new CAQLink(network, qLink, transitionArea);
		this.engineCA.registerLowResLink(lowResLink);
		return lowResLink;
	}

	private QCALink createQCALink(Link link, QNetwork network, QLinkI qLink) {
		CAEnvironment environmentCA = this.scenarioCA.getCAEnvironment(link);
		Node borderNode = link.getToNode();
		TransitionArea transitionArea;
		if (nodeToTransitionArea.containsKey(borderNode))
			transitionArea = nodeToTransitionArea.get(borderNode);
		else
			transitionArea = createTransitionArea(borderNode, environmentCA);
		QCALink linkQCA = new QCALink(link, network, qLink, environmentCA, this.agentFactoryCA, transitionArea);
		this.engineCA.registerHiResLink(linkQCA);
		environmentCA.addTransitionArea(linkQCA.getLinkId(), transitionArea);
		return linkQCA;
	}

	public TransitionArea createTransitionArea(Node borderNode, CAEnvironment environmentCA) {
		int rows = LinkUtility.getTransitionAreaWidth(borderNode, environmentCA);
		int columns = Constants.TRANSITION_AREA_COLUMNS;
		int destinationId = IdUtility.nodeIdToDestinationId(borderNode.getId());
		FinalDestination destination = environmentCA.getDestination(destinationId);
		TransitionArea transitionArea = new TransitionArea(rows,columns,destination);
		destination.setTransitionArea(transitionArea);
		registerTransitionArea(borderNode, transitionArea, environmentCA);
		return transitionArea;
	}
	
	private void registerTransitionArea(Node node, TransitionArea transitionArea, CAEnvironment environmentCA){
		environmentCA.getContext().registerTransitionArea(transitionArea);
		this.nodeToTransitionArea.put(node,transitionArea);
	}
}

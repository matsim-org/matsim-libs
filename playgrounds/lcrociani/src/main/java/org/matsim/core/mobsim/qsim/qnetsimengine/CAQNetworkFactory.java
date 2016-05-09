package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.network.NetworkImpl;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import pedCA.environment.markers.FinalDestination;

public class CAQNetworkFactory extends QNetworkFactory {
	private final EventsManager events ;
	private final Network network ;
	private final QSimConfigGroup qsimConfig ;
	private final Scenario scenario ;
	
	private CAEngine engineCA;
	private CAAgentFactory agentFactoryCA;
	private CAScenario scenarioCA;
	private Map<Node, TransitionArea> nodeToTransitionArea = new HashMap<Node, TransitionArea>();
	private NetsimEngineContext context;
	private NetsimInternalInterface netsimEngine;
	
	@Inject
	public CAQNetworkFactory( EventsManager events, Network network, QSimConfigGroup qsimConfig, Scenario scenario ) {
		// there is a bit of replication in the above arguments. kai, mar'31
		
		this.events = events ;
		this.network = network;
		this.qsimConfig = qsimConfig;
		this.scenario = scenario;
	}

	public CAQNetworkFactory(CAEngine engineCA, Scenario scenario, CAAgentFactory agentFactoryCA, EventsManager events) {
		this.engineCA = engineCA;
		this.agentFactoryCA = agentFactoryCA;
		this.scenarioCA = (CAScenario) scenario.getScenarioElement(Constants.CASCENARIO_NAME);
		this.scenario = scenario ;
		this.network = scenario.getNetwork() ;
		this.qsimConfig = scenario.getConfig().qsim() ;
		this.events = events ;
	}
	
	@Override
	void initializeFactory(AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface netsimEngine1) {
		double effectiveCellSize = ((NetworkImpl) network).getEffectiveCellSize() ;

		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
		if (! Double.isNaN(network.getEffectiveLaneWidth())){
			linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );
		}
		AgentSnapshotInfoFactory snapshotInfoFactory = new AgentSnapshotInfoFactory(linkWidthCalculator);
		AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );

		this.context = new NetsimEngineContext(events, effectiveCellSize, agentCounter, snapshotInfoBuilder, qsimConfig, mobsimTimer, 
				linkWidthCalculator ) ;
		
		this.netsimEngine = netsimEngine1 ;
	}
	
	@Override
	public QNode createNetsimNode(Node node) {
		QNode.Builder builder = new QNode.Builder( netsimEngine, context ) ;
		return builder.build( node ) ;
	}

	@Override
	public QLinkI createNetsimLink(Link link, QNode toQueueNode) {
		QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, netsimEngine ) ;
		QLinkI qLink = linkBuilder.build(link, toQueueNode) ;
		
		boolean isCAQLink = link.getAllowedModes().contains(Constants.TO_Q_LINK_MODE);
		boolean isQCALink = link.getAllowedModes().contains(Constants.TO_CA_LINK_MODE);
		boolean isCALink = link.getAllowedModes().contains(Constants.CA_LINK_MODE);
		
		if (isQCALink) {
			QCALink hiResLink = createQCALink(link, qLink);
			return hiResLink;
		} 
		else if (isCAQLink) {
			createCAQLink(qLink, context);
			return qLink;
		}
		else if (isCALink){
			createCALink(qLink, context);
			return qLink;
		}
		return qLink;
	}

	private CALink createCALink(QLinkI qLink, NetsimEngineContext context) {
		CALink linkCA = new CALink(qLink, context);
		this.engineCA.registerCALink(linkCA);
		return linkCA;
	}

	protected CAQLink createCAQLink(QLinkI qLink, NetsimEngineContext context) {
		CAEnvironment environmentCA = this.scenarioCA.getCAEnvironment(qLink.getLink());
		Node borderNode = qLink.getLink().getFromNode();
		TransitionArea transitionArea;
		if (nodeToTransitionArea.containsKey(borderNode))
			transitionArea = nodeToTransitionArea.get(borderNode);
		else
			transitionArea = createTransitionArea(borderNode, environmentCA);
		CAQLink lowResLink = new CAQLink( qLink, transitionArea, context );
		this.engineCA.registerLowResLink(lowResLink);
		return lowResLink;
	}

	private QCALink createQCALink(Link link, QLinkI qLink) {
		CAEnvironment environmentCA = this.scenarioCA.getCAEnvironment(link);
		Node borderNode = link.getToNode();
		TransitionArea transitionArea;
		if (nodeToTransitionArea.containsKey(borderNode))
			transitionArea = nodeToTransitionArea.get(borderNode);
		else
			transitionArea = createTransitionArea(borderNode, environmentCA);
		QCALink linkQCA = new QCALink(link, environmentCA, this.agentFactoryCA, transitionArea, context, netsimEngine);
		this.engineCA.registerHiResLink(linkQCA);
		environmentCA.addTransitionArea(linkQCA.getLink().getId(), transitionArea);
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

package org.matsim.vis.otfvis;

import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.otfvis.FacilityDrawer;
import org.matsim.pt.queuesim.TransitQueueSimulation;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.QueueSimulation;
import org.matsim.ptproject.qsim.QueueSimulationFeature;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataReader;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataWriter;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDrawer;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsLayer;
import org.matsim.vis.otfvis.data.teleportation.TeleportationVisData;
import org.matsim.vis.otfvis.server.OnTheFlyServer;

public class OTFVisQueueSimFeature implements QueueSimulationFeature {
	
	protected OnTheFlyServer otfServer = null;
	private boolean ownServer = true;
	private boolean doVisualizeTeleportedAgents = false;
	private OTFConnectionManager connectionManager = new DefaultConnectionManagerFactory().createConnectionManager();
	private OTFTeleportAgentsDataWriter teleportationWriter;
	private QueueSimulation queueSimulation;
	private final LinkedHashMap<Id, TeleportationVisData> visTeleportationData = new LinkedHashMap<Id, TeleportationVisData>();
	private final LinkedHashMap<Id, Integer> currentActivityNumbers = new LinkedHashMap<Id, Integer>();
	
	public OTFVisQueueSimFeature(QueueSimulation queueSimulation) {
		this.queueSimulation = queueSimulation;
	}
	
	public void setServer(OnTheFlyServer server) {
		this.otfServer = server;
		ownServer = false;
	}

	public void afterPrepareSim() {
		if(ownServer) {
			UUID idOne = UUID.randomUUID();
			this.otfServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), queueSimulation.getNetwork(), queueSimulation.getPopulation(), queueSimulation.getEvents(), false);
			this.otfServer.setSimulation(this);
			if (this.doVisualizeTeleportedAgents){
				this.teleportationWriter = new OTFTeleportAgentsDataWriter();
				this.otfServer.addAdditionalElement(this.teleportationWriter);
				this.connectionManager.add(OTFTeleportAgentsDataWriter.class, OTFTeleportAgentsDataReader.class);
				this.connectionManager.add(OTFTeleportAgentsDataReader.class, OTFTeleportAgentsDrawer.class);
				this.connectionManager.add(OTFTeleportAgentsDrawer.class, OTFTeleportAgentsLayer.class);
				
			}
			if (queueSimulation instanceof TransitQueueSimulation) {
				this.otfServer.addAdditionalElement(new FacilityDrawer.DataWriter_v1_0(
						queueSimulation.getNetwork().getNetworkLayer(), ((ScenarioImpl) queueSimulation.getScenario()).getTransitSchedule(),
						((TransitQueueSimulation) queueSimulation).getAgentTracker()));
				this.connectionManager.add(FacilityDrawer.DataWriter_v1_0.class, FacilityDrawer.DataReader_v1_0.class);
				this.connectionManager.add(FacilityDrawer.DataReader_v1_0.class, FacilityDrawer.DataDrawer.class);
			}
			OTFClientLive client = null;
			client = new OTFClientLive("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString(), this.connectionManager);
			if (this.queueSimulation.getScenario() != null) {
				client.setConfig(this.queueSimulation.getScenario().getConfig().otfVis());
			}
			client.start();

			try {
				this.otfServer.pause();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void beforeCleanupSim() {
		if(ownServer) {
			this.otfServer.cleanup();
		}
		this.otfServer = null;
	}

	public void beforeHandleAgentArrival(DriverAgent agent) {
		this.visTeleportationData.remove(agent.getPerson().getId());
	}

	public void afterAfterSimStep(final double time) {
		this.visualizeTeleportedAgents(time);
		this.otfServer.updateStatus(time);
	}
	
	public void beforeHandleUnknownLegMode(double now, final DriverAgent agent, Link link) {
		this.visTeleportationData.put(agent.getPerson().getId() , new TeleportationVisData(now, agent, link, this.queueSimulation.getNetwork().getNetworkLayer().getLinks().get(agent.getDestinationLinkId())));
	}

	private void visualizeTeleportedAgents(double time) {
		if (this.doVisualizeTeleportedAgents) {
			this.teleportationWriter.setSrc(this.visTeleportationData);
		}
		for (TeleportationVisData teleportationVisData : visTeleportationData.values()) {
			teleportationVisData.calculatePosition(time);
		}
	}
	
	public void setConnectionManager(OTFConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}
	
	public void setVisualizeTeleportedAgents(boolean active) {
		this.doVisualizeTeleportedAgents = active;
	}

	public void afterCreateAgents() {
		
	}

	@Override
	public void afterActivityBegins(DriverAgent agent, int planElementIndex) {
		currentActivityNumbers.put(agent.getPerson().getId(), planElementIndex);
	}

	public LinkedHashMap<Id, Integer> getCurrentActivityNumbers() {
		return currentActivityNumbers;
	}

	@Override
	public void afterActivityEnds(DriverAgent agent, double time) {
		currentActivityNumbers.remove(agent.getPerson().getId());
	}

	public QueueSimulation getQueueSimulation() {
		return queueSimulation;
	}

	public Map<Id, TeleportationVisData> getVisTeleportationData() {
		return visTeleportationData;
	}
	
}
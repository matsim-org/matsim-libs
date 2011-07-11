package playground.gregor.pedvis;

import java.util.LinkedHashMap;
import java.util.UUID;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.opengl.layer.AgentPointDrawer;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.VisMobsim;
import org.matsim.vis.snapshots.writers.VisMobsimFeature;

import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZEventsHandler;
import playground.gregor.snapshots.writers.XYZAzimuthPositionInfo;

public class OTFVisMobsimFeature implements VisMobsimFeature,XYZEventsHandler{

	protected OnTheFlyServer otfServer = null;
	private final VisMobsim queueSimulation;

	private OTFAgentsListHandler.Writer walk2dWriter;
	private final LinkedHashMap<Id, AgentSnapshotInfo> visData = new LinkedHashMap<Id, AgentSnapshotInfo>();

	private final OTFConnectionManager connectionManager = new DefaultConnectionManagerFactory().createConnectionManager();

	public OTFVisMobsimFeature(VisMobsim queueSimulation) {
		this.queueSimulation = queueSimulation;
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		UUID idOne = UUID.randomUUID();
		this.otfServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), this.queueSimulation.getEventsManager());
		this.otfServer.setSimulation(this);


		this.walk2dWriter = new OTFAgentsListHandler.Writer();
		this.walk2dWriter.setSrc(this.visData.values());
		this.otfServer.addAdditionalElement(this.walk2dWriter);

		this.connectionManager.connectWriterToReader(
				OTFAgentsListHandler.Writer.class,
				OTFAgentsListHandler.class);
		this.connectionManager.connectReaderToReceiver(
				OTFAgentsListHandler.class,
				AgentPointDrawer.class);

		OTFClientLive client = new OTFClientLive(this.otfServer, this.connectionManager);
		new Thread(client).start();
		this.otfServer.pause();

	}

	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
		double time = e.getSimulationTime() ;
		this.otfServer.unblockUpdates();
		this.otfServer.updateStatus(time);

	}

	@Override
	public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.visData.remove(event.getPersonId());

	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(AdditionalTeleportationDepartureEvent eve) {
		// TODO Auto-generated method stub

	}

	@Override
	public VisMobsim getVisMobsim() {
		return this.queueSimulation;
	}

	@Override
	public Plan findPlan(Id agentId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addTrackedAgent(Id agentId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTrackedAgent(Id agentId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(XYZAzimuthEvent event) {
		XYZAzimuthPositionInfo info = new XYZAzimuthPositionInfo(event.getPersonId(), event.getCoordinate(), event.getAzimuth(), event.getTime());
		this.visData.put(event.getPersonId(), info);
	}

}

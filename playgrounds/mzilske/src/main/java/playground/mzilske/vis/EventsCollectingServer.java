package playground.mzilske.vis;

import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.SnapshotGenerator;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFServerQuadI;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.SnapshotWriter;

public final class EventsCollectingServer implements OTFServerRemote {
	
	public static class TimeStep {
		
		public Collection<AgentSnapshotInfo> agentPositions = new ArrayList<AgentSnapshotInfo>();
		
	}

	private Network network;
	
	private final OTFAgentsListHandler.Writer agentWriter = new OTFAgentsListHandler.Writer();
	
	private MyQuadTree quadTree;
	
	private final ByteBuffer byteBuffer = ByteBuffer.allocate(20000000);
	
	private double nextTime = -1;
	
	private TreeMap<Double, TimeStep> timeSteps = new TreeMap<Double, TimeStep>();

	private TimeStep nextTimeStep;

	private class MyQuadTree extends OTFServerQuad2 {

		private static final long serialVersionUID = 1L;

		public MyQuadTree() {
			super(network);
		}

		@Override
		public void initQuadTree(OTFConnectionManager connect) {
			initQuadTree();
		}

		private void initQuadTree() {
			for (Link link : network.getLinks().values()) {
				double middleEast = (link.getToNode().getCoord().getX() + link.getFromNode().getCoord().getX()) * 0.5 - this.minEasting;
				double middleNorth = (link.getToNode().getCoord().getY() + link.getFromNode().getCoord().getY()) * 0.5 - this.minNorthing;
				OTFDefaultLinkHandler.Writer linkWriter = new OTFDefaultLinkHandler.Writer();
				this.put(middleEast, middleNorth, linkWriter);
			}
			this.addAdditionalElement(agentWriter);
		}
		
	}
	
	private class SnapshotReceiver implements SnapshotWriter {

		TimeStep currentTimeStep;
		
		@Override
		public void addAgent(AgentSnapshotInfo position) {
			currentTimeStep.agentPositions.add(position);
		}

		@Override
		public void beginSnapshot(double time) {
			currentTimeStep = new TimeStep();
			timeSteps.put(time, currentTimeStep);
		}

		@Override
		public void endSnapshot() {
			
		}

		@Override
		public void finish() {
			
		}
		
	}
	
	public EventsCollectingServer(Network network, double snapshotPeriod, SimulationConfigGroup simulationConfigGroup) {
		this.network = network;
		SnapshotGenerator snapshotGenerator = new SnapshotGenerator(network, snapshotPeriod, simulationConfigGroup);
		SnapshotReceiver snapshotReceiver = new SnapshotReceiver();
		snapshotGenerator.addSnapshotWriter(snapshotReceiver);
		EventsManager eventsManager = new EventsManagerImpl();
		eventsManager.addHandler(snapshotGenerator);
		quadTree = new MyQuadTree();
	}

	@Override
	public int getLocalTime() throws RemoteException {
		return (int) nextTime;
	}

	@Override
	public OTFServerQuadI getQuad(String id, OTFConnectionManager connect) throws RemoteException {
		return quadTree;
	}

	@Override
	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		byte[] result;
		byteBuffer.position(0);
		quadTree.writeConstData(byteBuffer);
		int pos = byteBuffer.position();
		result = new byte[pos];
		byteBuffer.position(0);
		byteBuffer.get(result);
		return result;
	}

	@Override
	public byte[] getQuadDynStateBuffer(String id, Rect bounds) throws RemoteException {
		byte[] result;
		byteBuffer.position(0);
		agentWriter.positions.clear();
		agentWriter.positions.addAll(nextTimeStep.agentPositions);
		quadTree.writeDynData(bounds, byteBuffer);
		int pos = byteBuffer.position();
		result = new byte[pos];
		byteBuffer.position(0);
		byteBuffer.get(result);
		return result;
	}

	@Override
	public Collection<Double> getTimeSteps() throws RemoteException {
		return this.timeSteps.keySet();
	}

	@Override
	public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
		Double dTime = (double) time;
		Map.Entry<Double, TimeStep> entry;
		if (searchDirection == TimePreference.EARLIER) {
			entry = timeSteps.floorEntry(dTime);
		} else if (searchDirection == TimePreference.LATER) {
			entry = timeSteps.ceilingEntry(dTime);
		} else {
			throw new IllegalArgumentException();
		}
		if (entry == null) {
			return false;
		} else {
			this.nextTime = entry.getKey();
			this.nextTimeStep = entry.getValue();
			return true;
		}
	}

	@Override
	public void toggleShowParking() throws RemoteException {
		
	}

	@Override
	public boolean isLive() throws RemoteException {
		return false;
	}

	@Override
	public OTFVisConfig getOTFVisConfig() throws RemoteException {
		return new OTFVisConfig();
	}

}

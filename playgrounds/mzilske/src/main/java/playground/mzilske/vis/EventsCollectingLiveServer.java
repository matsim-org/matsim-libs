package playground.mzilske.vis;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.SnapshotGenerator;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFServerQuadI;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFQueryRemote;
import org.matsim.vis.otfvis.opengl.queries.AbstractQuery;
import org.matsim.vis.otfvis.opengl.queries.QueryQueueModel;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.SnapshotWriter;

public class EventsCollectingLiveServer implements OTFLiveServerRemote {
	
	private QueryServer queryServer;
	
	private final OTFAgentsListHandler.Writer agentWriter = new OTFAgentsListHandler.Writer();

	private MyQuadTree quadTree;
	
	private volatile boolean synchedPlayback = true;
	
	private TimeStep nextTimeStep;
	
	private ArrayBlockingQueue<TimeStep> timeStepBuffer = new ArrayBlockingQueue<TimeStep>(1);
	
	private final ByteBuffer byteBuffer = ByteBuffer.allocate(80000000);

	private Scenario scenario;

	private SnapshotReceiver snapshotReceiver;
	
	private final class CurrentTimeStepView implements QueryQueueModel {
		
		@Override
		public Collection<AgentSnapshotInfo> getSnapshot() {
			return nextTimeStep.agentPositions;
		}
		
	}

	private static class TimeStep implements Serializable {

		private static final long serialVersionUID = 1L;

		public Collection<AgentSnapshotInfo> agentPositions = new ArrayList<AgentSnapshotInfo>();
		
		public int time;
		
	}
	
	private class MyQuadTree extends OTFServerQuad2 {

		private static final long serialVersionUID = 1L;

		public MyQuadTree() {
			super(scenario.getNetwork());
		}

		@Override
		public void initQuadTree(OTFConnectionManager connect) {
			initQuadTree();
		}

		private void initQuadTree() {
			for (Link link : scenario.getNetwork().getLinks().values()) {
				double middleEast = (link.getToNode().getCoord().getX() + link.getFromNode().getCoord().getX()) * 0.5 - this.minEasting;
				double middleNorth = (link.getToNode().getCoord().getY() + link.getFromNode().getCoord().getY()) * 0.5 - this.minNorthing;
				LinkHandler.Writer linkWriter = new LinkHandler.Writer();
				linkWriter.setSrc(link);
				this.put(middleEast, middleNorth, linkWriter);
			}
			this.addAdditionalElement(agentWriter);
		}
		
	}
	
	private class SnapshotReceiver implements SnapshotWriter {
		
		private TimeStep timeStep;

		@Override
		public void addAgent(AgentSnapshotInfo position) {
			if (position.getAgentState() == AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY) return;
			timeStep.agentPositions.add(position);
		}

		@Override
		public void beginSnapshot(double time) {
			timeStep = new TimeStep();
			timeStep.time = (int) time;
		}

		@Override
		public void endSnapshot() {
			putTimeStep(timeStep);
		}

		private void putTimeStep(TimeStep timeStep2) {
			if (!synchedPlayback) {
				System.out.println("Clearing.");
				timeStepBuffer.clear();
				nextTimeStep = timeStep;
			}
			try {
				timeStepBuffer.put(timeStep);
			} catch (InterruptedException e) {
				
			}
		}

		@Override
		public void finish() {
			
		}
		
	}

	public EventsCollectingLiveServer(Scenario scenario, EventsManager eventsManager) {
		this.scenario = scenario;
		this.snapshotReceiver = new SnapshotReceiver();
		this.quadTree = new MyQuadTree();
		this.quadTree.initQuadTree();
		QueryQueueModel queueModel = new CurrentTimeStepView();
		this.queryServer = new QueryServer(scenario, eventsManager, queueModel);
	}

	@Override
	public OTFQueryRemote answerQuery(AbstractQuery query)
			throws RemoteException {
		return queryServer.answerQuery(query);
	}

	@Override
	public int getControllerStatus() throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void pause() throws RemoteException {
		synchedPlayback = true;
		System.out.println("pause");
	}

	@Override
	public void play() throws RemoteException {
		synchedPlayback = false;
		timeStepBuffer.clear();
		System.out.println("play");
	}

	@Override
	public void removeQueries() throws RemoteException {
		queryServer.removeQueries();
	}

	@Override
	public boolean requestControllerStatus(int status) throws RemoteException {
		System.out.println("Request controller status: "+status);
		return false;
	}

	@Override
	public int getLocalTime() throws RemoteException {
		if (nextTimeStep == null) {
			return 0;
		} else {
			return nextTimeStep.time;
		}
	}

	@Override
	public OTFVisConfigGroup getOTFVisConfig() throws RemoteException {
		return new OTFVisConfigGroup();
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
	public byte[] getQuadDynStateBuffer(String id, Rect bounds)
			throws RemoteException {
		byte[] result;
		byteBuffer.position(0);
		agentWriter.positions.clear();
		if (nextTimeStep != null) {
			agentWriter.positions.addAll(nextTimeStep.agentPositions);
		}
		quadTree.writeDynData(bounds, byteBuffer);
		int pos = byteBuffer.position();
		result = new byte[pos];
		byteBuffer.position(0);
		byteBuffer.get(result);
		return result;
	}

	@Override
	public Collection<Double> getTimeSteps() throws RemoteException {
		return null;
	}

	@Override
	public boolean isLive() throws RemoteException {
		return true;
	}

	@Override
	public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
		System.out.println("Requested: " + time);
		while(nextTimeStep == null || nextTimeStep.time < time) {
			try {
				nextTimeStep = timeStepBuffer.take();
				System.out.println("Got: " + nextTimeStep.time);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		return true;
	}

	@Override
	public void toggleShowParking() throws RemoteException {
		
	}

	SnapshotReceiver getSnapshotReceiver() {
		return snapshotReceiver;
	}

}

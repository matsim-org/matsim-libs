package playground.mzilske.vis;

import java.io.File;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.MobsimConfigGroupI;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
import org.matsim.vis.otfvis2.LinkHandler;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.SnapshotWriter;

import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.DatabaseException;


public final class EventsCollectingServer implements OTFServerRemote {
	
	public static class TimeStep implements Serializable {

		private static final long serialVersionUID = 1L;

		public Collection<AgentSnapshotInfo> agentPositions = new ArrayList<AgentSnapshotInfo>();
		
	}

	private Network network;
	
	private final OTFAgentsListHandler.Writer agentWriter;
	
	private MyQuadTree quadTree;
	
	private final ByteBuffer byteBuffer = ByteBuffer.allocate(80000000);
	
	private double nextTime = -1;
	
	private StoredSortedMap<Double, AgentSnapshotInfo> timeSteps;

	private TimeStep nextTimeStep;

	private MovieDatabase db;

	private File tempFile;

	private Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();

	private class MyQuadTree extends OTFServerQuadTree {

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
				LinkHandler.Writer linkWriter = new LinkHandler.Writer();
				linkWriter.setSrc(link);
				this.put(middleEast, middleNorth, linkWriter);
			}
			this.addAdditionalElement(agentWriter);
		}
		
	}
	
	private class SnapshotReceiver implements SnapshotWriter {

		double time;
		
		@Override
		public void addAgent(AgentSnapshotInfo position) {
			if (position.getAgentState() == AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY) return;
			timeSteps.put(time, position);
		}

		@Override
		public void beginSnapshot(double time) {
			this.time = time;
		}

		@Override
		public void endSnapshot() {
			
		}

		@Override
		public void finish() {
			
		}
		
	}
	
	public EventsCollectingServer(Network network, EventsManager eventsManager, double snapshotPeriod, MobsimConfigGroupI simulationConfigGroup) {
		agentWriter = new OTFAgentsListHandler.Writer();
		agentWriter.setSrc(positions);
		this.network = network;
		QueuelessSnapshotGenerator snapshotGenerator = new QueuelessSnapshotGenerator(network, (int) snapshotPeriod); 
		SnapshotReceiver snapshotReceiver = new SnapshotReceiver();
		snapshotGenerator.addSnapshotWriter(snapshotReceiver);
		eventsManager.addHandler(snapshotGenerator);
		quadTree = new MyQuadTree();
		quadTree.initQuadTree();
		try {
			tempFile = DirectoryUtils.createTempDirectory();
			db = new MovieDatabase(tempFile);
			MovieView view = new MovieView(db);
			timeSteps = view.getTimeStepMap();
		} catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public int getLocalTime() {
		if (nextTimeStep == null) {
			nextTime = timeSteps.firstKey();
			nextTimeStep = getTimeStep(nextTime);
		}
		return (int) nextTime;
	}

	private TimeStep getTimeStep(double time) {
		TimeStep timeStep = new TimeStep();
		timeStep.agentPositions = timeSteps.duplicates(time);
		return timeStep;
	}

	@Override
	public OTFServerQuadTree getQuad(String id, OTFConnectionManager connect) {
		return quadTree;
	}

	@Override
	public byte[] getQuadConstStateBuffer(String id) {
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
	public byte[] getQuadDynStateBuffer(String id, Rect bounds) {
		byte[] result;
		byteBuffer.position(0);
		positions.clear();
		if (nextTimeStep != null) {
			positions.addAll(nextTimeStep.agentPositions);
		}
		quadTree.writeDynData(bounds, byteBuffer);
		int pos = byteBuffer.position();
		result = new byte[pos];
		byteBuffer.position(0);
		byteBuffer.get(result);
		return result;
	}

	@Override
	public Collection<Double> getTimeSteps() {
		return this.timeSteps.keySet();
	}

	@Override
	public boolean requestNewTime(int time, TimePreference searchDirection) {
		Double dTime = (double) time;
		Double foundTime;
		if (searchDirection == TimePreference.EARLIER || searchDirection == TimePreference.RESTART) {
			foundTime = timeSteps.headMap(dTime + 1).lastKey();
		} else if (searchDirection == TimePreference.LATER) {
			foundTime = timeSteps.tailMap(dTime).firstKey();
		} else {
			throw new IllegalArgumentException();
		}
		
		if (foundTime == null) {
			return false;
		} else {
			this.nextTime = foundTime;
			this.nextTimeStep = getTimeStep(foundTime);
			return true;
		}
	}

	@Override
	public void toggleShowParking() {
		
	}

	@Override
	public boolean isLive() {
		return false;
	}

	@Override
	public OTFVisConfigGroup getOTFVisConfig() {
		return new OTFVisConfigGroup();
	}

	public void close() {
		db.close();
		DirectoryUtils.deleteDirectory(tempFile);
	}

}

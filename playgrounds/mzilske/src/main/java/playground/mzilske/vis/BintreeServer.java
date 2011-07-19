package playground.mzilske.vis;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfoFactory;

import playground.mzilske.vis.BintreeGenerator.Trajectory;


public final class BintreeServer implements OTFServerRemote {
	
	private static Logger logger = Logger.getLogger(BintreeServer.class);
	
	public static class TimeStep implements Serializable {

		private static final long serialVersionUID = 1L;

		public Collection<AgentSnapshotInfo> agentPositions = new ArrayList<AgentSnapshotInfo>();
		
	}

	private Network network;
	
	private final OTFAgentsListHandler.Writer agentWriter;
	
	private MyQuadTree quadTree;
	
	private final ByteBuffer byteBuffer = ByteBuffer.allocate(80000000);
	
	private double nextTime = -1;

	private TimeStep nextTimeStep;

	private BintreeGenerator bintreeGenerator;

	private double snapshotPeriod;

	private double lastTime = 86400.0;

	private TreeSet<Double> timeSteps;
	
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
	
	public BintreeServer(Network network, EventsManager eventsManager, double snapshotPeriod, MobsimConfigGroupI simulationConfigGroup) {
		this.network = network;
		agentWriter = new OTFAgentsListHandler.Writer();
		agentWriter.setSrc(positions);
		bintreeGenerator = new BintreeGenerator(network);
		quadTree = new MyQuadTree();
		quadTree.initQuadTree();
		eventsManager.addHandler(bintreeGenerator);
		this.snapshotPeriod = snapshotPeriod;
	}

	@Override
	public int getLocalTime() {
		if (nextTimeStep == null) {
			nextTime = snapshotPeriod;
			nextTimeStep = getTimeStep((int) nextTime);
		}
		return (int) nextTime;
	}

	private TimeStep getTimeStep(int time) {
		TimeStep timeStep = new TimeStep();
		List<Integer> candidates = bintreeGenerator.getBintree().query(time);
		for (Integer id : candidates) {
			Trajectory trajectory = bintreeGenerator.getTrajectory(id);
			if (trajectory.startTime <= time && time < trajectory.endTime) {
				Id personId = trajectory.personId;
				double easting = trajectory.x + (time - trajectory.startTime) * trajectory.dx;
				double northing = trajectory.y + (time - trajectory.startTime) * trajectory.dy; 
				AgentSnapshotInfo agentPositionInfo = AgentSnapshotInfoFactory.staticCreateAgentSnapshotInfo(personId, easting, northing, 0.0d, 0.0d);
				agentPositionInfo.setAgentState(AgentState.PERSON_DRIVING_CAR);
				timeStep.agentPositions.add(agentPositionInfo);
			}
		}
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
		this.positions.clear();
		if (nextTimeStep != null) {
			this.positions.addAll(nextTimeStep.agentPositions);
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
		timeSteps = new TreeSet<Double>();
		for (double timeStep = 0.0; timeStep < lastTime; timeStep += snapshotPeriod) {
			timeSteps.add(timeStep);
		}
		return timeSteps;
	}

	@Override
	public boolean requestNewTime(int time, TimePreference searchDirection) {
		Double foundTime;
		if (searchDirection == TimePreference.EARLIER || searchDirection == TimePreference.RESTART) {
			foundTime = timeSteps.floor((double) time);
		} else if (searchDirection == TimePreference.LATER) {
			foundTime = timeSteps.ceiling((double) time);
		} else {
			throw new IllegalArgumentException();
		}
		if (foundTime == null) {
			return false;
		} else {
			this.nextTime = foundTime;
			this.nextTimeStep = getTimeStep(time);
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
		OTFVisConfigGroup otfVisConfigGroup = new OTFVisConfigGroup();
		otfVisConfigGroup.setCachingAllowed(false);
		return otfVisConfigGroup;
	}

	public void close() {
		
	}

}

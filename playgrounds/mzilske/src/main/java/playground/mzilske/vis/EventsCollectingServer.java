package playground.mzilske.vis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.events.algorithms.SnapshotGenerator;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFServerQuadI;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.SnapshotWriter;

import com.sleepycat.je.DatabaseException;


public final class EventsCollectingServer implements OTFServerRemote {
	
	private static Logger logger = Logger.getLogger(EventsCollectingServer.class);
	
	public static class TimeStep implements Serializable {

		private static final long serialVersionUID = 1L;

		public Collection<AgentSnapshotInfo> agentPositions = new ArrayList<AgentSnapshotInfo>();
		
	}

	private Network network;
	
	private final OTFAgentsListHandler.Writer agentWriter = new OTFAgentsListHandler.Writer();
	
	private MyQuadTree quadTree;
	
	private final ByteBuffer byteBuffer = ByteBuffer.allocate(80000000);
	
	private double nextTime = -1;
	
	private SortedMap<Double, TimeStep> timeSteps;

	private TimeStep nextTimeStep;

	private MovieDatabase db;

	private File tempFile;

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
				LinkHandler.Writer linkWriter = new LinkHandler.Writer();
				linkWriter.setSrc(link);
				this.put(middleEast, middleNorth, linkWriter);
			}
			this.addAdditionalElement(agentWriter);
		}
		
	}
	
	private class SnapshotReceiver implements SnapshotWriter {

		double time;
		TimeStep currentTimeStep;
		
		@Override
		public void addAgent(AgentSnapshotInfo position) {
			if (position.getAgentState() == AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY) return;
			currentTimeStep.agentPositions.add(position);
		}

		@Override
		public void beginSnapshot(double time) {
			this.time = time;
			this.currentTimeStep = new TimeStep();

		}

		@Override
		public void endSnapshot() {
			timeSteps.put(time, currentTimeStep);
		}

		@Override
		public void finish() {
			
		}
		
	}
	
	public EventsCollectingServer(Network network, EventsManager eventsManager, double snapshotPeriod, SimulationConfigGroup simulationConfigGroup) {
		this.network = network;
		SnapshotGenerator snapshotGenerator = new SnapshotGenerator(network, snapshotPeriod, simulationConfigGroup);
		SnapshotReceiver snapshotReceiver = new SnapshotReceiver();
		snapshotGenerator.addSnapshotWriter(snapshotReceiver);
		eventsManager.addHandler(snapshotGenerator);
		quadTree = new MyQuadTree();
		quadTree.initQuadTree();
		try {
			tempFile = createTempDirectory();
			db = new MovieDatabase(tempFile);
			MovieView view = new MovieView(db);
			timeSteps = view.getTimeStepMap();
		} catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public int getLocalTime() throws RemoteException {
		if (nextTimeStep == null) {
			nextTime = timeSteps.firstKey();
			nextTimeStep = timeSteps.get(nextTime);
		}
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
		return this.timeSteps.keySet();
	}

	@Override
	public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
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
			this.nextTimeStep = timeSteps.get(foundTime);
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

	public static File createTempDirectory() {
		final File temp;

		try {
			temp = File.createTempFile("otfvis", Long.toString(System.nanoTime()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (!(temp.delete())) {
			throw new RuntimeException("Could not delete temp file: "
					+ temp.getAbsolutePath());
		}

		if (!(temp.mkdir())) {
			throw new RuntimeException("Could not create temp directory: "
					+ temp.getAbsolutePath());
		}

		return (temp);
	}
	
	static public boolean deleteDirectory(File path) {
	    if( path.exists() ) {
	      File[] files = path.listFiles();
	      for(int i=0; i<files.length; i++) {
	         if(files[i].isDirectory()) {
	           deleteDirectory(files[i]);
	         }
	         else {
	           files[i].delete();
	         }
	      }
	    }
	    return( path.delete() );
	  }

	public void close() {
		db.close();
		deleteDirectory(tempFile);
	}

}

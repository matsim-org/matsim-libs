package playground.mzilske.neo;

import java.rmi.RemoteException;
import java.util.Collection;

import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadI;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.interfaces.OTFLiveServerRemote;
import org.matsim.vis.otfvis.interfaces.OTFQueryRemote;
import org.matsim.vis.otfvis.opengl.queries.AbstractQuery;
import org.neo4j.graphdb.Transaction;

public class NeoOTFLiveServerTransactionWrapper implements OTFLiveServerRemote {
	
	private final OTFLiveServerRemote delegate;
	
	private final NeoScenario scenario;

	public NeoOTFLiveServerTransactionWrapper(OTFLiveServerRemote delegate,
			NeoScenario scenario) {
		super();
		this.delegate = delegate;
		this.scenario = scenario;
	}

	public OTFQueryRemote answerQuery(AbstractQuery query)
			throws RemoteException {
		Transaction tx = scenario.beginTx();
		try {
			OTFQueryRemote queryRemote = delegate.answerQuery(query);
			tx.success();
			return queryRemote;
		} finally {
			tx.finish();
		}
	}

	public int getLocalTime() throws RemoteException {
		return delegate.getLocalTime();
	}

	public OTFVisConfigGroup getOTFVisConfig() throws RemoteException {
		return delegate.getOTFVisConfig();
	}

	public OTFServerQuadI getQuad(String id, OTFConnectionManager connect)
			throws RemoteException {
		return delegate.getQuad(id, connect);
	}

	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		return delegate.getQuadConstStateBuffer(id);
	}

	public byte[] getQuadDynStateBuffer(String id, Rect bounds)
			throws RemoteException {
		return delegate.getQuadDynStateBuffer(id, bounds);
	}

	public Collection<Double> getTimeSteps() throws RemoteException {
		return delegate.getTimeSteps();
	}

	public boolean isLive() throws RemoteException {
		return delegate.isLive();
	}

	public void pause() throws RemoteException {
		delegate.pause();
	}

	public void play() throws RemoteException {
		delegate.play();
	}

	public void removeQueries() throws RemoteException {
		delegate.removeQueries();
	}

	public boolean requestNewTime(int time, TimePreference searchDirection)
			throws RemoteException {
		return delegate.requestNewTime(time, searchDirection);
	}

	public void toggleShowParking() throws RemoteException {
		delegate.toggleShowParking();
	}

	
	
	
}

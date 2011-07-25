package playground.mzilske.neo;

import java.util.Collection;

import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
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

	public OTFQueryRemote answerQuery(AbstractQuery query) {
		Transaction tx = scenario.beginTx();
		try {
			OTFQueryRemote queryRemote = delegate.answerQuery(query);
			tx.success();
			return queryRemote;
		} finally {
			tx.finish();
		}
	}

	public int getLocalTime() {
		return delegate.getLocalTime();
	}

	public OTFVisConfigGroup getOTFVisConfig() {
		return delegate.getOTFVisConfig();
	}

	public OTFServerQuadTree getQuad(OTFConnectionManager connect) {
		return delegate.getQuad(connect);
	}

	public byte[] getQuadConstStateBuffer() {
		return delegate.getQuadConstStateBuffer();
	}

	public byte[] getQuadDynStateBuffer(Rect bounds) {
		return delegate.getQuadDynStateBuffer(bounds);
	}

	public Collection<Double> getTimeSteps() {
		return delegate.getTimeSteps();
	}

	public boolean isLive() {
		return delegate.isLive();
	}

	public void pause() {
		delegate.pause();
	}

	public void play() {
		delegate.play();
	}

	public void removeQueries() {
		delegate.removeQueries();
	}

	public boolean requestNewTime(int time, TimePreference searchDirection) {
		return delegate.requestNewTime(time, searchDirection);
	}

	public void toggleShowParking() {
		delegate.toggleShowParking();
	}

	
	
	
}

package lsp;

import lsp.controler.LSPSimulationTracker;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class LSPDataObject<T> implements HasSimulationTrackers<T> {

	private final Collection<LSPSimulationTracker<T>> trackers = new ArrayList<>();
	private final Attributes attributes = new Attributes();

	@Override public final void addSimulationTracker( LSPSimulationTracker<T> tracker ){
		this.trackers.add( tracker );
		tracker.setEmbeddingContainer( (T) this );
	}

	@Override public final Collection<LSPSimulationTracker<T>> getSimulationTrackers(){
		return Collections.unmodifiableCollection( this.trackers );
	}

	@Override public final void clearSimulationTrackers() {
		trackers.clear();
	}

	public final Attributes getAttributes() {
		return attributes;
	}
}

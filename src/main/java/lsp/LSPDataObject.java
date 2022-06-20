package lsp;

import lsp.controler.LSPSimulationTracker;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class LSPDataObject<T> implements HasSimulationTrackers<T>, Attributable, Identifiable<T> {

	private final Collection<LSPSimulationTracker<T>> trackers = new ArrayList<>();
	private final Attributes attributes = new Attributes();
	private final Id<T> id;

	public LSPDataObject( Id<T> id ) {
		this.id = id;
	}

	@Override public final void addSimulationTracker( LSPSimulationTracker<T> tracker ){
		this.trackers.add( tracker );
		tracker.setEmbeddingContainer( (T) this );
		// It may not be possible to do this without this cast.  Since "this" only knows that it is at least an LSPDataObject, and only we
		// know that it is truly of type T.  kai, jun'22
	}

	@Override public final Collection<LSPSimulationTracker<T>> getSimulationTrackers(){
		return Collections.unmodifiableCollection( this.trackers );
	}

	@Override public final void clearSimulationTrackers() {
		trackers.clear();
	}

	@Override  public final Attributes getAttributes() {
		return attributes;
	}

	@Override  public final Id<T> getId() {
		return id;
	}
}

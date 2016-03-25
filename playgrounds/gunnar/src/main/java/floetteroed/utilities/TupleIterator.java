package floetteroed.utilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Iterates over all pair-wise combinations of the elements of a given
 * collection. This collection must not change during the iteration.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TupleIterator<E> implements Iterator<Tuple<E, E>> {

	// -------------------- MEMBERS --------------------

	private final Collection<E> elements;

	private Iterator<E> firstElementIterator = null;

	private Iterator<E> secondElementIterator = null;

	private E firstElement = null;

	// -------------------- CONSTRUCTION --------------------

	public TupleIterator(final Collection<E> elements) {
		this.elements = elements;
		this.firstElementIterator = elements.iterator();
		this.secondElementIterator = elements.iterator();
		if (this.firstElementIterator.hasNext()) {
			this.firstElement = this.firstElementIterator.next();
		}
	}

	// -------------------- IMPLEMENTATION OF Iterator --------------------

	@Override
	public boolean hasNext() {
		// note that this returns false if this.elements is empty
		return (this.firstElementIterator.hasNext() || this.secondElementIterator
				.hasNext());
	}

	@Override
	public Tuple<E, E> next() {
		if (!this.secondElementIterator.hasNext()) {
			this.firstElement = this.firstElementIterator.next();
			this.secondElementIterator = this.elements.iterator();
		}
		return new Tuple<E, E>(this.firstElement,
				this.secondElementIterator.next());
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		final List<String> elements = Arrays.asList("a", "b", "c");
		for (TupleIterator<String> it = new TupleIterator<>(elements); it
				.hasNext();) {
			System.out.println(it.next());
		}
	}
}

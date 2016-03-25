package floetteroed.utilities;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Iterates over all pair-wise combinations of the elements of a given
 * collection, apart from a predefined set of excluded tuples. The collection
 * must not change during the iteration.
 * 
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TupleIteratorWithExclusions<E> implements Iterator<Tuple<E, E>> {

	// -------------------- MEMBERS --------------------

	private final TupleIterator<E> fullIterator;

	private final Set<Tuple<E, E>> exceptions;

	private Tuple<E, E> next = null;

	// -------------------- CONSTRUCTION --------------------

	public TupleIteratorWithExclusions(final Collection<E> elements,
			Collection<Tuple<E, E>> exceptions) {
		this.fullIterator = new TupleIterator<>(elements);
		this.exceptions = new LinkedHashSet<>(exceptions);
		this.advance();
	}

	// -------------------- INTERNALS --------------------

	private void advance() {
		this.next = null;
		while ((this.next == null) && this.fullIterator.hasNext()) {
			final Tuple<E, E> candidate = this.fullIterator.next();
			if (!this.exceptions.contains(candidate)) {
				this.next = candidate;
			}
		}
	}

	// -------------------- IMPLEMENTATION OF Iterator --------------------

	@Override
	public boolean hasNext() {
		return (this.next != null);
	}

	@Override
	public Tuple<E, E> next() {
		final Tuple<E, E> result = this.next;
		this.advance();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		final List<String> elements = Arrays.asList("a", "b", "c");
		final List<Tuple<String, String>> exclusions = Arrays.asList(
				new Tuple<String, String>("a", "c"), new Tuple<String, String>(
						"b", "c"));

		for (TupleIteratorWithExclusions<String> it = new TupleIteratorWithExclusions<>(
				elements, exclusions); it.hasNext();) {
			System.out.println(it.next());
		}
	}

}

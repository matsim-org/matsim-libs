package gunnar.ihop2.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FractionalIterator<E> implements Iterator<E> {

	private final Iterator<E> iterator;

	private final double fraction;

	private double cumulative = 0.0;

	private E next = null;

	public FractionalIterator(final Iterator<E> iterator, final double fraction) {
		this.iterator = iterator;
		this.fraction = Math.max(0.0, Math.min(1.0, fraction));
		this.advance();
	}

	private void advance() {
		this.next = null;
		while (this.iterator.hasNext() && this.cumulative < 1.0) {
			this.next = this.iterator.next();
			this.cumulative += this.fraction;
			// if (this.cumulative < 1.0) {
			// System.out.print(".");
			// } else {
			// System.out.print("X");
			// }
		}
		if (this.cumulative >= 1.0) {
			this.cumulative -= 1.0;
		} else {
			this.next = null;
		}
	}

	@Override
	public boolean hasNext() {
		return (this.next != null);
	}

	@Override
	public E next() {
		final E result = this.next;
		this.advance();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public static void main(String[] args) {
		final List<Integer> all = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			all.add(i);
		}
		for (double f = 0; f <= 1.0; f += 0.1) {
			double cnt = 0;
			for (Iterator<Integer> it = new FractionalIterator<>(
					all.iterator(), f); it.hasNext();) {
				it.next();
				cnt++;
			}
			System.out.println(" should be: " + f + "; is "
					+ (cnt / all.size()));
		}
	}
}

package gunnar.ihop2.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FractionalIterable<T> implements Iterable<T> {

	private final Iterable<T> iterable;

	private final double fraction;

	public FractionalIterable(final Iterable<T> iterable, final double fraction) {
		this.iterable = iterable;
		this.fraction = fraction;
	}

	@Override
	public Iterator<T> iterator() {
		return new FractionalIterator<>(this.iterable.iterator(), fraction);
	}

	public static void main(String[] args) {
		final List<Integer> all = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			all.add(i);
		}
		for (double f = 0; f <= 1.0; f += 0.1) {
			double cnt = 0;
			for (Integer element : new FractionalIterable<>(all, f)) {
				cnt++;
			}
			System.out
					.println(" should be: " + f + "; is " + (cnt / all.size()));
		}
	}
}

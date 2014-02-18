package playground.mzilske.util;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

public class PowerList {

	public static <A> Collection<List<A>> powerList(final List<A> list) {
		if (list.isEmpty()) {
			return Arrays.asList(list);
		} else {
			final A elem = list.get(0);
			List<A> sublist = list.subList(1, list.size());
			final Collection<List<A>> powersublist = powerList(sublist);
			return new AbstractCollection<List<A>>() {

				@Override
				public Iterator<List<A>> iterator() {
					return Iterables.concat(Collections2.transform(powersublist, new Function<List<A>, List<A>>() {


						@Override
						public List<A> apply(List<A> arg0) {
							List<A> result = new ArrayList<A>(arg0.size()+1);
							result.add(elem);
							result.addAll(arg0);
							return result;
						}
						
					}), powersublist).iterator();
				}

				@Override
				public int size() {
					return (int) Math.pow(2, list.size());
				}

			};
		}
	}
	
	public static void main(String[] args) {
		for (List<String> strings : powerList(Arrays.asList("A", "B", "C", "D", "E", "F", "G"))) {
			System.out.println(strings);
		}
	}


}

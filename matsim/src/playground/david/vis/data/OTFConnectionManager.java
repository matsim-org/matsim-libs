package playground.david.vis.data;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


public class OTFConnectionManager {

	class Entry {
		Class from, to;

		public Entry(Class from, Class to) {
			this.from = from;
			this.to = to;
		}

	}
	
	private final List<Entry> connections = new LinkedList<Entry>();
	
	public void add(Entry entry) {
		connections.add(entry);
	}
	
	public void add (Class from, Class to) {
		add(new Entry(from,to));
	}

	public Collection<Class> getEntries(Class srcClass) {
		List classList = new LinkedList<Class>();
		for(Entry entry : connections) {
			if (entry.from.equals(srcClass)) classList.add(entry.to);
		}
		return classList;
	}
}

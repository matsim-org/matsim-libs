package playground.david.vis.data;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import playground.david.vis.interfaces.OTFDataReader;


public class OTFConnectionManager {

	class Entry {
		Class from, to;

		public Entry(Class from, Class to) {
			this.from = from;
			this.to = to;
		}
		
		@Override
		public String toString() {
			return "(" + from.toString() + "," + to.toString() + ") ";
		}

	}
	
	private final List<Entry> connections = new LinkedList<Entry>();
	
	public void add(Entry entry) {
		connections.add(entry);
	}
	
	public void add (Class from, Class to) {
		add(new Entry(from,to));
	}

	public void remove(Class from, Class to) {
		Iterator<Entry> iter = connections.iterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			if (entry.from == from && entry.to == to) iter.remove();
		}
	}

	public void remove (Class from) {
		Iterator<Entry> iter = connections.iterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			if (entry.from == from) iter.remove();
		}
	}

	public Collection<Class> getEntries(Class srcClass) {
		List classList = new LinkedList<Class>();
		for(Entry entry : connections) {
			if (entry.from.equals(srcClass)) classList.add(entry.to);
		}
		return classList;
	}

	private Class handleClassAdoption(Class entryClass, String fileFormat) {
		Class newReader = null;
		
		if (OTFDataReader.class.isAssignableFrom(entryClass)) {
			// entry.to is a OTFDataReader class, check for special version for this fileversion
			String ident = entryClass.getCanonicalName() + fileFormat;
			if (OTFDataReader.previousVersions.containsKey(ident)) {
				// there is a special version, replace the entry to with it!
				newReader = OTFDataReader.previousVersions.get(ident);
			}
		}
		return newReader;
	}
	
	public void adoptFileFormat(String fileFormat) {
		// go through every reader class and look for the appropriate Reader Version for this fileformat
		
		ListIterator<Entry> iter = connections.listIterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			// make sure, that the static members have been initialized
			try {
				Object o = entry.to.newInstance();
				Object p = entry.from.newInstance();
			} catch (Exception e) {
				
			}
			
			// check for both classes, if they need to be replaced
			Class newReader = handleClassAdoption(entry.to, fileFormat);
			if (newReader != null) iter.set(new Entry(entry.from, newReader));

			newReader = handleClassAdoption(entry.from, fileFormat);
			if (newReader != null) iter.set(new Entry(newReader, entry.to));
		}
		System.out.println(OTFDataReader.previousVersions);
		System.out.println(connections);

	}
}

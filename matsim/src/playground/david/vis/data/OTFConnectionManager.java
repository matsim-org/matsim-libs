package playground.david.vis.data;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import playground.david.vis.caching.SceneGraph;
import playground.david.vis.caching.SceneLayer;
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
	
	public OTFConnectionManager clone() {
		OTFConnectionManager clone = new OTFConnectionManager();
		Iterator<Entry> iter = connections.iterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			clone.add(entry.from, entry.to);
		}
		return clone;
	}

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
		List<Class> classList = new LinkedList<Class>();
		for(Entry entry : connections) {
			if (entry.from.equals(srcClass)) classList.add(entry.to);
		}
		return classList;
	}

	public Collection<OTFData.Receiver> getReceivers(Class srcClass, SceneGraph graph) {
		Collection<Class> classList = getEntries(srcClass);
		List<OTFData.Receiver> receiverList = new LinkedList<OTFData.Receiver>();
		
		for(Class entry : classList) {
			try {
				receiverList.add((OTFData.Receiver)(graph.newInstance(entry)));
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return receiverList;
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

	public void fillLayerMap(Map<Class, SceneLayer> layers) {
		Iterator<Entry> iter = connections.iterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			if (SceneLayer.class.isAssignableFrom(entry.to))
				try {
					layers.put(entry.from, (SceneLayer)(entry.to.newInstance()));
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
}

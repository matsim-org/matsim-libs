package playground.mzilske.osm;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.filter.v0_6.TagFilter;
import org.openstreetmap.osmosis.core.filter.v0_6.UsedNodeFilter;
import org.openstreetmap.osmosis.core.merge.common.ConflictResolutionMethod;
import org.openstreetmap.osmosis.core.merge.v0_6.EntityMerger;
import org.openstreetmap.osmosis.core.progress.v0_6.EntityProgressLogger;
import org.openstreetmap.osmosis.core.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.core.xml.v0_6.FastXmlReader;
import org.openstreetmap.osmosis.core.xml.v0_6.XmlWriter;

public class OsmPrepare {
	
	private final String infFile;
	private final String outFile;
	
	private final String[] streetFilter;
	private final String[] transitFilter;
	
	public OsmPrepare(String infFile, String outFile, String[] streetFilter, String[] transitFilter){
		this.infFile = infFile;
		this.outFile = outFile;
		this.streetFilter = streetFilter;
		this.transitFilter = transitFilter;
	}

	public static void main(String[] args) {
		new OsmPrepare("e:/_out/osm/berlinbrandenburg.osm", "e:/_out/osm/berlinbrandenburg_filtered_small.osm", new String[]{"motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary"}, new String[]{"tram", "train", "bus"}).prepareOsm();		
	}
	
	public void prepareOsm(){
		String filename = this.infFile;
		String targetFilename = this.outFile;
		
		JOSMTolerantFastXMLReader reader = new JOSMTolerantFastXMLReader(new File(filename), true, CompressionMethod.None);		
		UsedNodeFilter usedNodeFilter = new UsedNodeFilter(IdTrackerType.BitSet);
		EntityProgressLogger logger= new EntityProgressLogger(10);
		
		JOSMTolerantFastXMLReader reader2 = new JOSMTolerantFastXMLReader(new File(filename), true, CompressionMethod.None);		
		EntityProgressLogger logger2= new EntityProgressLogger(10);
				
		TagFilter streetTagFilter = createStreetFilter(this.streetFilter);		
		TagFilter transitTagFilter = createTransitFilter(this.transitFilter);
		UsedNodeAndWayFilter usedFilter = new UsedNodeAndWayFilter(IdTrackerType.BitSet);
		
		EntityMerger entityMerger = new EntityMerger(ConflictResolutionMethod.LatestSource, 20);
		
		XmlWriter writer = new XmlWriter(new File(targetFilename), CompressionMethod.None);
		
		reader.setSink(logger);
		logger.setSink(streetTagFilter);
		streetTagFilter.setSink(usedNodeFilter);
		reader2.setSink(logger2);
		logger2.setSink(transitTagFilter);
		transitTagFilter.setSink(usedFilter);
		usedNodeFilter.setSink(entityMerger.getSink(0));
		usedFilter.setSink(entityMerger.getSink(1));
		entityMerger.setSink(writer);
		
		Thread t1 = new Thread(reader);
		t1.start();
		Thread t2 = new Thread(reader2);
		t2.start();
		Thread t3 = new Thread(entityMerger);
		t3.start();
		
		try {
			t1.join();
			t2.join();
			t3.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
	}

	private static TagFilter createStreetFilter(String[] filter) {
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		tagKeyValues.put("highway", new HashSet<String>(Arrays.asList(filter)));
		Set<String> tagKeys = Collections.emptySet();
		TagFilter tagFilter = new TagFilter("accept-way", tagKeys, tagKeyValues);
		return tagFilter;
	}

	private static TagFilter createTransitFilter(String[] filter) {
		Map<String, Set<String>> tagKeyValues = new HashMap<String, Set<String>>();
		tagKeyValues.put("route", new HashSet<String>(Arrays.asList(filter))); 
		Set<String> tagKeys = Collections.emptySet();
		TagFilter transitFilter = new TagFilter("accept-relation", tagKeys, tagKeyValues);
		return transitFilter;
	}
	
}

package playground.andreas.osmBB;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.xml.sax.SAXException;

import playground.andreas.osmBB.osm2counts.Osm2Counts;
//import playground.droeder.osm.ResizeLinksByCount2;
//import playground.droeder.osm.ResizeLinksByCount3;
//import playground.droeder.osm.ResizeLinksByCount4;
//import playground.droeder.osm.ResizeLinksByCount2;
//import playground.droeder.osm.ResizeLinksByCount3;
import playground.mzilske.osm.OsmPrepare;
import playground.mzilske.osm.OsmTransitMain;

public class PTCountsOsm2Matsim {
	

	public static void main(String[] args) {
		String osmRepository = "e:/_shared-svn/osm_berlinbrandenburg/workingset/";
		String osmFile = "berlinbrandenburg_filtered.osm";
		String countsFile = "f:/bln_counts/Di-Do_counts.xml";
		String countsOutFile = "f:/bln_counts/Di-Do_counts_out.xml";
		
		String outDir = "f:/bln_counts/";
		String outName = "counts";
		
		String filteredOsmFile = outDir + outName + ".osm";
		
//		String[] streetFilter = new String[]{"motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary","tertiary","minor","unclassified","residential","living_street"};
		String[] streetFilter = new String[]{"motorway","motorway_link","trunk","trunk_link","primary","primary_link","secondary","tertiary"};
		
//		String[] transitFilter = new String[]{"ferry", "subway", "light_rail", "tram", "train", "bus", "trolleybus"};
		String[] transitFilter = new String[]{"fsfsfgsg"};
		
		OsmPrepare osmPrepare = new OsmPrepare(osmRepository + osmFile, filteredOsmFile, streetFilter, transitFilter);
		osmPrepare.prepareOsm();
		
		Osm2Counts osm2Counts = new Osm2Counts(filteredOsmFile);
		osm2Counts.prepareOsm();
		HashMap<String, String> shortNameMap = osm2Counts.getShortNameMap();
		Counts counts = new Counts();
		CountsReaderMatsimV1 countsReader = new CountsReaderMatsimV1(counts);
		try {
			countsReader.parse(countsFile);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		OsmTransitMain osmTransitMain = new OsmTransitMain(filteredOsmFile, TransformationFactory.WGS84, TransformationFactory.DHDN_GK4, outDir + outName + "_network.xml", outDir + outName + "_schedule.xml");
		osmTransitMain.convertOsm2Matsim(transitFilter);
		
//		ResizeLinksByCount2 r = new ResizeLinksByCount2(outDir + outName + "_network.xml", counts, shortNameMap, new Double(1.1));
//		r.run(outDir + outName + "_network_resized.xml");
		
//		ResizeLinksByCount3 r = new ResizeLinksByCount3(outDir + outName + "_network.xml", counts, shortNameMap, 1.0);
//		r.run(outDir + outName + "_network_resized.xml");
		
//		ResizeLinksByCount4 r = new ResizeLinksByCount4(outDir + outName + "_network.xml", counts, shortNameMap, 1.0);
//		r.run(outDir + outName + "_network_resized.xml");
		
		PTCountsNetworkSimplifier ptCountNetSimplifier = new PTCountsNetworkSimplifier(outDir + outName + "_network_resized.xml", outDir + outName + "_schedule.xml", outDir + outName + "_network_merged.xml", outDir + outName + "_schedule_merged.xml", shortNameMap, counts, countsOutFile);
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));
		ptCountNetSimplifier.setNodesToMerge(nodeTypesToMerge);
		ptCountNetSimplifier.setMergeLinkStats(false);
		ptCountNetSimplifier.simplifyPTNetwork();
		

		
	}

}

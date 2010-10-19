package playground.droeder.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.Volume;
import org.xml.sax.SAXException;

import playground.andreas.osmBB.osm2counts.Osm2Counts;

public class ResizeLinksByCount3 extends AbstractResizeLinksByCount{
	
	private static final Logger log = Logger.getLogger(ResizeLinksByCount3.class);
	private boolean countsMatched = false;
	private Counts newCounts;
	
	public static void main(String[] args){
		String countsFile = "d:/VSP/output/osm_bb/Di-Do_counts.xml";
		String filteredOsmFile = "d:/VSP/output/osm_bb/counts.osm";
		String networkFile = "d:/VSP/output/osm_bb/counts_network.xml";
		
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
		
		ResizeLinksByCount3 r = new ResizeLinksByCount3(networkFile, counts, shortNameMap);
		r.run("d:/VSP/output/osm_bb/network_resized");
	}


	
	/**
	 * use this contructor if the counts loc_Ids are NOT matched to the linkIds. The shortNameMap 
	 * consists of  toNodeIds mapped to counts cs_Ids!  
	 * @param networkFile
	 * @param counts
	 * @param shortNameMap
	 */
	public ResizeLinksByCount3(String networkFile, Counts counts, Map<String, String> shortNameMap){
		super(networkFile, counts, shortNameMap);
	}
	
	/**
	 * use this constructor if counts loc_Ids and linkIds are matched!
	 * @param networkFile
	 * @param counts
	 */
	public ResizeLinksByCount3(String networkFile, Counts counts){
		super(networkFile, counts, null);
		this.countsMatched = true;
	}
		
	public void run(String outFile){
		if(!countsMatched){
			this.preProcessCounts();
			this.writePreprocessedCounts(outFile);
		}else{
			this.newCounts = super.oldCounts;
		}
		super.run(outFile + ".xml");
		
	}
	
	private void preProcessCounts() {
		Node node;
		Count oldCount;
		Count newCount;
		Id outLink = null;
		this.newCounts = new Counts();
		this.newCounts.setDescription("none");
		this.newCounts.setName("counts merged");
		this.newCounts.setYear(2009);
		
		for(Entry<String, String> e : this.shortNameMap.entrySet()){
			if(this.net.getNodes().containsKey(new IdImpl(e.getKey())) && this.oldCounts.getCounts().containsKey(new IdImpl(e.getValue()))){
				node =  this.net.getNodes().get(new IdImpl(e.getKey()));
				oldCount = this.oldCounts.getCounts().get(new IdImpl(e.getValue()));
				
				//nodes with countingStations on it contain only one outlink
				for(Link l : node.getOutLinks().values()){
					outLink = l.getId();
					break;
				}
				if(!(outLink == null)){
					newCount = this.newCounts.createCount(outLink, oldCount.getCsId());
					newCount.setCoord(oldCount.getCoord());
					for(Entry<Integer, Volume> ee : oldCount.getVolumes().entrySet()){
						newCount.createVolume(ee.getKey(), ee.getValue().getValue());
					}
				}
			}
		}
	}

	protected void resize() {
		Double maxCount;
		String origId;
		Integer nrOfNewLanes;
		LinkImpl countLink;
		Double capPerLane;
		
		for(Count c : this.newCounts.getCounts().values()){
			countLink = (LinkImpl) this.net.getLinks().get(c.getLocId());
			origId = countLink.getOrigId();
			maxCount = c.getMaxVolume().getValue();
			
			for(Link l : this.net.getLinks().values()){
				if(((LinkImpl)l).getOrigId().equals(origId)){
					capPerLane = l.getCapacity() / l.getNumberOfLanes();
					// if maxCount < cap set cap to maxCount and keep nrOfLanes
					if(maxCount < l.getCapacity()){
						nrOfNewLanes = (int) l.getNumberOfLanes();
					}
					// else set nrOfNewLanes to int(maxCount/capPerLane) and cap to maxCount
					else{
						nrOfNewLanes = (int) (maxCount/capPerLane);
					}
					System.out.print(l.getId() + " " + l.getCapacity() + " " + l.getNumberOfLanes() + " " + maxCount + " " + nrOfNewLanes);
					System.out.println();
					l.setCapacity(maxCount);
					l.setNumberOfLanes(nrOfNewLanes);
				}
			}
		}
		log.info("resizing finished!!!");
	}
	
	
	private void writePreprocessedCounts(String outFile) {
		log.info("writing counts to " + outFile + "_counts.xml...");
		new CountsWriter(newCounts).write(outFile + "_counts.xml");
		log.info("done...");
	}

	
}

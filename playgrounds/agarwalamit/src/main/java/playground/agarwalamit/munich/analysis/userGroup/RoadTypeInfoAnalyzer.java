package playground.agarwalamit.munich.analysis.userGroup;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.agarwalamit.utils.LoadMyScenarios;

public class RoadTypeInfoAnalyzer {
	
	
	public static void main(String[] args) {
		String networkFile = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";
		String runDir ="../../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run10/policies/backcasting/";
		String [] runCases = {"bau","ei","5ei","10ei","15ei","20ei","25ei"};
		String outFile = runDir+"/analysis/";
		
		Network network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile+"/roadTypeInfo_freight.txt");
		
		Map<String,Map<String,Integer>> runCase2RoadType2Count = new HashMap<>();
		Set<String> roadTypes = new HashSet<>();
		
		for(String runCase:runCases){
			RoadTypeInfoAnalyzer rti = new RoadTypeInfoAnalyzer(network, runDir+runCase+"/ITERS/it.1500/1500.events.xml.gz");
			rti.run();
			Map<String, Integer> roadType2OccuranceCount = rti.getRoadType2OccuranceCount(MunichUserGroup.Freight);
			runCase2RoadType2Count.put(runCase, roadType2OccuranceCount);
			roadTypes.addAll(roadType2OccuranceCount.keySet());
		}
		
		
		try {
			writer.write("roadType \t");
			for (String rc : runCases){
				writer.write(rc+"\t");
			}
			writer.newLine();
			for(String rt:roadTypes){
				writer.write(rt+"\t");
				for (String rc : runCases){
					writer.write(runCase2RoadType2Count.get(rc).get(rt)+"\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Rason : "+e);
		}
	}
	
	
	public RoadTypeInfoAnalyzer (Network network, String eventsFile){
		this.rth = new RoadTypeHandler(network);
		this.eventsFile = eventsFile;
	}

	private final RoadTypeHandler rth;
	private final String eventsFile;
	
	public void run(){
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(rth);
		reader.readFile(eventsFile);
	}
	
	public Map<MunichUserGroup, Map<String, Integer>> getUserGroupToRoadType2OccuranceCount(){
		return rth.userGrp2roadType2Count;
	}

	public Map<String, Integer> getRoadType2OccuranceCount (MunichUserGroup ug){
		return rth.userGrp2roadType2Count.get(ug);
	}
	
	private class RoadTypeHandler implements LinkLeaveEventHandler {

		RoadTypeHandler(Network network) {
			this.net = network;

			for(MunichUserGroup ug : MunichUserGroup.values()){
				this.userGrp2roadType2Count.put(ug, new HashMap<>());
			}
		}

		private final Network net;
		private final MunichPersonFilter pf = new MunichPersonFilter();

		private final Map<MunichUserGroup,Map<String,Integer>> userGrp2roadType2Count = new HashMap<>();

		@Override
		public void reset(int iteration) {
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Id<Link> linkId = event.getLinkId();
			Id<Person> personId = Id.createPersonId(event.getVehicleId());
			MunichUserGroup ug = pf.getMunichUserGroupFromPersonId(personId);
			String roadType = NetworkUtils.getType((net.getLinks().get(linkId)));

			Map<String, Integer> road2count = userGrp2roadType2Count.get(ug);
			if(road2count.containsKey(roadType)){
				road2count.put(roadType, road2count.get(roadType)+1);
			} else road2count.put(roadType, 1);
		}
	}
}

package playground.agarwalamit.munich.analysis.userGroup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.agarwalamit.utils.AreaFilter;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class RoadTypeInfoAnalyzer {
	
	public static void main(String[] args) {
		String networkFile = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/hEART/input/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";
		String shapeFileCity = FileUtils.SHARED_SVN+"/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";

		String runDir =FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/hEART/output/";
		String [] runCases = {"bau","ei","5ei","10ei","15ei","20ei","25ei"};
		String outFolder = runDir+"/analysis/";
		
		Network network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();
		BufferedWriter writer = IOUtils.getBufferedWriter(outFolder+"/roadTypeInfo_freight_city.txt");
		
		Map<String,Map<String,Integer>> runCase2RoadType2Count = new HashMap<>();
		Set<String> roadNr = new HashSet<>();
		
		for(String runCase:runCases){
			RoadTypeInfoAnalyzer rti = new RoadTypeInfoAnalyzer(network, runDir+runCase+"/ITERS/it.1500/1500.events.xml.gz", new AreaFilter(shapeFileCity));
			rti.run();
			Map<String, Integer> roadType2OccuranceCount = rti.getRoadType2OccuranceCount(MunichUserGroup.Freight);
			runCase2RoadType2Count.put(runCase, roadType2OccuranceCount);
			roadNr.addAll(roadType2OccuranceCount.keySet());
		}

		String roadTypeMappingFile = FileUtils.SHARED_SVN + "/projects/detailedEval/matsim-input-files/roadTypeMapping_v0.txt";
		Map<Integer, String> roadNr2roadType = getRoadNr2RoadType(roadTypeMappingFile);

		try {
			writer.write("roadNr \t roadName \t");
			for (String rc : runCases){
				writer.write(rc+"\t");
			}
			writer.newLine();
			for(String rn:roadNr){
				writer.write(rn+"\t");
				writer.write(roadNr2roadType.get(Integer.valueOf(rn))+"\t");
				for (String rc : runCases){
					int count = runCase2RoadType2Count.get(rc).containsKey(rn) ? runCase2RoadType2Count.get(rc).get(rn) : 0;
					writer.write( count +"\t");
				}
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Rason : "+e);
		}
	}


	private static Map<Integer, String> getRoadNr2RoadType(final String roadTypeMappingFile) {
		Map<Integer, String> roadNr2roadType = new HashMap<>();

		BufferedReader reader = IOUtils.getBufferedReader(roadTypeMappingFile);
		try {
			String  line = reader.readLine();
			boolean isFirstLine = true;
			while(line!=null){
				String parts [] = line.split(";");
				if(! isFirstLine ) {
					int roadNr = Integer.valueOf(parts[0]); // VISUM_RT_NR
					String roadName = parts[2]; // HBEFA_RT_NAME
					roadNr2roadType.put(roadNr, roadName);
				}
				line = reader.readLine();
				isFirstLine = false;
			}
		} catch (IOException e) {
			throw new RuntimeException("Data is not written/read. Reason : " + e);
		}
		return roadNr2roadType;
	}

	RoadTypeInfoAnalyzer (final Network network, final String eventsFile, final AreaFilter areaFilter){
		this.rth = new RoadTypeHandler(network, areaFilter);
		this.eventsFile = eventsFile;
	}

	private final RoadTypeHandler rth;
	private final String eventsFile;

	void run(){
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(rth);
		reader.readFile(eventsFile);
	}
	
	Map<MunichUserGroup, Map<String, Integer>> getUserGroupToRoadType2OccuranceCount(){
		return rth.userGrp2roadType2Count;
	}

	Map<String, Integer> getRoadType2OccuranceCount (MunichUserGroup ug){
		return rth.userGrp2roadType2Count.get(ug);
	}
	
	private class RoadTypeHandler implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

		private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

		RoadTypeHandler(final Network network, final AreaFilter areaFilter) {
			this.net = network;
			this.areaFilter = areaFilter;
			for(MunichUserGroup ug : MunichUserGroup.values()){
				this.userGrp2roadType2Count.put(ug, new HashMap<>());
			}
		}

		private final Network net;
		private final MunichPersonFilter pf = new MunichPersonFilter();
		private final AreaFilter areaFilter;

		private final Map<MunichUserGroup,Map<String,Integer>> userGrp2roadType2Count = new HashMap<>();

		@Override
		public void reset(int iteration) {
			this.userGrp2roadType2Count.clear();
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Id<Link> linkId = event.getLinkId();
			Link l = net.getLinks().get(linkId);

			if (! this.areaFilter.isLinkInsideShape(l)) return;

			Id<Person> personId = delegate.getDriverOfVehicle(event.getVehicleId());
			MunichUserGroup ug = pf.getMunichUserGroupFromPersonId(personId);
			String roadType = NetworkUtils.getType((net.getLinks().get(linkId)));

			Map<String, Integer> road2count = userGrp2roadType2Count.get(ug);
			if(road2count.containsKey(roadType)){
				road2count.put(roadType, road2count.get(roadType)+1);
			} else road2count.put(roadType, 1);
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			delegate.handleEvent(event);
		}

		@Override
		public void handleEvent(VehicleLeavesTrafficEvent event) {
			delegate.handleEvent(event);
		}
	}
}

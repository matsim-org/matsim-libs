package playground.dhosse.gap.scenario.carsharing.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.io.IOUtils;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import playground.dhosse.gap.Global;
import playground.dhosse.utils.osm.OsmCoordUtils;

public class CsSink  implements Sink{
	
	private static final String HEADER = "Ort\tStandort\tPLZ\tStao-Nr.\tKanton\tGeoX\tGeoY\tNorth\tEast\tFahrzeuge\tParkplätze";
	
	private Set<String> keys;
	private Map<String, String> typeMap = new HashMap<String, String>();
	
	private Map<Long, NodeContainer> nodeMap;
	private Map<Long, WayContainer> wayMap;
	private Map<Long, RelationContainer> relationMap;
	
	private final Scenario scenario;
	
	Map<String, OneWayCarsharingStation> csStations = new HashMap<>();
	
	public CsSink(Scenario scenario, Map<String, String> osmToMatsimType, Set<String> keys){
		
		this.scenario = scenario;
		this.keys = keys;
		this.typeMap = osmToMatsimType;
		
		this.nodeMap = new HashMap<>();
		this.wayMap = new HashMap<>();
		this.relationMap = new HashMap<>();
		
	}

	@Override
	public void initialize(Map<String, Object> arg0) {
		
	}
	
	private void process(Map<Long, ? extends EntityContainer> nodeMap){
		
		for(Long n : nodeMap.keySet()){
			
			Entity entity = nodeMap.get(n).getEntity();
			Map<String, String> tags = new TagCollectionImpl(entity.getTags()).buildMap();
			
			for(String s : this.keys){
				
				String tourism = tags.get(s);
				String matsimType = null;
				
				if(tourism != null){
					
					matsimType = getActivityType(tourism);
				}
				
				if(matsimType != null){
					
					Coord coord = OsmCoordUtils.getCoord(entity, Global.ct, this.nodeMap, this.wayMap, this.relationMap);
					String id = Long.toString(entity.getId());
					
					Link link = NetworkUtils.getNearestLink(this.scenario.getNetwork(), coord);
					OneWayCarsharingStation station = new OneWayCarsharingStation(link, 2, null, 4);
					this.csStations.put(id, station);
					
				}
				
			}
			
		}
		
	}

	@Override
	public void complete() {
		
		process(this.nodeMap);
//		process(this.wayMap);
//		process(this.relationMap);
		
		BufferedWriter writer = IOUtils.getBufferedWriter("/home/dhosse/csStationsAtParkingSpaces.txt");
		
		try {
			
			writer.write(HEADER);
			
			for(Entry<String, OneWayCarsharingStation> cs : this.csStations.entrySet()){
				
				writer.newLine();
				writer.write(cs.getKey() + "\tFOO\t" + cs.getValue().getCoord().getX() + "\t" + cs.getValue().getCoord().getY() + "\t0\t0\t" + cs.getValue().getNumberOfVehicles() + "\t" + cs.getValue().getNumberOfAvailableParkingSpaces());
				
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
			
	}
		
	@Override
	public void release() {
		
	}

	@Override
	public void process(EntityContainer arg0) {
		
		arg0.process(new EntityProcessor() {
			
			@Override
			public void process(RelationContainer relationContainer) {
				relationMap.put(relationContainer.getEntity().getId(), relationContainer);
			}
			
			@Override
			public void process(WayContainer wayContainer) {
				wayMap.put(wayContainer.getEntity().getId(), wayContainer);
			}
			
			@Override
			public void process(NodeContainer nodeContainer) {
				nodeMap.put(nodeContainer.getEntity().getId(), nodeContainer);
			}
			
			@Override
			public void process(BoundContainer boundContainer) {
				
			}
			
		});
		
	}
	
	private String getActivityType(String s){
		String type = typeMap.get(s);
		if(type==null){
//			log.warn("There is no activity type mapping for " + s + "! Returning NULL");
		}
//		MapUtils.addToInteger(type, typeCount, 0, 1);
		return type;
	}

}

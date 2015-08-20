package playground.dhosse.utils.osm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class OsmSink implements Sink{
	
	private final Logger log = Logger.getLogger(OsmSink.class);
	private final CoordinateTransformation ct;
	private Map<Long, NodeContainer> nodeMap;
	private Map<Long, WayContainer> wayMap;
	private Map<Long, RelationContainer> relationMap;
	private ActivityFacilities facilities;
	private ObjectAttributes attributes;
	private Map<String, String> typeMap = new HashMap<String, String>();
	private Map<String, Integer> typeCount = new HashMap<>();
	private final Set<String> keys;
	
	public OsmSink(CoordinateTransformation ct, Map<String, String> osmToMatsimType, Set<String> keys){
		this.ct = ct;
		this.typeMap = osmToMatsimType;
		this.nodeMap = new HashMap<>();
		this.wayMap = new HashMap<>();
		this.relationMap = new HashMap<>();
		this.keys = keys;
		
		this.facilities = FacilitiesUtils.createActivityFacilities("Facilities");
		this.attributes = new ObjectAttributes();
	}
	
	private void processFacilities(ActivityFacilitiesFactory aff, Map<Long, ? extends EntityContainer> nodeMap){
		
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
				
				String name = tags.get("name");
				
				if(name != null){
					
					name.replace("ä", "ae");
					name.replace("ö", "oe");
					name.replace("ü", "ue");
					
				}
				Coord coord = OsmCoordUtils.getCoord(entity, this.ct, this.nodeMap, this.wayMap, this.relationMap);
				Id<ActivityFacility> id = Id.create(entity.getId(), ActivityFacility.class);
				ActivityFacility af;
				if(!this.facilities.getFacilities().containsKey(id)){
					af = aff.createActivityFacility(id, coord);
					((ActivityFacilityImpl)af).setDesc(name);
					this.facilities.addActivityFacility(af);
				} else{
					af = (ActivityFacilityImpl) this.facilities.getFacilities().get(id);
				}
				
				ActivityOption ao = aff.createActivityOption(matsimType);
				af.addActivityOption(ao);
				
			}
			}
		}
		
	}

	@Override
	public void initialize(Map<String, Object> arg0) {
		
	}

	@Override
	public void complete() {
		
		log.info("   nodes: " + this.nodeMap.size());
		log.info("    ways:" + this.wayMap.size());
		log.info("reations:" + this.relationMap.size());
		
		log.info("Creating facilities...");
		
		ActivityFacilitiesFactory aff = new ActivityFacilitiesFactoryImpl();
		
		processFacilities(aff, this.nodeMap);
		processFacilities(aff, this.wayMap);
		processFacilities(aff, this.relationMap);
		
	}
	
	public ActivityFacilities getFacilities(){
		return this.facilities;
	}
	
	public ObjectAttributes getFacilityAttributes(){
		return this.attributes;
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
			log.warn("There is no activity type mapping for " + s + "! Returning NULL");
		}
		MapUtils.addToInteger(type, typeCount, 0, 1);
		return type;
	}

}
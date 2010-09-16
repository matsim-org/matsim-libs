package playground.andreas.osmBB.osm2counts;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class NodeSink implements Sink{
	
	static final Logger log = Logger.getLogger(NodeSink.class);

	int count = 0;
	HashMap<String, String> shortNameMap = new HashMap<String, String>();
	HashMap<String, String> unitNameMap = new HashMap<String, String>();
	
	@Override
	public void process(EntityContainer entityContainer) {
		entityContainer.process(new EntityProcessor() {

			/**
			 * @param arg0  
			 */
			@Override
			public void process(BoundContainer arg0) {
				// nothing to do here
			}

			@Override
			public void process(NodeContainer container) {
				
				for (Tag tag : container.getEntity().getTags()) {
					if(tag.getKey().equalsIgnoreCase("SHORT_NAME")){
						NodeSink.this.shortNameMap.put(String.valueOf(container.getEntity().getId()), tag.getValue());
					}
					if(tag.getKey().equalsIgnoreCase("UNIT_NAME")){
						NodeSink.this.unitNameMap.put(String.valueOf(container.getEntity().getId()), tag.getValue());
					}
				}			
			
				// debug
				NodeSink.this.count++;
				if (NodeSink.this.count % 50000 == 0)
					log.info(NodeSink.this.count + " nodes processed so far");
			}

			/**
			 * @param relationContainer  
			 */
			@Override
			public void process(RelationContainer relationContainer) {
				// nothing to do here
			}

			/**
			 * @param container  
			 */
			@Override
			public void process(WayContainer container) {
				// nothing to do here
			}

		});
		
	}

	@Override
	public void complete() {
		for (String shortNode : this.shortNameMap.keySet()) {
			if(!this.unitNameMap.containsKey(shortNode)){
				log.info("Node " + shortNode + " has short name tag, but no unit name tag");
			}
		}
		
		for (String unitNode : this.unitNameMap.keySet()) {
			if(!this.shortNameMap.containsKey(unitNode)){
				log.info("Node " + unitNode + " has unit name tag, but no short name tag");
			}
		}
		
		log.info("Short name map contains " + this.shortNameMap.keySet().size() + " entries.");
		log.info("Unit name map contains " + this.unitNameMap.keySet().size() + " entries.");
	}

	@Override
	public void release() {
		// nothing to do here		
	}

	protected HashMap<String, String> getShortNameMap() {
		return this.shortNameMap;
	}

	protected HashMap<String, String> getUnitNameMap() {
		return this.unitNameMap;
	}

}

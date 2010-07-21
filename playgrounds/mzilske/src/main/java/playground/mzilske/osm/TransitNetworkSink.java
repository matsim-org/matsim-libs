package playground.mzilske.osm;

import java.util.Map;

import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.TagCollectionImpl;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class TransitNetworkSink implements Sink {

	@Override
	public void process(EntityContainer entityContainer) {
		entityContainer.process(new EntityProcessor() {

			@Override
			public void process(BoundContainer arg0) {
				
			}

			@Override
			public void process(NodeContainer arg0) {
				
			}

			@Override
			public void process(RelationContainer relationContainer) {
				Relation relation = relationContainer.getEntity();
				Map<String, String> tags = new TagCollectionImpl(relation.getTags()).buildMap();
				if ("route".equals(tags.get("type")) && "bus".equals(tags.get("route"))) {
					String ref = tags.get("ref");
					String operator = tags.get("operator");
					String name = tags.get("name");
					String network = tags.get("network");
					System.out.println(network + " // " + ref + " // " + operator + " // " + name);
					int nStops = 0; 
					int nWays = 0;
					for (RelationMember relationMember : relation.getMembers()) {
						if (relationMember.getMemberType().equals(EntityType.Node)) {
							++nStops;
						} else if (relationMember.getMemberType().equals(EntityType.Way)) {
							++nWays;
						} 
					}
					System.out.println(nStops + " stops. " + nWays + " ways. ");
				}
			}

			@Override
			public void process(WayContainer arg0) {
				
			}
			
		});
	}

	@Override
	public void complete() {
		
	}

	@Override
	public void release() {
		
	}

}

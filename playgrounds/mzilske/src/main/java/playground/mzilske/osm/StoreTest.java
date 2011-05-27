package playground.mzilske.osm;


import java.util.Date;

import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.store.IndexedObjectStore;
import org.openstreetmap.osmosis.core.store.IndexedObjectStoreReader;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;

public class StoreTest {
public static void main(String[] args) {
	
	IndexedObjectStore<NodeContainer> stopNodeStore = new IndexedObjectStore<NodeContainer>(
			new SingleClassObjectSerializationFactory(NodeContainer.class),
	"stops");
	OsmUser user = new OsmUser(3, "michael");
	long id = 100;
	int version = 3;
	Date timestamp = new Date();
	long changesetId = 5;
	CommonEntityData ce = new CommonEntityData(id, version, timestamp, user, changesetId);

	Node node = new Node(ce, 10, 10);
	stopNodeStore.add(1000, new NodeContainer(node));
	stopNodeStore.complete();
	IndexedObjectStoreReader<NodeContainer> reader = stopNodeStore.createReader();
	NodeContainer nodeContainer = reader.get(1000);
	Node readNode = nodeContainer.getEntity();
	System.out.println(readNode.getLatitude());
	
}
}

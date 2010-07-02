package playground.mzilske.neo;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.index.IndexService;

public class NeoNetworkImpl implements Network {

	private final class LinkEntryIterator implements Iterator<Entry<Id, Link>> {

		Iterator<Relationship> delegate;

		public LinkEntryIterator() {
			delegate = linkRoot.getRelationships(RelationshipTypes.NETWORK_TO_LINK).iterator();
		}

		@Override
		public boolean hasNext() {
			return delegate.hasNext();
		}

		@Override
		public Entry<Id, Link> next() {
			final org.neo4j.graphdb.Node nextNode = delegate.next().getEndNode();
			final Link nextLink = new NeoLinkImpl(nextNode);
			return new Entry<Id, Link>() {

				@Override
				public Id getKey() {
					return nextLink.getId();
				}

				@Override
				public Link getValue() {
					return nextLink;
				}

				@Override
				public Link setValue(Link value) {
					throw new RuntimeException();
				}

			};
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub

		}

	}

	private final class NodeEntryIterator implements Iterator<Entry<Id, Node>> {

		Iterator<Relationship> delegate;

		public NodeEntryIterator() {
			delegate = nodeRoot.getRelationships(RelationshipTypes.NETWORK_TO_NODE).iterator();
		}

		@Override
		public boolean hasNext() {
			return delegate.hasNext();
		}

		@Override
		public Entry<Id, Node> next() {
			final org.neo4j.graphdb.Node nextNode = delegate.next().getEndNode();
			final Node nextNetworkNode = new NeoNodeImpl(nextNode);
			return new Entry<Id, Node>() {

				@Override
				public Id getKey() {
					return nextNetworkNode.getId();
				}

				@Override
				public Node getValue() {
					return nextNetworkNode;
				}

				@Override
				public Node setValue(Node value) {
					throw new RuntimeException();
				}

			};
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub

		}

	}

	private final org.neo4j.graphdb.Node nodeRoot;
	
	private final org.neo4j.graphdb.Node linkRoot;

	private GraphDatabaseService graphDb;

	private IndexService index;

	private Map<Id, Link> links;

	private AbstractMap<Id, Node> nodes;

	public NeoNetworkImpl(GraphDatabaseService graphDb, IndexService index, org.neo4j.graphdb.Node nodeRoot, org.neo4j.graphdb.Node linkRoot) {
		this.graphDb = graphDb;
		this.index = index;
		this.nodeRoot = nodeRoot;
		this.linkRoot = linkRoot;
		this.links = new AbstractMap<Id, Link>() {

			@Override
			public Set<Entry<Id, Link>> entrySet() {
				return new AbstractSet<Entry<Id, Link>>() {

					@Override
					public Iterator<Entry<Id, Link>> iterator() {
						return new LinkEntryIterator();
					}

					@Override
					public int size() {
						//TODO: Warning!
						return 0;
					}

				};
			}

		};
		this.nodes = new AbstractMap<Id, Node>() {

			@Override
			public Set<Entry<Id, Node>> entrySet() {
				return new AbstractSet<Entry<Id, Node>>() {

					@Override
					public Iterator<Entry<Id, Node>> iterator() {
						return new NodeEntryIterator();
					}

					@Override
					public int size() {
						throw new RuntimeException();
					}
					
				};
			}
			
		};
	}

	@Override
	public void addLink(Link ll) {

	}

	@Override
	public void addNode(Node nn) {
		// TODO Auto-generated method stub

	}

	@Override
	public double getCapacityPeriod() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getEffectiveLaneWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public NetworkFactory getFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Id, ? extends Link> getLinks() {
		return links;
	}

	@Override
	public Map<Id, ? extends Node> getNodes() {
		return nodes;
	}

	@Override
	public Link removeLink(Id linkId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node removeNode(Id nodeId) {
		// TODO Auto-generated method stub
		return null;
	}

}

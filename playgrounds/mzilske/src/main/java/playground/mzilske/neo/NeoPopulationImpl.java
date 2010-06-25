package playground.mzilske.neo;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.index.IndexService;

public class NeoPopulationImpl implements Population {
 
	private final class PersonEntryIterator implements
			Iterator<Entry<Id, Person>> {
		
		Iterator<Relationship> delegate;
		
		public PersonEntryIterator() {
			delegate = underlyingNode.getRelationships(RelationshipTypes.POPULATION_TO_PERSON).iterator();
		}
		
		@Override
		public boolean hasNext() {
			return delegate.hasNext();
		}

		@Override
		public Entry<Id, Person> next() {
			final Node nextNode = delegate.next().getEndNode();
			final Person nextPerson = new NeoPersonImpl(nextNode);
			return new Entry<Id, Person>() {

				@Override
				public Id getKey() {
					return nextPerson.getId();
				}

				@Override
				public Person getValue() {
					return nextPerson;
				}

				@Override
				public Person setValue(Person value) {
					throw new RuntimeException();
				}
				
			};
		}

		@Override
		public void remove() {
			throw new RuntimeException();
		}
	}

	private final NeoPopulationFactory factory;
	
	private final Node underlyingNode;

	private final Map<Id, ? extends Person> map;

	private final IndexService index; 
	
	public NeoPopulationImpl(GraphDatabaseService graphDb, final IndexService index, Node underlyingNode) {
		this.underlyingNode = underlyingNode;
		this.factory = new NeoPopulationFactory(graphDb, index);
		this.index = index;
		this.map = new AbstractMap<Id, Person>() {

			@Override
			public Set<Entry<Id, Person>> entrySet() {
				return new AbstractSet<Entry<Id, Person>>() {

					@Override
					public Iterator<Entry<Id, Person>> iterator() {
						return new PersonEntryIterator();
					}

					@Override
					public int size() {
						throw new RuntimeException();
					}
					
				};
			}

			@Override
			public Person get(Object key) {
				Node node = index.getSingleNode(NeoPersonImpl.KEY_ID, key.toString());
				if (node != null) {
					return new NeoPersonImpl(node);
				} else {
					return null;
				}
			}
			
		};
	}

	@Override
	public void addPerson(Person p) {
		Node personNode = ((NeoPersonImpl) p).getUnderlyingNode();
		underlyingNode.createRelationshipTo(personNode, RelationshipTypes.POPULATION_TO_PERSON);
	}

	@Override
	public PopulationFactory getFactory() {
		return factory;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Id, ? extends Person> getPersons() {
		return map;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub

	}

}

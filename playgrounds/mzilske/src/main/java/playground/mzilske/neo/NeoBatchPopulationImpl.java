package playground.mzilske.neo;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.neo4j.index.lucene.LuceneIndexBatchInserter;
import org.neo4j.kernel.impl.batchinsert.BatchInserter;

import playground.mzilske.neo.NeoBatchNetworkImpl.BasicNetworkRoute;

public class NeoBatchPopulationImpl implements Population {

	private final PopulationFactory factory;

	private final long underlyingNode;

	private BatchInserter inserter;

	private Map<String,Object> properties = new HashMap<String,Object>();

	private LuceneIndexBatchInserter index;

	public NeoBatchPopulationImpl(BatchInserter inserter, LuceneIndexBatchInserter index, long populationNode) {
		this.underlyingNode = populationNode;
		this.inserter = inserter;
		this.index = index;
		this.factory = new PopulationFactory() {

			@Override
			public Activity createActivityFromCoord(String actType, Coord coord) {
				return new ActivityImpl(actType, coord);
			}

			@Override
			public Activity createActivityFromLinkId(String actType, Id linkId) {
				return new ActivityImpl(actType, linkId);
			}

			@Override
			public Leg createLeg(String legMode) {
				return new LegImpl(legMode);
			}

			@Override
			public Person createPerson(Id id) {
				return new PersonImpl(id);
			}

			@Override
			public Plan createPlan() {
				return new PlanImpl();
			}

		};

	}

	@Override
	public void addPerson(Person p) {
		properties.clear();
		properties.put(NeoPersonImpl.KEY_ID, p.getId().toString());
		long personId = inserter.createNode(properties);
		index.index(personId, NeoPersonImpl.KEY_ID, p.getId());

		inserter.createRelationship(underlyingNode, personId, RelationshipTypes.POPULATION_TO_PERSON, null);

		for (Plan plan : p.getPlans()) {
			properties.clear();
			properties.put(NeoPlanImpl.KEY_SCORE, plan.getScore());
			properties.put(NeoPlanImpl.KEY_SELECTED, plan.isSelected());
			long planId = inserter.createNode(properties);
			inserter.createRelationship(personId, planId, RelationshipTypes.PERSON_TO_PLAN, null);

			Long previous = null;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					properties.clear();
					Activity act = (Activity) pe;
					properties.put(NeoActivityImpl.KEY_TYPE, act.getType());
					long id = inserter.createNode(properties);
					if (previous == null) {
						inserter.createRelationship(planId, id, RelationshipTypes.PLAN_TO_FIRST_ACTIVITY, null);
					} else {
						inserter.createRelationship(previous, id, RelationshipTypes.NEXT_PLAN_ELEMENT, null);
					}
					inserter.createRelationship(planId, id, RelationshipTypes.PLAN_TO_ACTIVITY, null);
					if (act.getLinkId() != null) {
						long linkid = index.getSingleNode(NeoLinkImpl.KEY_ID, act.getLinkId());
						inserter.createRelationship(id, linkid, RelationshipTypes.TAKES_PLACE_AT, null);
					}
					previous = id;
				} else if (pe instanceof Leg) {
					properties.clear();
					Leg leg = (Leg) pe;
					properties.put(NeoLegImpl.KEY_MODE, leg.getMode().toString());
					// planNodes.add(inserter.createNode(properties));
					long id = inserter.createNode(properties);
					if (previous == null) {
						inserter.createRelationship(planId, id, RelationshipTypes.PLAN_TO_FIRST_ACTIVITY, null);
					} else {
						inserter.createRelationship(previous, id, RelationshipTypes.NEXT_PLAN_ELEMENT, null);
					}
					inserter.createRelationship(planId, id, RelationshipTypes.PLAN_TO_LEG, null);
					Route route = leg.getRoute();
					if (route != null) {
						long routeId = insertRoute(route);
						inserter.createRelationship(id, routeId, RelationshipTypes.LEG_TO_ROUTE, null);
					}
					previous = id;
				}
			}

		}

	}

	private long insertRoute(Route route) {
		properties.clear();
		if (route instanceof NetworkRoute) {
			BasicNetworkRoute networkRoute = (BasicNetworkRoute) route;
			properties.put(NeoNetworkRouteImpl.KEY_DESCRIPTION, networkRoute.routeDescription);
		}
		long routeId = inserter.createNode(properties);
		if (route instanceof NetworkRoute) {
			long startlinkid = index.getSingleNode(NeoLinkImpl.KEY_ID, route.getStartLinkId());
			long endlinkid = index.getSingleNode(NeoLinkImpl.KEY_ID, route.getEndLinkId());
			inserter.createRelationship(routeId, startlinkid, RelationshipTypes.START_LINK, properties);
			inserter.createRelationship(routeId, endlinkid, RelationshipTypes.END_LINK, properties);
		}
		return routeId;
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
		throw new RuntimeException();
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub

	}

}

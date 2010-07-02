/**
 * 
 */
package playground.mzilske.neo;

import org.neo4j.graphdb.RelationshipType;

enum RelationshipTypes implements RelationshipType {
	PERSON_TO_PLAN, POPULATION_TO_PERSON, PLAN_TO_FIRST_ACTIVITY, NEXT_PLAN_ELEMENT, PLAN_TO_ACTIVITY, PLAN_TO_LEG, LINK_TO, LEG_TO_ROUTE, TAKES_PLACE_AT, ON_ROUTE, NETWORK_TO_NODE, NETWORK_TO_LINK, START_LINK, END_LINK
}
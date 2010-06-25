/**
 * 
 */
package playground.mzilske.neo;

import org.neo4j.graphdb.RelationshipType;

enum RelationshipTypes implements RelationshipType {
	PERSON_TO_PLAN, POPULATION_TO_PERSON, PLAN_TO_FIRST_ACTIVITY, NEXT_PLAN_ELEMENT, PLAN_TO_ACTIVITY, PLAN_TO_LEG
}
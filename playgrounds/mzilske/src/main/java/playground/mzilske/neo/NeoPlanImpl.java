package playground.mzilske.neo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Traverser.Order;

public class NeoPlanImpl implements Plan {

	static final String KEY_SCORE = "score";

	static final String KEY_SELECTED = "isSelected";

	private final Node underlyingNode;

	public NeoPlanImpl(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	@Override
	public void addActivity(Activity act) {
		Node actNode = ((NeoActivityImpl) act).getUnderlyingNode();
		addPlanElement(actNode);
		underlyingNode.createRelationshipTo(actNode, RelationshipTypes.PLAN_TO_ACTIVITY);
	}

	private void addPlanElement(Node actNode) {
		Relationship firstPlanElement = underlyingNode.getSingleRelationship(RelationshipTypes.PLAN_TO_FIRST_ACTIVITY, Direction.OUTGOING);
		if (firstPlanElement != null) {
			Node firstPlanElementNode = firstPlanElement.getEndNode();
			Node lastPlanElement = firstPlanElementNode;
			for (Node node : firstPlanElementNode.traverse(Order.DEPTH_FIRST, StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL, RelationshipTypes.NEXT_PLAN_ELEMENT, Direction.OUTGOING)) {
				lastPlanElement = node;
			}
			lastPlanElement.createRelationshipTo(actNode, RelationshipTypes.NEXT_PLAN_ELEMENT);
		} else {
			underlyingNode.createRelationshipTo(actNode, RelationshipTypes.PLAN_TO_FIRST_ACTIVITY);
		}
	}

	@Override
	public void addLeg(Leg leg) {
		Node legNode = ((NeoLegImpl) leg).getUnderlyingNode();
		addPlanElement(legNode);
		underlyingNode.createRelationshipTo(legNode, RelationshipTypes.PLAN_TO_LEG);
	}

	@Override
	public Person getPerson() {
		Node personNode = underlyingNode.getSingleRelationship(RelationshipTypes.PERSON_TO_PLAN, Direction.INCOMING).getStartNode();
		return new NeoPersonImpl(personNode);
	}

	@Override
	public List<PlanElement> getPlanElements() {
		ArrayList<PlanElement> planElements = new ArrayList<PlanElement>();
		Relationship firstPlanElement = underlyingNode.getSingleRelationship(RelationshipTypes.PLAN_TO_FIRST_ACTIVITY, Direction.OUTGOING);
		if (firstPlanElement != null) {
			for (Node node : firstPlanElement.getEndNode().traverse(Order.DEPTH_FIRST, StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL, RelationshipTypes.NEXT_PLAN_ELEMENT, Direction.OUTGOING)) {
				if (node.hasRelationship(RelationshipTypes.PLAN_TO_LEG)) {
					planElements.add(new NeoLegImpl(node));
				} else if (node.hasRelationship(RelationshipTypes.PLAN_TO_ACTIVITY)) {
					planElements.add(new NeoActivityImpl(node));
				} else {
					throw new RuntimeException();
				}
			}
		}
		return planElements;
	}

	@Override
	public Double getScore() {
		return (Double) underlyingNode.getProperty(KEY_SCORE);
	}

	@Override
	public boolean isSelected() {
		return (Boolean) underlyingNode.getProperty(KEY_SELECTED);
	}

	@Override
	public void setPerson(Person person) {
		// Assume that this is already done with by Person.addPlan?
	}

	@Override
	public void setScore(Double score) {
		underlyingNode.setProperty(KEY_SCORE, score);
	}

	public void setSelected(boolean selected) {
		underlyingNode.setProperty(KEY_SELECTED, selected);
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	public Node getUnderlyingNode() {
		return underlyingNode;
	}

}

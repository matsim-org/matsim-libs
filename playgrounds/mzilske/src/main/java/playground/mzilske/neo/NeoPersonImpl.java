package playground.mzilske.neo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class NeoPersonImpl implements Person {

	static final String KEY_ID = "id";
	
	final private Node underlyingNode;

	public NeoPersonImpl(Node personNode) {
		this.underlyingNode = personNode;
	}

	@Override
	public boolean addPlan(Plan p) {
		Node planNode = ((NeoPlanImpl) p).getUnderlyingNode();
		underlyingNode.createRelationshipTo(planNode, RelationshipTypes.PERSON_TO_PLAN);
		return true;
	}

	@Override
	public List<? extends Plan> getPlans() {
		ArrayList<Plan> plans = new ArrayList<Plan>();
		for (Relationship r : underlyingNode.getRelationships(RelationshipTypes.PERSON_TO_PLAN)) {
			plans.add(new NeoPlanImpl(r.getEndNode()));
		}
		return plans;
	}

	@Override
	public Plan getSelectedPlan() {
		for (Relationship r : underlyingNode.getRelationships(RelationshipTypes.PERSON_TO_PLAN)) {
			Node planNode = r.getEndNode();
			Plan plan = new NeoPlanImpl(planNode);
			if (plan.isSelected()) {
				return plan;
			}
		}
		return null;
	}

	@Override
	public void setId(Id id) {
		underlyingNode.setProperty(KEY_ID, id.toString());
	}

	@Override
	public Id getId() {
		return new IdImpl((String) underlyingNode.getProperty(KEY_ID));
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

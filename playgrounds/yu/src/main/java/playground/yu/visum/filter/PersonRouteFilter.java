package playground.yu.visum.filter;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;

/**
 * transfer the "right" persons to next PersonFilter. These "right" persons
 * don't move on false links and nodes, which should not exist in network(file).
 * 
 * beurteilen, ob Personen auf nicht existierende Links oder durch nicht
 * existierende Nodes fahren oder andere Aktivitaeten durchfuehren. Die
 * richtigen Personen wurden hier zur NewPlansWriter übertragen.
 * 
 * @author yu chen
 */
public class PersonRouteFilter extends PersonFilterA {
	/**
	 * The underlying list of link-IDs of this PersonRouteFilter.
	 */
	private List<Id> criterionLinkIds;

	/**
	 * The underlying list of node-IDs of this PersonRouteFilter.
	 */
	private List<Id> criterionNodeIds;

	/**
	 * create a PersonFilter, which deletes Persons moving or staying on some
	 * links and nodes, which should not exist.
	 * 
	 * @param linkIds -
	 *            a list of link-IDs, which should not exist in network-file.
	 * @param nodeIds -
	 *            a list of node-IDs, which should not exist in network-file
	 * 
	 */
	public PersonRouteFilter(List<Id> linkIds, List<Id> nodeIds) {
		this.criterionLinkIds = linkIds;
		this.criterionNodeIds = nodeIds;
	}

	/**
	 * judge, whether the person will move or stay on some links and nodes,
	 * which should not exist in network(file).
	 */
	@Override
	public boolean judge(Person person) {
		List<? extends Plan> plans = person.getPlans();
		for (Plan plan : plans) {
			if (plan.isSelected()) {
				List<? extends PlanElement> acts_Legs = plan.getPlanElements();
				boolean even = false;
				for (PlanElement obj : acts_Legs) {
					if (even) {
						LegImpl leg = (LegImpl) obj;
						NetworkRouteWRefs route = (NetworkRouteWRefs) leg.getRoute();
						if (route != null) {
							List<Id> linkIds = route.getLinkIds();
							if (linkIds != null)
								for (Id linkId : linkIds) {
									if (this.criterionLinkIds.contains(linkId))
										return false;
								}
							List<Node> nodes = route.getNodes();
							if (nodes != null)
								for (Node node : nodes) {
									if (this.criterionNodeIds.contains(node
											.getId()))
										return false;
								}
						}
					} else {
						ActivityImpl act = (ActivityImpl) obj;
						if (this.criterionLinkIds.contains(act.getLinkId()))
							return false;
					}
					even = !even;
				}
			}
		}
		return true;
	}
}

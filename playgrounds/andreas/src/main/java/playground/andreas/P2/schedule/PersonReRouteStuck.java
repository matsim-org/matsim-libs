package playground.andreas.P2.schedule;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.router.TransitActsRemover;

/**
 * ReRoutes every person from a given set of person ids
 *
 * @author aneumann
 */
public class PersonReRouteStuck extends AbstractPersonAlgorithm {

	private final PlanAlgorithm router;
	private final Network  network;

	private static final Logger log = Logger.getLogger(PersonReRouteStuck.class);
	
	private TransitActsRemover transitActsRemover;
	private Set<Id> agentsStuck;

	public PersonReRouteStuck(final PlanAlgorithm router, final ScenarioImpl scenario, Set<Id> agentsStuck) {
		super();
		this.router = router;
		this.network = scenario.getNetwork();
		Network net = this.network;
		if (NetworkUtils.isMultimodal(network)) {
			log.info("Network seems to be multimodal. XY2Links will only use car links.");
			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
			net = NetworkImpl.createNetwork();
			HashSet<String> modes = new HashSet<String>();
			modes.add(TransportMode.car);
			filter.filter(net, modes);
		}
		this.agentsStuck = agentsStuck;
		this.transitActsRemover = new TransitActsRemover(); 
		log.info("initialized");
	}
	
	@Override
	public void run(final Person person) {
		Plan selectedPlan = person.getSelectedPlan();
		if (selectedPlan == null) {
			// the only way no plan can be selected should be when the person has no plans at all
			log.warn("Person " + person.getId() + " has no plans!");
			return;
		}
		
		if(this.agentsStuck.contains(person.getId())){
			this.transitActsRemover.run(selectedPlan);
			this.router.run(selectedPlan);
		}
	}
}
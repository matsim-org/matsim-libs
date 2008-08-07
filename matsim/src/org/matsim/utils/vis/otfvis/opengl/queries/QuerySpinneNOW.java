package org.matsim.utils.vis.otfvis.opengl.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.population.Plan;
import org.matsim.population.Population;

public class QuerySpinneNOW extends QuerySpinne {

	/* (non-Javadoc)
	 * @see org.matsim.utils.vis.otfvis.opengl.queries.QuerySpinne#getPersons(org.matsim.population.Plans)
	 */
	@Override
	protected List<Plan> getPersons(Population plans, QueueNetwork net) {
		List<Plan> actPersons = new ArrayList<Plan>();
		QueueLink link = net.getLinks().get(linkId);
		Collection<Vehicle> vehs = link.getAllVehicles();
		for( Vehicle veh : vehs) actPersons.add(veh.getDriver().getSelectedPlan());
		
		return actPersons;
	}

}

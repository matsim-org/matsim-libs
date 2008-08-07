package org.matsim.utils.vis.otfvis.opengl.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.Vehicle;
import org.matsim.population.Plan;
import org.matsim.population.Plans;

public class QuerySpinneNOW extends QuerySpinne {

	/* (non-Javadoc)
	 * @see org.matsim.utils.vis.otfvis.opengl.queries.QuerySpinne#getPersons(org.matsim.population.Plans)
	 */
	@Override
	protected List<Plan> getPersons(Plans plans, QueueNetworkLayer net) {
		List<Plan> actPersons = new ArrayList<Plan>();
		QueueLink link = net.getLinks().get(linkId);
		Collection<Vehicle> vehs = link.getAllVehicles();
		for( Vehicle veh : vehs) actPersons.add(veh.getDriver().getSelectedPlan());
		
		return actPersons;
	}

}

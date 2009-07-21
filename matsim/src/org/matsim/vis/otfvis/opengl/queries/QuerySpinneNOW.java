package org.matsim.vis.otfvis.opengl.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;

/**
 * QuerySpinneNOW is a special case of QuerySpinne, where not all agents passing the link are 
 * considered, but only the ones traveling the links at the time given.
 * 
 * @author dstrippgen
 *
 */
public class QuerySpinneNOW extends QuerySpinne {

	/* (non-Javadoc)
	 * @see org.matsim.utils.vis.otfvis.opengl.queries.QuerySpinne#getPersons(org.matsim.population.Plans)
	 */
	@Override
	protected List<PlanImpl> getPersons(PopulationImpl plans, QueueNetwork net) {
		List<PlanImpl> actPersons = new ArrayList<PlanImpl>();
		QueueLink link = net.getLinks().get(linkId);
		Collection<QueueVehicle> vehs = link.getAllVehicles();
		for( QueueVehicle veh : vehs) actPersons.add(veh.getDriver().getPerson().getSelectedPlan());
		
		return actPersons;
	}

}

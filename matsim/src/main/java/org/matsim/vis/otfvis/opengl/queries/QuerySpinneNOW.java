package org.matsim.vis.otfvis.opengl.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.vis.snapshots.writers.VisLink;
import org.matsim.vis.snapshots.writers.VisNetwork;
import org.matsim.vis.snapshots.writers.VisVehicle;

/**
 * QuerySpinneNOW is a special case of QuerySpinne, where not all agents passing the link are 
 * considered, but only the ones traveling the links at the time given.
 * 
 * @author dstrippgen
 *
 */
public class QuerySpinneNOW extends QuerySpinne {

	@Override
	protected List<Plan> getPersons(Population plans, VisNetwork net) {
		List<Plan> actPersons = new ArrayList<Plan>();
		VisLink link = net.getVisLinks().get(queryLinkId);
		Collection<? extends VisVehicle> vehs = link.getAllVehicles();
		for( VisVehicle veh : vehs) actPersons.add(veh.getDriver().getSelectedPlan());
		
		return actPersons;
	}

}

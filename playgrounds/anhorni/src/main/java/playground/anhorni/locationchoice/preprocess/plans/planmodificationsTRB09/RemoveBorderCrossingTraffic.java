package playground.anhorni.locationchoice.preprocess.plans.planmodificationsTRB09;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;

public class RemoveBorderCrossingTraffic extends Modifier {

	private final static Logger log = Logger.getLogger(RemoveBorderCrossingTraffic.class);

	public RemoveBorderCrossingTraffic(PopulationImpl plans, NetworkLayer network, ActivityFacilitiesImpl  facilities){
		super(plans, network, facilities);
	}


	@Override
	public void modify() {
		this.removeBorderCrossingPersonsTraffic();
	}

	private void removeBorderCrossingPersonsTraffic() {

		List<Id> toRemoveList=new Vector<Id>();

		// find border crossing persons
		log.info("running removeBorderCrossingPersonsTraffic:");
		for (Person person : this.plans.getPersons().values()) {
			//if (person.getId().compareTo(new IdImpl(1000000000))>0) {
			if (Integer.valueOf(person.getId().toString())>1000000000) {
				toRemoveList.add(person.getId());
			}
		}

		//and remove them
		Iterator<Id> id_it = toRemoveList.iterator();
		while (id_it.hasNext()) {
			Id id = id_it.next();
			this.plans.getPersons().remove(id);
		}
		log.info("Removed " + toRemoveList.size()+ " persons");
		log.info("RemoveBorderCrossingTraffic done.");
	}
}

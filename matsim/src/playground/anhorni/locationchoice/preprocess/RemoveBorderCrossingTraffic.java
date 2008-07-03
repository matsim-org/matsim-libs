package playground.anhorni.locationchoice.preprocess;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facilities;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.utils.misc.Counter;

public class RemoveBorderCrossingTraffic extends Modifier {

	private final static Logger log = Logger.getLogger(RemoveBorderCrossingTraffic.class);

	public RemoveBorderCrossingTraffic(Plans plans, NetworkLayer network, Facilities  facilities){
		super(plans, network, facilities);
	}


	@Override
	public void modify() {
		this.removeBorderCrossingPersonsTraffic();
	}


	private void removeBorderCrossingPersonsTraffic() {

		log.info("running removeBorderCrossingPersonsTraffic:");
		Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
		Counter counter = new Counter(" person # ");
		while (person_iter.hasNext()) {
			Person person = person_iter.next();
			counter.incCounter();
			if (person.getId().compareTo(new IdImpl(1000000000))>0) {
				this.plans.getPersons().remove(person.getId());
			}
		}
		log.info("RemoveBorderCrossingTraffic done.");
	}
}

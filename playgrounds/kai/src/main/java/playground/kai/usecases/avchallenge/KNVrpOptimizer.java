/**
 * 
 */
package playground.kai.usecases.avchallenge;

import java.util.LinkedList;
import java.util.Queue;

import javax.inject.Inject;

import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author nagel
 *
 */
class KNVrpOptimizer implements VrpOptimizer {
	
	@Inject MobsimTimer timer ;
	
	private Queue<Request> requests = new LinkedList<>() ;

	KNVrpOptimizer() {
	}

	@Override
	public void requestSubmitted(Request request) {
		requests.add( request ) ;
	}

	@Override
	public void nextTask(Schedule<? extends Task> schedule) {
		schedule.getTasks().clear(); 
		
		Request rr = requests.poll() ;
		if ( rr==null ) {
//			schedule.addTask( new Task(){} ) ; // yyyyyy this is not allowed because of the <? extends Task>.  Deliberately?  kai, jan'17
			return ;
		}
	}
	
	
}

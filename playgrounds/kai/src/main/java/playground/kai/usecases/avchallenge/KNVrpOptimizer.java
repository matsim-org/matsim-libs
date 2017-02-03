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
import org.matsim.contrib.dvrp.tracker.TaskTracker;
import org.matsim.contrib.taxi.schedule.TaxiTask;
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
	public void nextTask(Schedule schedule) {
		schedule.getTasks().clear(); 
		
		Request rr = requests.poll() ;
		if ( rr==null ) { 
			// no request in queue, schedule 
			schedule.addTask( new TaxiTask(){
				@Override public TaskStatus getStatus() {
					return TaskStatus.STARTED ;
				}
				@Override public double getBeginTime() {
					return timer.getTimeOfDay() ;
				}
				@Override public double getEndTime() {
					return Double.POSITIVE_INFINITY ;
				}
				@Override public Schedule getSchedule() {
					return schedule ;
				}
				@Override public int getTaskIdx() {
					return 0;
				}
				@Override public void setBeginTime(double beginTime) {
					throw new RuntimeException("when is this called?") ;
				}
				@Override public void setEndTime(double endTime) {
					throw new RuntimeException("when is this called?") ;
				}
				@Override public TaskTracker getTaskTracker() {
					throw new RuntimeException("what is this?" ) ;
				}

				@Override
				public void initTaskTracker(TaskTracker taskTracker) {
					throw new RuntimeException("what is this?" ) ;
				}
				@Override
				public TaxiTaskType getTaxiTaskType() {
					// TODO Auto-generated method stub
					return null;
				}
			} ) ;
			return ;
		}
	}
	
	
}

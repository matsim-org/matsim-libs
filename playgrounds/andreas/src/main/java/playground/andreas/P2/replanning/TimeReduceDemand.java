package playground.andreas.P2.replanning;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;

/**
 * 
 * Limits demand to periods with demand.
 * 
 * @author aneumann
 *
 */
public class TimeReduceDemand extends PStrategy implements PPlanStrategy, PersonEntersVehicleEventHandler{
	
	private final static Logger log = Logger.getLogger(TimeReduceDemand.class);
	
	public static final String STRATEGY_NAME = "TimeReduceDemand";
	
	private final int timeBinSize;
	private HashMap<String, int[]> lineId2DemandTimeBins = new HashMap<String, int[]>();
	
	public TimeReduceDemand(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 1){
			log.error("Too many parameter. Will ignore: " + parameter);
		}
		this.timeBinSize = Integer.parseInt(parameter.get(0));
		log.info("enabled");
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		// get start Time
		
		int[] demandTimeBins = this.lineId2DemandTimeBins.get(cooperative.getId().toString());
		double startTime = 0.0;
		double endTime = 24 * 3600.0;
		
		for (int i = 0; i < demandTimeBins.length; i++) {
			if(demandTimeBins[i] == 0){
				startTime = this.timeBinSize * i;
			} else {
				break;
			}			
		}
		
		for (int i = demandTimeBins.length - 1; i > 0; i--) {
			if(demandTimeBins[i] == 0){
				endTime = this.timeBinSize * i;
			} else {
				break;
			}			
		}
				
		// profitable route, change startTime
		PPlan newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration()));
		newPlan.setStartStop(cooperative.getBestPlan().getStartStop());
		newPlan.setEndStop(cooperative.getBestPlan().getEndStop());
		newPlan.setStartTime(startTime);
		newPlan.setEndTime(endTime);
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStartStop(), newPlan.getEndStop(), new IdImpl(cooperative.getCurrentIteration())));
		
		return newPlan;
	}

	@Override
	public String getName() {
		return TimeReduceDemand.STRATEGY_NAME;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith("p_")){
			String lineId = event.getVehicleId().toString().split("-")[0];

			if(this.lineId2DemandTimeBins.get(lineId) == null){
				this.lineId2DemandTimeBins.put(lineId, new int[24*4+1]);
			}
			
			int slot = ((int) event.getTime() / this.timeBinSize);
			if(slot < this.lineId2DemandTimeBins.get(lineId).length - 1){
				this.lineId2DemandTimeBins.get(lineId)[slot]++;
			} else {
				this.lineId2DemandTimeBins.get(lineId)[this.lineId2DemandTimeBins.get(lineId).length - 1]++;
			}
		}
	}

}

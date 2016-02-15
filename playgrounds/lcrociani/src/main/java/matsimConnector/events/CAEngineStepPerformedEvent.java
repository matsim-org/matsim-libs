package matsimConnector.events;

import java.util.Map;

import org.matsim.api.core.v01.events.Event;

public class CAEngineStepPerformedEvent extends Event{
	public static final String EVENT_TYPE = "CAEngineStepPerformed";
	public static final String ATTRIBUTE_STEP_COMPUTATION = "stepComputationalTime";
	public static final String ATTRIBUTE_POPULATION_SIZE = "populationSize";
	private final float stepCompTime;
	private final int populationSize;
	
	
	public CAEngineStepPerformedEvent(double time, float stepComputationalTime, int populationSize) {
		super((int)time+1);
		this.stepCompTime = stepComputationalTime;
		this.populationSize = populationSize;
	}
	
	
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_STEP_COMPUTATION, ""+stepCompTime);
		attr.put(ATTRIBUTE_STEP_COMPUTATION, ""+populationSize);
		return attr;
	}

	public float getStepCompTime() {
		return stepCompTime;
	}
	
	
	public int getPopulationSize() {
		return populationSize;
	}
	
}

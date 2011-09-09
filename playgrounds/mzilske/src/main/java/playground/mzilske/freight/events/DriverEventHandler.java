package playground.mzilske.freight.events;


public interface DriverEventHandler extends CarrierEventHandler{
	public abstract void handleEvent(DriverPerformanceEvent event);
}

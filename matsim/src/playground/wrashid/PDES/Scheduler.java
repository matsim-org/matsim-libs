package playground.wrashid.PDES;

import java.util.HashMap;

public class Scheduler {
	static double simTime=0;
	static MessageQueue queue=new MessageQueue();
	HashMap<Long,SimUnit> simUnits=new HashMap<Long, SimUnit>();


	
	
	public static void schedule(Message m){		
		if (m.getMessageArrivalTime()>=simTime){
			queue.putMessage(m);
		} else {
			System.out.println("WARNING: You tried to put message in the past. Request discarded.");
		}
	}
	
	
	public void startSimulation(){
		initializeSimulation();
		
		Message m;
		while(queue.hasElement() && simTime<SimulationParameters.simulationLength){
			m=queue.getNextMessage();
			simTime=m.getMessageArrivalTime();
			if (m instanceof SelfhandleMessage){
				((SelfhandleMessage) m).selfhandleMessage();
			} else {
				m.receivingUnit.handleMessage(m);
			}
		}
	}
	
	
	public void register(SimUnit su){
		simUnits.put(new Long(su.unitNo), su);
	}
	
	public Object getSimUnit(long unitId){
		return simUnits.get(new Long(unitId));
	}
	
	
	// attention: this procedure only invokes
	// the initialization method of objects, which
	// exist at the beginning of the simulation
	public void initializeSimulation(){
		Object[] objects=simUnits.values().toArray();
		SimUnit su;
		
		for (int i=0;i<objects.length;i++){
			su=(SimUnit) objects[i];
			su.initialize();
		}
	}


	public double getSimTime() {
		return simTime;
	}


	public void unregister(SimUnit unit) {
		simUnits.remove(new Long(unit.unitNo));
	}
	
	
}

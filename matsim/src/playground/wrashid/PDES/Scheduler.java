package playground.wrashid.PDES;

import java.util.HashMap;

public class Scheduler {
	static long simTime=0;
	static MessageQueue queue=new MessageQueue();
	static HashMap<Long,SimUnit> simUnits=new HashMap<Long, SimUnit>();


	
	
	public static void schedule(Message m){		
		if (m.getMessageArrivalTime()>=simTime){
			queue.putMessage(m);
		} else {
			System.out.println("WARNING: You tried to put message in the past. Request discarded.");
		}
	}
	
	
	public static void startSimulation(){
		initializeSimulation();
		
		Message m;
		while(queue.hasElement() && simTime<SimulationParameters.simulationLength){
			m=queue.getNextMessage();
			simTime=m.getMessageArrivalTime();
			m.receivingUnit.handleMessage(m);
		}
	}
	
	
	public static void register(SimUnit su){
		simUnits.put(new Long(su.unitNo), su);
	}
	
	public static Object getSimUnit(long unitId){
		return simUnits.get(new Long(unitId));
	}
	
	
	// attention: this procedure only invokes
	// the initialization method of objects, which
	// exist at the beginning of the simulation
	public static void initializeSimulation(){
		Object[] objects=simUnits.values().toArray();
		SimUnit su;
		
		for (int i=0;i<objects.length;i++){
			su=(SimUnit) objects[i];
			su.initialize();
		}
	}


	public static long getSimTime() {
		return simTime;
	}


	public static void unregister(SimUnit unit) {
		simUnits.remove(new Long(unit.unitNo));
	}
	
	
}

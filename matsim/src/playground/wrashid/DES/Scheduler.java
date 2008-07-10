package playground.wrashid.DES;

import java.util.HashMap;

public class Scheduler {
	double simTime=0;
	MessageQueue queue=new MessageQueue();
	HashMap<Long,SimUnit> simUnits=new HashMap<Long, SimUnit>();


	
	
	public void schedule(Message m){		
		if (m.getMessageArrivalTime()>=simTime){	
			queue.putMessage(m);
		} else {
			System.out.println("WARNING: You tried to send a message in the past. Message discarded.");
			//System.out.println("m.getMessageArrivalTime():"+m.getMessageArrivalTime());
			//System.out.println("simTime:"+simTime);
			//System.out.println(m.getClass());
			assert(false); // for backtracing, where a wrong message has been scheduled
		}
	}
	
	
	public void startSimulation(){
		initializeSimulation();
		
		Message m;
		while(queue.hasElement() && simTime<SimulationParameters.simulationLength){
			m=queue.getNextMessage();
			simTime=m.getMessageArrivalTime();
			m.printMessageLogString();
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

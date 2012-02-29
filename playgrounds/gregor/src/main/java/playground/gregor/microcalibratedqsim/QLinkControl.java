
package playground.gregor.microcalibratedqsim;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.ptproject.qsim.InternalInterface;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.qnetsimengine.NetsimNetwork;

import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.gregor.sim2d_v2.statistics.OnLinkStatistics;
import playground.gregor.sim2d_v2.statistics.OnLinkStatistics.AgentInfo;


public class QLinkControl implements MobsimEngine{

	private final NetsimNetwork nn;

	private final List<Id> controlledLinks = new ArrayList<Id>();

	
	private final Map<Id,LinkVelocityClassifier> lvcs = new HashMap<Id,LinkVelocityClassifier>(); 
	
	private final NetworkImpl n;
	private final NetworkChangeEventFactory nceFac;

	public QLinkControl(QSim sim) {

		

		this.nn = sim.getNetsimNetwork();
		this.n = (NetworkImpl)this.nn.getNetwork();
		this.nceFac = new NetworkChangeEventFactoryImpl();

		init();
	}

	private void init() {
		//TODO setter for controlled links
		this.controlledLinks.clear();
		this.controlledLinks.add(new IdImpl(4));
		this.controlledLinks.add(new IdImpl(16));
		this.controlledLinks.add(new IdImpl(5));
		this.controlledLinks.add(new IdImpl(17));
		OnLinkStatistics lees = new OnLinkStatistics();
		for (Id id : this.controlledLinks) {
			lees.addObservationLinkId(id);
			this.lvcs.put(id,  new SVMClassifier());
		}
		
		lees.addObservationLinkId(new IdImpl(3));
		lees.addObservationLinkId(new IdImpl(15));
		EventsManager emngr = EventsUtils.createEventsManager();
		emngr.addHandler(lees);
		new XYVxVyEventsFileReader(emngr).parse("/Users/laemmel/devel/gr90/input/events_walk2d.xml.gz");
//		new XYVxVyEventsFileReader(emngr).parse("/Users/laemmel/devel/gr90/input/events.xml");
		
		intitCLassifier(new IdImpl(4),new Id[]{new IdImpl(15),new IdImpl(16)},lees);	
		intitCLassifier(new IdImpl(5),new Id[]{new IdImpl(16),new IdImpl(17)},lees);
		intitCLassifier(new IdImpl(16),new Id[]{new IdImpl(3),new IdImpl(4)},lees);
		intitCLassifier(new IdImpl(17),new Id[]{new IdImpl(4),new IdImpl(5)},lees);
//		intitCLassifier(new IdImpl(3),new Id[]{new IdImpl(2)},lees);	
//		intitCLassifier(new IdImpl(8),new Id[]{new IdImpl(7)},lees);
	}

	private void intitCLassifier(IdImpl idImpl, Id[] ids, OnLinkStatistics lees) {
		LinkVelocityClassifier classifier = this.lvcs.get(idImpl);
		List<AgentInfo> ais = lees.getAndRemoveAgentInfos(idImpl);
		double ll = this.n.getLinks().get(idImpl).getLength();
		Id [] predIds = new Id[ids.length+1];
		for (int i = 0; i < ids.length; i++) {
			predIds[i] = ids[i];
		}	
		predIds[predIds.length-1] = idImpl;
		
//		//default
//		for (int j = 0; j <=5; j++) {
//		double [] defPred = new double [ids.length+1];
//		for (int i =0; i < defPred.length; i++) {
//			defPred[i] = MatsimRandom.getRandom().nextInt(1);
//			classifier.addInstance(defPred, 1.34);
//		}
//		
//		}
		
		
		
		for (AgentInfo ai : ais) {
//			if (MatsimRandom.getRandom().nextDouble() > 0.1) {
//				continue;
//			}
			double [] predictors = new double [ids.length+1];
			double t = ai.enterTime;
			double v = ll/(ai.travelTime);//TODO subtracted here one second for the "node time" needs to be revised!!!
			for (int i = 0; i < predIds.length; i++) {
				Id id = predIds[i];
				double[] tmp = lees.getOnLinkHistory(id, t, 1);
				predictors[i] = tmp[0];
			}
			classifier.addInstance(predictors, v);
		}
		
		classifier.setIds(predIds);
		classifier.build();
	}

	private void handleLink(double time, Id id) {
		LinkVelocityClassifier lvc = this.lvcs.get(id);
		Id[] ids = lvc.getIds();
		double [] predictors = new double [ids.length];
		for (int i = 0; i < ids.length; i++) {
			Id idd = ids[i];
			int onLink = this.nn.getNetsimLink(idd).getAllNonParkedVehicles().size();
			predictors[i] = onLink;
		}
		double v = lvc.getEstimatedVelocity(predictors);

		NetworkChangeEvent event = this.nceFac.createNetworkChangeEvent(time);
		event.addLink(this.n.getLinks().get(id));
		ChangeValue val = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,v);
		event.setFreespeedChange(val);
		
//		ChangeValue cap = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,1000);
//		ChangeValue lanes = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,1000);
//		event.setFlowCapacityChange(cap);
//		event.setLanesChange(lanes);
		
		this.n.addNetworkChangeEvent(event);
		this.nn.getNetsimLink(id).recalcTimeVariantAttributes(time);

	}

	
	
	@Override
	public void doSimStep(double time) {
		for (Id id : this.controlledLinks) {
			handleLink(time,id);
		}
	}

	@Override
	public Netsim getMobsim() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onPrepareSim() {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterSim() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		// TODO Auto-generated method stub

	}





}

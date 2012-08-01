
package playground.gregor.microcalibratedqsim;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetwork;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkImpl;



public class QLinkControl implements MobsimEngine{

	private final NetsimNetwork nn;

	private final Map<Id,LinkInfo> controlledLinks = new LinkedHashMap<Id, LinkInfo>();

	private final NetworkImpl n;
	private final NetworkChangeEventFactory nceFac;


	double lastUpadte = Double.NEGATIVE_INFINITY;

	private final int binSize;

	private BufferedWriter bw;

	public QLinkControl(QSim sim) {

		try {
			this.bw = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/ICEM/dens")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		this.nn = sim.getNetsimNetwork();
		this.n = (NetworkImpl)this.nn.getNetwork();
		if (this.n.getNetworkChangeEvents() != null) {
			this.n.setNetworkChangeEvents(new ArrayList<NetworkChangeEvent>());
		}
		this.nceFac = new NetworkChangeEventFactoryImpl();
		
		this.binSize = sim.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize();
		
		
		testInit();

	}


	private void testInit() {

		addControlledLink(new IdImpl("35497"));
		addControlledLink(new IdImpl("35498"));
		addControlledLink(new IdImpl("44963"));
		addControlledLink(new IdImpl("56015"));
		addControlledLink(new IdImpl("53065"));
		addControlledLink(new IdImpl("53066"));
		addControlledLink(new IdImpl("34807"));
		addControlledLink(new IdImpl("34808"));
		addControlledLink(new IdImpl("24987"));
		addControlledLink(new IdImpl("24988"));
		addControlledLink(new IdImpl("18075"));
		addControlledLink(new IdImpl("628"));
		addControlledLink(new IdImpl("34862"));
		addControlledLink(new IdImpl("61118"));
		addControlledLink(new IdImpl("41981"));
		addControlledLink(new IdImpl("7505"));
		addControlledLink(new IdImpl("39878"));
		addControlledLink(new IdImpl("39879"));
	}

	private void handleLink(double time, Id id) {

		NetsimLink link = this.nn.getNetsimLinks().get(id);
		int veh = link.getAllNonParkedVehicles().size();

		LinkInfo li = this.controlledLinks.get(id);

//		
//		3) Fundamentaldiagramm bei offenen Randbedingungen
//		Für das FD auf einem Ring gibt es drei "regimes":
//		a) free (f ~ D)
//		b) capacity (f = C)
//		c) jam (f = C * (1 - (D-D*)/(D_max-D*))
//		mit D* = 1-C/N_sites
//		für D=D*: f=C und für D=D_max: f=0
		
		
		
		double D = veh/li.spaceCap;
		double f = li.flowCap;
		if (D > li.DStar) {
			f = (li.flowCap * (1 - (D-li.DStar)/(1-li.DStar)));
			f = Math.max(f, li.flowCap/10);
			
		}
		

		NetworkChangeEvent event = this.nceFac.createNetworkChangeEvent(time);
		event.addLink(this.n.getLinks().get(id));
		ChangeValue val = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE,f);
		event.setFlowCapacityChange(val);


		this.n.addNetworkChangeEvent(event);
		this.nn.getNetsimLink(id).recalcTimeVariantAttributes(time);
		
		if (id.toString().equals("18075")) {
			try {
				this.bw.append(D + "\t" + f + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}



	@Override
	public void doSimStep(double time) {
		
		if (this.lastUpadte + this.binSize > time) {
			return;
		}
		
		for (Id id : this.controlledLinks.keySet()) {
			handleLink(time,id);
		}
		
		this.lastUpadte = time;
	}

	@Override
	public void onPrepareSim() {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterSim() {
		try {
			this.bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		// TODO Auto-generated method stub

	}

	public void addControlledLink(Id id) {
		Link l = this.n.getLinks().get(id);
		double cap = l.getCapacity(Double.NEGATIVE_INFINITY);
		double cs = this.n.getEffectiveCellSize();
		double spaceCap =  (l.getNumberOfLanes(Double.NEGATIVE_INFINITY) * (l.getLength() / cs));
		LinkInfo li = new LinkInfo();
		double period = this.n.getCapacityPeriod();
		li.flowCap = cap/period;
		li.spaceCap = spaceCap;
		li.DStar = 0.5; //1 - li.flowCap/li.spaceCap;
		this.controlledLinks.put(id, li);

	}


	private static final class LinkInfo {
		public double DStar;
		double flowCap;
		double spaceCap;
	}



}

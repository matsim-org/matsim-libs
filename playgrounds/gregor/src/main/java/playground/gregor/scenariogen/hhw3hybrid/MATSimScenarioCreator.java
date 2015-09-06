package playground.gregor.scenariogen.hhw3hybrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jj2000.j2k.entropy.encoder.EntropyCoder;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Transition;

public class MATSimScenarioCreator {
	
	private static final Logger log = Logger.getLogger(MATSimScenarioCreator.class);
	
	private static final double EPSILON = 0.25;

	private JuPedSimGeomtry geo;
	
	private Map<Transition,Node> tn = new HashMap<>();
	private Map<Node,Transition> nt = new HashMap<>();
	
	private List<Id<Link>> ext2q = new ArrayList<>();
	private List<Id<Link>> q2ext = new ArrayList<>();

	private Scenario sc;

	private Map<Node,Node> nn = new HashMap<>();

	private int frId;

	private Set<Id<Link>> excl = new HashSet<>();

	private Scenario nsc;

	public MATSimScenarioCreator(JuPedSimGeomtry geo) {
		this.geo = geo;
		Config c = ConfigUtils.createConfig();
		c.network().setInputFile("/Users/laemmel/devel/hhw_hybrid/input/network.xml.gz");
		c.network().setChangeEventInputFile("/Users/laemmel/devel/hhw_hybrid/input/networkChangeEvents.xml.gz");
		Scenario sc = ScenarioUtils.createScenario(c);
		new MatsimNetworkReader(sc).parse(c.network().getInputFile());
		this.sc = sc;
	}
	
	public void run() {
		//0. cleanup network
		cleanupNetwork();
		
		//1. find matches
		findMatches();
		
		//2. create new network with corrected node ids
		buildNewNetwork();
		
		//3. create center node & new links
		createJPSLinksAndNode();
		
		//5. set modes correct (2ext, ext2)
		setModes();
		
		
		Network impl = nsc.getNetwork();
		new NetworkWriter(impl).write("/Users/laemmel/arbeit/papers/2015/trgindia2015/hhwsim/input/network.xml.gz");
		
		createConfig();
	}

	private void createConfig() {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		String inputDir = "/Users/laemmel/arbeit/papers/2015/trgindia2015/hhwsim/input/";
		String outputDir = "/Users/laemmel/arbeit/papers/2015/trgindia2015/hhwsim/output/";


		((NetworkImpl) sc.getNetwork()).setEffectiveCellSize(.26);
		((NetworkImpl) sc.getNetwork()).setEffectiveLaneWidth(.71);
		((NetworkImpl) sc.getNetwork()).setCapacityPeriod(1);

		c.network().setInputFile(inputDir + "/network.xml.gz");

		// c.strategy().addParam("Module_1",
		// "playground.gregor.sim2d_v4.replanning.Sim2DReRoutePlanStrategy");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "50");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".9");

		c.controler().setOutputDirectory(outputDir);
		c.controler().setLastIteration(100);

		c.plans().setInputFile(inputDir + "/population.xml.gz");

		ActivityParams pre = new ActivityParams("origin");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when
		// running a simulation one gets
		// "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in
		// ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);

		ActivityParams post = new ActivityParams("destination");
		post.setTypicalDuration(49); // dito
		post.setMinimalDuration(49);
		post.setClosingTime(49);
		post.setEarliestEndTime(49);
		post.setLatestStartTime(49);
		post.setOpeningTime(49);
		sc.getConfig().planCalcScore().addActivityParams(pre);
		sc.getConfig().planCalcScore().addActivityParams(post);

		sc.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
		sc.getConfig().planCalcScore().setPerforming_utils_hr(0.);

		QSimConfigGroup qsim = sc.getConfig().qsim();
		// qsim.setEndTime(20 * 60);
		c.controler().setMobsim("extsim");
		c.global().setCoordinateSystem("EPSG:3395");

		c.qsim().setEndTime(4 * 3600);

		new ConfigWriter(c).write(inputDir + "/config.xml");

		
	}

	private void setModes() {
		Network impl = nsc.getNetwork();
		for (Id<Link> qe : this.q2ext){
			Link l = impl.getLinks().get(qe);
			Set<String> modes = new HashSet<>(l.getAllowedModes());
			modes.add("2ext");
			l.setAllowedModes(modes);
		}
		for (Id<Link> qe : this.ext2q){
			Link l = impl.getLinks().get(qe);
			if (l == null) {
				continue;
			}
			Set<String> modes = new HashSet<>(l.getAllowedModes());
			modes.add("ext2");
			modes.add("car");
			l.setAllowedModes(modes);
			
		}
		
	}

	private void createJPSLinksAndNode() {
		
		Network impl = nsc.getNetwork();
		NetworkFactory fac = impl.getFactory();

		
		double locX = 0;
		double locY = 0;
		
		for (Node n : this.nn.values()) {
			locX += n.getCoord().getX();
			locY += n.getCoord().getY();
		}
		locX /= this.nn.size();
		locY /= this.nn.size();
		Node centerN = fac.createNode(Id.createNodeId(this.frId), new Coord(locX, locY));
		impl.addNode(centerN);
		
		
		double length = 10;
		double fs = 1.34;
		double lanes = 1;
		double cap = 3600;
		
		int id = 0;
		for (Node n : this.nn.values()) {
			Link l1 = fac.createLink(Id.createLinkId("jps_in"+id), n, centerN);
			this.q2ext.add(l1.getId());
			Link l2 = fac.createLink(Id.createLinkId("jps_out"+id++), centerN,n);
			l1.setFreespeed(fs);
			l1.setCapacity(cap);
			l1.setLength(length);
			l1.setNumberOfLanes(lanes);
			l2.setFreespeed(fs);
			l2.setCapacity(cap);
			l2.setLength(length);
			l2.setNumberOfLanes(lanes);			
			impl.addLink(l2);
			impl.addLink(l1);
		}
		
		
	}

	private void cleanupNetwork() {
		Set<String> fromTo = new HashSet<>();
		Iterator<?> it = this.sc.getNetwork().getLinks().entrySet().iterator();
		while (it.hasNext()) {
			Entry<Id<Link>, Link> e = (Entry<Id<Link>, Link>) it.next();
			String ft = e.getValue().getFromNode().getId().toString()+"_"+e.getValue().getToNode().getId().toString();
			if (fromTo.contains(ft)){
				this.excl .add(e.getValue().getId());
			} else {
				fromTo.add(ft);
			}
			
		}
		
		
	}

	private void buildNewNetwork() {
		Config c = ConfigUtils.createConfig();
		this.nsc = ScenarioUtils.createScenario(c);
		Network impl = nsc.getNetwork();
		NetworkFactory fac = impl.getFactory();
		
		for (Node n : this.sc.getNetwork().getNodes().values()) {
			Node nn;
			if ((nn=this.nn.get(n)) != null) {
				impl.addNode(nn);
				nn.getOutLinks().clear();
				nn.getInLinks().clear();
			} else {
				impl.addNode(n);
				n.getOutLinks().clear();
				n.getInLinks().clear();
			}
		}
		for (Link l : this.sc.getNetwork().getLinks().values()) {
			if (this.excl.contains(l.getId())) {
				continue;
			}
			Node to;
			if ((to = this.nn.get(l.getToNode())) != null) {
				l.setToNode(to);
			}
			Node from;
			if ((from = this.nn.get(l.getFromNode())) != null) {
				l.setFromNode(from);
			}
			if (l.getCapacity() < 10) {
				l.setCapacity(l.getCapacity()*3600);
			}
			impl.addLink(l);
		}
		
	}

	private void findMatches() {
		this.frId = 0;
		NetworkImpl impl = (NetworkImpl) this.sc.getNetwork();
		NetworkFactoryImpl fac = impl.getFactory();
		for (Transition t : geo.transitions){
			if (!t.room2Id.equals("-1")){
				Coord c = new Coord((t.v1.px + t.v2.px) / 2, (t.v1.py + t.v2.py) / 2);
				Node n = impl.getNearestNode(c);
				double dist = CoordUtils.calcDistance(c, n.getCoord());
				if (dist > EPSILON) {
					log.warn("Node and transition center are rather far away from each other ("+dist +"m) possibly wrong!");
				}
				this.tn.put(t, n);
				this.nt.put(n, t);
				
				if (n.getOutLinks().size() > 1) {
					log.error("more than one outlink for node\n"
							+ n
							+ ", possibly wrong!\n"
							+ t);
				}
				
				for (Link l : n.getOutLinks().values()) {
					this.ext2q.add(l.getId());
				}
				
				NodeImpl nn = fac.createNode(Id.createNodeId(t.room2Id), n.getCoord());
				int id = Integer.parseInt(t.room2Id);
				if (id >= frId) {
					frId = id+1;
				}
				
				this.nn.put(n,nn);
			}
		}
	}
	
}

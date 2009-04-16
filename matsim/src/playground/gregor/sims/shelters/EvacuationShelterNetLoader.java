package playground.gregor.sims.shelters;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkChangeEventsParser;
import org.matsim.core.network.NetworkFactory;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.xml.sax.SAXException;

import playground.gregor.sims.evacbase.EvacuationNetGenerator;

public class EvacuationShelterNetLoader {


	private static final Logger log = Logger.getLogger(EvacuationShelterNetLoader.class);

	private final Config config;
	private final List<Building> buildings;

	private final Set<Link> shelterLinks = new HashSet<Link>();

	private final HashMap<Id,Building> buildingsLinkMapping = new HashMap<Id, Building>();

	private NetworkLayer network = null;

	public EvacuationShelterNetLoader(List<Building> buildings, Config config) {
		this.config = config;
		this.buildings = buildings;
	}

	public NetworkLayer getNetwork() {
		if (this.network != null) {
			return this.network;
		}
		this.network = loadNetwork();

		createEvacuationNet();
		
		generateShelterLinks();
		
		
		return this.network;
	}


	private void generateShelterLinks() {
		Node saveNode = this.network.getNode("en1"); //TODO GL Apr. 09 -- evacuation node should not retrieved via String id
		int count = 0;
		for (Building building : this.buildings) {
			if (!building.isQuakeProof()) {
				continue;
			}
			Coord c = MGC.point2Coord(building.getGeo().getCentroid());
			Node from = this.network.getNearestNode(c);
			Node sn1 = this.network.createNode(new IdImpl("sn" + count + "a"), c);
			Node sn2 = this.network.createNode(new IdImpl("sn" + count + "b"), c);
			Link l1 = this.network.createLink(new IdImpl("sl" + count + "a"), from, sn1, getDist(from,sn1) , 1.66, 1, 1); //FIXME find right values flow cap, lanes, ...
			this.buildingsLinkMapping.put(l1.getId(), building);
			Link l2 = this.network.createLink(new IdImpl("sl" + count + "b"), sn1,sn2, 20 , 20, 1, 1); //FIXME find right values flow cap, lanes, ...
			Link l3 = this.network.createLink(new IdImpl("sl" + count++ + "c"), sn2,saveNode, 10 , 10000, 10000, 1); //FIXME find right values flow cap, lanes, ...
			this.shelterLinks.add(l1);
			this.shelterLinks.add(l2);
			this.shelterLinks.add(l3);
		}
		this.network.connect();
	}
	
	
	public HashMap<Id,Building> getShelterLinkMapping() {
		
		getNetwork(); //make sure that mapping has been created;
		
		return this.buildingsLinkMapping;
	}
	
	public Set<Link> getShelterLinks() {
		if (this.network == null) {
			getNetwork();
		}
		
		return this.shelterLinks;
	}
	
	private double getDist(Node n1, Node n2) {
		return Math.sqrt(Math.pow(n1.getCoord().getX()-n2.getCoord().getX(), 2) + Math.pow(n1.getCoord().getY()-n2.getCoord().getY(), 2));
	}

	private void createEvacuationNet() {
		new EvacuationNetGenerator(this.network,this.config).run();
	}

	private NetworkLayer loadNetwork() {
		log.info("loading network from " + this.config.network().getInputFile());
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkFactory(new TimeVariantLinkFactory());
		NetworkLayer net = new NetworkLayer(nf);

		try {
			new MatsimNetworkReader(net).parse(this.config.network().getInputFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (this.config.network().getChangeEventsInputFile() != null && this.config.network().isTimeVariantNetwork()){
			log.info("loading network change events from " + this.config.network().getChangeEventsInputFile());
			NetworkChangeEventsParser parser = new NetworkChangeEventsParser(net);
			try {
				parser.parse(this.config.network().getChangeEventsInputFile());
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			net.setNetworkChangeEvents(parser.getEvents());
		}

		this.network = net;
		return net;
	}


}

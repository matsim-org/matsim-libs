package playground.gregor.evacuation.lostagents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;

public class LostAgentsAnalyser implements AgentDepartureEventHandler, AgentArrivalEventHandler{

	private static final Logger log = Logger.getLogger(LostAgentsAnalyser.class);

	private final List<MultiPolygon> polygons;
	private final String out;
	private final String eventsFile;
	private QuadTree<PolygonFeature> quad;
	private final Map<Id,PolygonFeature> agentPolygonMapping = new HashMap<Id,PolygonFeature>();
	private final NetworkLayer network;

	public LostAgentsAnalyser(String outputShapeFile,  String events, List<MultiPolygon> polygons, NetworkLayer net) {
		this.polygons = polygons;
		this.eventsFile = events;
		this.out = outputShapeFile;
		this.network = net;
	}

	public void run() {
		buildQuad();

		EventsManagerImpl  ev = new EventsManagerImpl();
		ev.addHandler(this);
		new EventsReaderTXTv1(ev).readFile(this.eventsFile);

		writeFeature();

	}

	private void writeFeature() {

		FeatureType ft = initFeatureType();
		Collection<Feature> fts = new ArrayList<Feature>();
		int depart = 0;
		int lost = 0;
		for (PolygonFeature pf : this.quad.values()) {
			double rate = ((double)pf.agLost/pf.agDepart);
			String label = pf.agDepart +"\n" + pf.agLost;
			lost+=pf.agLost;
			if (pf.agLost > 0) {
				depart+=pf.agDepart;
			}
			try {
				fts.add(ft.create(new Object[]{pf.p,pf.agDepart,pf.agArr,pf.agLost,rate, label}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}
		try {
			ShapeFileWriter.writeGeometries(fts, this.out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("total depart:" + depart + "  total lost:" + lost);
	}

	private FeatureType initFeatureType() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, targetCRS);
		AttributeType agDep = AttributeTypeFactory.newAttributeType("agDep", Integer.class);
		AttributeType agArr = AttributeTypeFactory.newAttributeType("agArr", Integer.class);
		AttributeType agLost = AttributeTypeFactory.newAttributeType("agLost", Integer.class);
		AttributeType agLostRate = AttributeTypeFactory.newAttributeType("agLostRate", Double.class);
//		AttributeType agLostPerc = AttributeTypeFactory.newAttributeType("agLostPerc", Integer.class);
//		AttributeType agLostPercStr = AttributeTypeFactory.newAttributeType("agLostPercStr", String.class);
		AttributeType agLabel = AttributeTypeFactory.newAttributeType("agLabel", String.class);
		Exception ex;
		try {
			return FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, agDep,agArr,agLost,agLostRate,agLabel}, "LostAgents");
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}

	private void buildQuad() {
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (MultiPolygon p : this.polygons) {
			Coordinate c = p.getCentroid().getCoordinate();
			if (c.x > maxX) {
				maxX = c.x;
			}
			if (c.x < minX) {
				minX = c.x;
			}
			if (c.y > maxY) {
				maxY = c.y;
			}
			if (c.y < minY) {
				minY = c.y;
			}
		}

		this.quad = new QuadTree<PolygonFeature>(minX,minY,maxX,maxY);
		for (MultiPolygon p : this.polygons) {
			Coordinate c = p.getCentroid().getCoordinate();
			PolygonFeature pf = new PolygonFeature();
			pf.p = p;
			this.quad.put(c.x, c.y, pf);
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Link link = this.network.getLinks().get(event.getLinkId());
		PolygonFeature pf = this.quad.get(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		if (!pf.p.contains(MGC.coord2Point(link.getToNode().getCoord()))) {
			log.warn("got wrong polygon! check the quad tree! Performing linear search! this will slow done the programm significant!");
			for (PolygonFeature pf2 : this.quad.values()) {
				if (pf2.p.contains(MGC.coord2Point(link.getToNode().getCoord()))) {
					pf = pf2;
					break;
				}
			}
		}
		pf.agDepart++;
		pf.agLost++;
		this.agentPolygonMapping.put(event.getPersonId(), pf);
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		PolygonFeature pf = this.agentPolygonMapping.get(event.getPersonId());
		pf.agArr++;
		pf.agLost--;

	}

	private static class PolygonFeature {
		MultiPolygon p;
		int agDepart = 0;
		int agArr = 0;
		int agLost = 0;
	}



}

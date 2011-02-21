package playground.gregor.evacuation.evactimecompare;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerFactoryImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.gis.helper.GTH;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class EvacTimeCompare implements AgentDepartureEventHandler, AgentArrivalEventHandler{
	private static final double WIDTH = 300;
	private String r1;
	private String r2;
	private QuadTree<MultiPolygon> qt;
	private String shape;
	private HashMap<Id, EvacTimeInfo> etis;
	private Map<MultiPolygon,CellInfo> cis = new HashMap<MultiPolygon, EvacTimeCompare.CellInfo>();
	private Scenario sc;
	
	
	public EvacTimeCompare(String r1, String r2, QuadTree<MultiPolygon> qt,
			String shape, Scenario sc) {
		this.r1 = r1;
		this.r2 = r2;
		this.qt = qt;
		this.shape = shape;
		this.sc = sc;
	}

	public void run() {
		this.etis = new HashMap<Id, EvacTimeCompare.EvacTimeInfo>();
		EventsManager ev = new EventsManagerFactoryImpl().createEventsManager();
		ev.addHandler(this);
		new MatsimEventsReader(ev).readFile(r1);
		HashMap<Id, EvacTimeInfo> etis1 = this.etis;
		this.etis = new HashMap<Id, EvacTimeCompare.EvacTimeInfo>();
		new MatsimEventsReader(ev).readFile(r2);
		HashMap<Id, EvacTimeInfo> etis2 = this.etis;
		
		createCellInfos(etis1,etis2);
		
		createShapeFile();
		
		
	}
	
	private void createShapeFile() {
		FeatureType ft = initFeatureType();
		List<Feature> fts = new ArrayList<Feature>();
		for (Entry<MultiPolygon, CellInfo>  e : this.cis.entrySet()) {
			CellInfo ci = e.getValue();
			String label = (int)(0.5+ci.diffR1R2) + "";
			Feature f;
			try {
				f = ft.create(new Object[]{e.getKey(),ci.depR1,ci.arrR1,ci.lostR1,ci.depR2,ci.arrR2,ci.lostR2,ci.avgR1,ci.avgR2,ci.diffR1R2,ci.diffR2R1,label});
				fts.add(f);
			} catch (IllegalAttributeException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		try {
			ShapeFileWriter.writeGeometries(fts, shape);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
	}

	private FeatureType initFeatureType() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class, true, null, null, targetCRS);
		AttributeType agDepR1 = AttributeTypeFactory.newAttributeType("agDepR1", Integer.class);
		AttributeType agArrR1 = AttributeTypeFactory.newAttributeType("agArrR1", Integer.class);
		AttributeType agLostR1 = AttributeTypeFactory.newAttributeType("agLostR1", Integer.class);
		AttributeType agDepR2 = AttributeTypeFactory.newAttributeType("agDepR2", Integer.class);
		AttributeType agArrR2 = AttributeTypeFactory.newAttributeType("agArrR2", Integer.class);
		AttributeType agLostR2 = AttributeTypeFactory.newAttributeType("agLostR2", Integer.class);
		AttributeType avgR1 = AttributeTypeFactory.newAttributeType("avgR1", Integer.class);
		AttributeType avgR2 = AttributeTypeFactory.newAttributeType("avgR2", Integer.class);
		AttributeType diffR1R2 = AttributeTypeFactory.newAttributeType("diffR1R2", Double.class);
		AttributeType diffR2R1 = AttributeTypeFactory.newAttributeType("diffR2R1", Double.class);
		AttributeType label = AttributeTypeFactory.newAttributeType("label", String.class);
		Exception ex;
		try {
			return FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, agDepR1,agArrR1,agLostR1,agDepR2,agArrR2,agLostR2,avgR1,avgR2,diffR1R2,diffR2R1,label}, "LostAgents");
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);
	}


	private void createCellInfos(HashMap<Id, EvacTimeInfo> etis1,
			HashMap<Id, EvacTimeInfo> etis2) {
		for (EvacTimeInfo eti : etis1.values()) {
			double x = eti.l.getToNode().getCoord().getX();
			double y = eti.l.getToNode().getCoord().getY();
			MultiPolygon mp = this.qt.get(x, y);
			CellInfo ci = this.cis.get(mp);
			if (ci == null) {
				ci = new CellInfo();
				this.cis.put(mp, ci);
			}
			ci.depR1++;
			if (eti.arrive > -1) {
				ci.arrR1++;
				ci.avgR1 += (eti.arrive - eti.depart);
			}
		}

		for (EvacTimeInfo eti : etis2.values()) {
			double x = eti.l.getToNode().getCoord().getX();
			double y = eti.l.getToNode().getCoord().getY();
			MultiPolygon mp = this.qt.get(x, y);
			CellInfo ci = this.cis.get(mp);
//			if (ci == null) {
//				ci = new CellInfo();
//				this.cis.put(mp, ci);
//			}
			ci.depR2++;
			if (eti.arrive > -1) {
				ci.arrR2++;
				ci.avgR2 += (eti.arrive - eti.depart);
			}
		}
		
		for (CellInfo ci : this.cis.values()) {
			ci.avgR1 /= (double)ci.arrR1;
			ci.avgR2 /= (double)ci.arrR2;
			ci.lostR1 = ci.depR1 - ci.arrR1;
			ci.lostR2 = ci.depR2 - ci.arrR2;
			ci.diffR1R2 = ci.avgR1 - ci.avgR2;
			ci.diffR2R1 = ci.avgR2 - ci.avgR2;
		}
		
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		EvacTimeInfo eti = this.etis.get(event.getPersonId());
		eti.arrive = event.getTime();
		
	}
	
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Link l = this.sc.getNetwork().getLinks().get(event.getLinkId());
		EvacTimeInfo eti = new EvacTimeInfo();
		eti.depart = event.getTime();
		eti.l = l;
		this.etis.put(event.getPersonId(), eti);
	}

	
	private static class CellInfo {
		double avgR1;
		double avgR2;
		int depR1;
		int depR2;
		int arrR1;
		int arrR2;
		double diffR1R2;
		double diffR2R1;
		int lostR1;
		int lostR2;
	}
	
	private static class EvacTimeInfo {
		double depart;
		double arrive = -1;
		Link l;
	}
	
	public static void main(String [] args) {
		String svn ="/Users/laemmel/svn/runs-svn/";
		String iter ="/output/ITERS/it.1000/1000.events.txt.gz";
		String iter1 ="/output/ITERS/it.0/0.events.txt.gz";
		String r1 = svn + "run1362" + iter;
		String r2 = svn + "run1363" + iter;
		String net = svn + "run1362" + "/output/output_network.xml.gz";
		String shape = svn + "run1362/analysis/etc1362.it1000Vs1363.it1000.shp"; 
 		Scenario sc = new ScenarioImpl();
 		new MatsimNetworkReader(sc).readFile(net);
 		Envelope e = getEnvelope(sc.getNetwork());
 		QuadTree<MultiPolygon> qt = getMultiPolygons(e);
 		EvacTimeCompare etc = new EvacTimeCompare(r1,r2,qt,shape,sc);
 		etc.run();
 		
	}
	
	private static QuadTree<MultiPolygon> getMultiPolygons(Envelope e) {
		GeometryFactory geofac = new GeometryFactory();
		GTH gth = new GTH(geofac);
		QuadTree<MultiPolygon> ret = new QuadTree<MultiPolygon>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		int countX = 0;
		int countY = 0;
		for (double x = e.getMinX(); x <= e.getMaxX(); x += WIDTH) {
			System.out.println("countX:" + ++countX + "  countY:" + countY);
			for (double y = e.getMinY(); y <= e.getMaxY(); y += WIDTH) {
				countY++;
				Polygon p = gth.getSquare(new Coordinate(x,y),WIDTH);
				MultiPolygon mp = geofac.createMultiPolygon(new Polygon[]{p});
				ret.put(p.getCentroid().getX(), p.getCentroid().getY(), mp);
			}
		}
		return ret;
	}

	private static Envelope getEnvelope(Network net) {
		Envelope e = null; 
		for (Node n : net.getNodes().values()) {
			if (e == null) {
				e = new Envelope(n.getCoord().getX(),n.getCoord().getX(),n.getCoord().getY(),n.getCoord().getY());
			} else {
				e.expandToInclude(n.getCoord().getX(), n.getCoord().getY());
			}
		}		
		return e;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	
}

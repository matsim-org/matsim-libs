package playground.gregor.evacuation.evacuationdirections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.FeatureSource;
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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Point;

public class EvacuationDirections implements LinkEnterEventHandler{
	
	private static final String network = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/gis/network_v20080618/links_v20090728.shp";
	
	private static final double PI_HALF = Math.PI / 2.0;
	private static final double TWO_PI = 2.0 * Math.PI;
	Map<Id,LinkInfo> links = new HashMap<Id,LinkInfo>();
	private final List<String> evs;
	private ArrayList<Feature> features;
	private FeatureType ft;
	private final ScenarioImpl scenario;
	private final String outfile;

	private HashMap<Id, Feature> streetMap;
	
	
	public EvacuationDirections(List<String> evs, ScenarioImpl scenario, String outfile) {
		
		this.evs = evs;
		this.scenario = scenario;
		this.outfile = outfile;
	}

	private void run() throws IOException {
		initFeatures();
		buildStreetMap();
		for (String ef : this.evs) {
			EventsManagerImpl events1 = new EventsManagerImpl();
			events1.addHandler(this);
			new EventsReaderTXTv1(events1).readFile(ef);
		}
		
		for (LinkImpl l : this.scenario.getNetwork().getLinks().values()) {
			LinkInfo li = this.links.get(l.getId());
			if (li == null || l.getId().toString().contains("l")) {
				continue;
			}
			int intId = Integer.parseInt(l.getId().toString());
			if (li.highId > 0 && li.lowId > 0) {
				continue;
			}
			if (li.highId > 0 && intId > 100000) {
					createFeature(l,new IdImpl(intId-100000),li.highId);
			} else if (li.lowId > 0 && intId < 100000) {
					createFeature(l,l.getId(), li.lowId);
			}
		}
		
//		Point p = MGC.coord2Point(new CoordImpl(100,100));
//		try {
//			this.features.add(this.ft.create(new Object[]{p,90,1}));
//		} catch (IllegalAttributeException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		try {
			ShapeFileWriter.writeGeometries(this.features, this.outfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void buildStreetMap() throws IOException {
		this.streetMap = new HashMap<Id,Feature>();
		FeatureSource fts = ShapeFileReader.readDataFile(network);
		Iterator it = fts.getFeatures().iterator();
		while ( it.hasNext()) {
			Feature ft = (Feature) it.next();
			Long intId = (Long) ft.getAttribute("ID");
			Id id = new IdImpl(intId);
			if (this.streetMap.get(id) != null) {
				throw new RuntimeException("Id already exists!");
			}
			this.streetMap.put(id, ft);
		}
	}
	
	
	private void createFeature(LinkImpl l, Id idImpl, int count) {
//				Point p = MGC.coord2Point(l.getCoord());
		Feature ft = this.streetMap.get(idImpl);
		if (ft == null) {
			return;
		}
		Point p = ft.getDefaultGeometry().getCentroid();
		double angle = getAngle(l);
		try {
			this.features.add(this.ft.create(new Object[]{p,angle,count/this.evs.size()}));
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
	}

	private double getAngle(LinkImpl l) {
		final double dx = -l.getFromNode().getCoord().getX()   + l.getToNode().getCoord().getX();
		final double dy = -l.getFromNode().getCoord().getY()   + l.getToNode().getCoord().getY();		
		

		double theta = 0.0;
		if (dx > 0) {
			theta = Math.atan(dy/dx);
		} else if (dx < 0) {
			theta = Math.PI + Math.atan(dy/dx);
		} else { // i.e. DX==0
			if (dy > 0) {
				theta = PI_HALF;
			} else {
				theta = -PI_HALF;
			}
		}
		if (theta < 0.0) theta += TWO_PI;
		double tmp = 360 - theta/TWO_PI * 360 + 90;
		if (tmp > 360) { tmp -= 360; }
		return tmp;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().toString().contains("l")){
			return;
		}
		int intId = Integer.parseInt(event.getLinkId().toString());
		LinkInfo li = this.links.get(event.getLinkId());
		if (li == null) {
			li = new LinkInfo();
			if (intId < 100000) {
				Id id2 =  new IdImpl(intId + 100000);
				li.id = event.getLinkId();
				this.links.put(event.getLinkId(), li);
				this.links.put(id2, li);
			} else {
				Id id2 =  new IdImpl(intId - 100000);
				li.id = id2;
				this.links.put(event.getLinkId(), li);
				this.links.put(id2, li);
			}
		}
		
		if (intId < 100000) {
			li.lowId++;
		} else {
			li.highId++;
		}
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	private void initFeatures() {
		this.features = new ArrayList<Feature>();
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM47S);
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, crs);
		AttributeType angle = AttributeTypeFactory.newAttributeType("angle", Double.class);
		AttributeType user = AttributeTypeFactory.newAttributeType("persons", Integer.class);
		try {
			this.ft = FeatureTypeFactory.newFeatureType(new AttributeType[] {geom, angle, user}, "links");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		

		
	}
	
	private static class LinkInfo {
		Id id;
		int lowId = 0;
		int highId = 0;
	}
	
	public static void main(String [] args) throws IOException {
		
		List<String> evs = new ArrayList<String>();
		evs.add("/home/laemmel/arbeit/svn/runs-svn/run1014/output/ITERS/it.500/500.events.txt.gz");
		evs.add("/home/laemmel/arbeit/svn/runs-svn/run1022/output/ITERS/it.500/500.events.txt.gz");
		evs.add("/home/laemmel/arbeit/svn/runs-svn/run1030/output/ITERS/it.500/500.events.txt.gz");
		
		String network = "/home/laemmel/arbeit/svn/runs-svn/run1014/output/output_network.xml.gz";
		String outfile = "/home/laemmel/arbeit/svn/shared-svn/projects/LastMile/berichte/abschlussworkshop/gis/evac_directions.shp";
		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(network);
		
		new EvacuationDirections(evs, scenario,outfile).run();
	}



}

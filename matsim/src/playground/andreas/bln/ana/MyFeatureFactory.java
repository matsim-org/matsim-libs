package playground.andreas.bln.ana;

import net.opengis.kml._2.AbstractFeatureType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.StyleType;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.kml.NetworkFeatureFactory;

public class MyFeatureFactory extends NetworkFeatureFactory{

	// TODO [AN] visibility DG
	private static final Logger log = Logger.getLogger(NetworkFeatureFactory.class);
	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	private CoordinateTransformation coordTransform;
	
	public MyFeatureFactory(CoordinateTransformation coordTransform) {
		super(coordTransform);
		this.coordTransform = coordTransform;
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public AbstractFeatureType createActFeature(ActivityImpl act, StyleType style) {

		PlacemarkType p = this.kmlObjectFactory.createPlacemarkType();
		p.setName(act.getType().toString());

		Coord coord = this.coordTransform.transform(act.getCoord());
		PointType point = this.kmlObjectFactory.createPointType();
		point.getCoordinates().add(Double.toString(coord.getX()) + "," + Double.toString(coord.getY()) + ",0.0");
		p.setAbstractGeometryGroup(this.kmlObjectFactory.createPoint(point));

		p.setStyleUrl(style.getId());
		return p;
	}

	@Override
	public AbstractFeatureType createLegFeature(LegImpl leg, StyleType style) {
		
		PlacemarkType p = this.kmlObjectFactory.createPlacemarkType();
		p.setName(leg.getMode().toString());
		
//		FolderType folder = this.kmlObjectFactory.createFolderType();
//		folder.setName(leg.getMode().toString() + "_" + Time.writeTime(leg.getDepartureTime()));
//
//		for (Link l : ((NetworkRouteWRefs) leg.getRoute()).getLinks()) {
//
//			AbstractFeatureType abstractFeature = this.createLinkFeature(l, style);
//			if (abstractFeature.getClass().equals(FolderType.class)) {
//				folder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder((FolderType) abstractFeature));
//			} else {
//				log.warn("Not yet implemented: Adding link KML features of type" + abstractFeature.getClass());
//			}
//		}
//		for (Node n : ((NetworkRouteWRefs) leg.getRoute()).getNodes()) {
//			
//			AbstractFeatureType abstractFeature = this.createNodeFeature(n, style);
//			if (abstractFeature.getClass().equals(PlacemarkType.class)) {
//				folder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark((PlacemarkType) abstractFeature));
//			} else {
//				log.warn("Not yet implemented: Adding node KML features of type" + abstractFeature.getClass());
//			}
//		}
		
		return p;
	}
}

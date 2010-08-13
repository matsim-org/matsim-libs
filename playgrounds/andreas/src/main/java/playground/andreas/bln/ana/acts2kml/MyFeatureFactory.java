package playground.andreas.bln.ana.acts2kml;

import net.opengis.kml._2.AbstractFeatureType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.StyleType;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.kml.NetworkFeatureFactory;

public class MyFeatureFactory extends NetworkFeatureFactory{

	// TODO [AN] visibility DG
	private static final String STARTUL = "<ul>";
	private static final String ENDUL = "</ul>";
	private static final String STARTLI = "<li>";
	private static final String ENDLI = "</li>";

	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	private CoordinateTransformation coordTransform;

	private MyFeatureFactory(CoordinateTransformation coordTransform, Network network) {
		super(coordTransform, network);
		this.coordTransform = coordTransform;
	}
	
	public MyFeatureFactory(CoordinateTransformation coordTransform) {
		this(coordTransform, null);
	}


	@Override
	public AbstractFeatureType createActFeature(Activity act, StyleType style) {

		PlacemarkType p = this.kmlObjectFactory.createPlacemarkType();
		p.setName(act.getType() + " act");
		p.setDescription(this.createActDescription(act));
		Coord coord = this.coordTransform.transform(act.getCoord());
		PointType point = this.kmlObjectFactory.createPointType();
		point.getCoordinates().add(Double.toString(coord.getX()) + "," + Double.toString(coord.getY()) + ",0.0");
		p.setAbstractGeometryGroup(this.kmlObjectFactory.createPoint(point));

		p.setStyleUrl(style.getId());
		return p;
	}	

	private String createActDescription(Activity act) {
		StringBuilder buffer = new StringBuilder(100);
//		buffer.append(NetworkFeatureFactory.STARTCDATA);
//		buffer.append(NetworkFeatureFactory.STARTH2);
		buffer.append("Link: " );
		buffer.append(act.getLinkId());
//		buffer.append(NetworkFeatureFactory.ENDH2);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(NetworkFeatureFactory.ENDP);

		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append("Attributes: ");
		buffer.append(NetworkFeatureFactory.ENDH3);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(STARTUL);
		buffer.append(STARTLI);
		buffer.append("StartTime: ");
		buffer.append(Time.writeTime(act.getStartTime()));
		buffer.append(ENDLI);
//		buffer.append(STARTLI);
//		buffer.append("Duration: ");
//		buffer.append(Time.writeTime(act.getDuration()));
//		buffer.append(ENDLI);
		buffer.append(STARTLI);
		buffer.append("EndTime: ");
		buffer.append(Time.writeTime(act.getEndTime()));
		buffer.append(ENDLI);
		buffer.append(STARTLI);
		buffer.append("X: ");
		buffer.append(act.getCoord().getX());
		buffer.append(ENDLI);
		buffer.append(STARTLI);
		buffer.append("Y: ");
		buffer.append(act.getCoord().getY());
		buffer.append(ENDLI);
		buffer.append(ENDUL);
		buffer.append(NetworkFeatureFactory.ENDP);

//		buffer.append(NetworkFeatureFactory.ENDCDATA);

		return buffer.toString();
	}
}

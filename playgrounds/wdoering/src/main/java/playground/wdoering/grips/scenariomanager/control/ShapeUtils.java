package playground.wdoering.grips.scenariomanager.control;

import java.awt.geom.Point2D;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.grips.algorithms.PolygonalCircleApproximation;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import playground.wdoering.grips.scenariomanager.model.shape.CircleShape;
import playground.wdoering.grips.scenariomanager.model.shape.PolygonShape;

public class ShapeUtils
{
	private Controller controller;
	private static final GeometryFactory geofac = new GeometryFactory();
	private static final double INCR = Math.PI / 16;

	public ShapeUtils(Controller controller)
	{
		this.controller = controller;
	}

	public PolygonShape getPolygonFromCircle(CircleShape circle)
	{
		// copy circle data to polygon data
		PolygonShape polygon = new PolygonShape(circle.getLayerID(), null);
		polygon.setDescription(circle.getDescription());
		polygon.setId(circle.getId());
		polygon.setMetaData(circle.getAllMetaData());
		
		polygon.setStyle(circle.getStyle());

		CoordinateReferenceSystem sourceCRS = MGC.getCRS(controller.getSourceCoordinateSystem());
		CoordinateReferenceSystem targetCRS = MGC.getCRS(controller.getConfigCoordinateSystem());

		MathTransform transform = null;
		try
		{
			transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
		}
		catch (FactoryException e)
		{
			throw new RuntimeException(e);
		}
		
		Point2D c0 = circle.getOrigin();
		Point2D c1 = circle.getDestination();
		
//		System.out.println("c0:" + c0);
//		System.out.println("c1:" + c1);
		
		Coordinate coord0 = new Coordinate(c0.getY(), c0.getX());
		Coordinate coord1 = new Coordinate(c1.getY(), c1.getX());
		PolygonalCircleApproximation.transform(coord0, transform);
		PolygonalCircleApproximation.transform(coord1, transform);

		Polygon poly = PolygonalCircleApproximation.getPolygonFromGeoCoords(coord0, coord1);

		try
		{
			poly = (Polygon) PolygonalCircleApproximation.transform(poly, transform.inverse());
		}
		catch (NoninvertibleTransformException e)
		{
			e.printStackTrace();
		}

		polygon.setPolygon(poly);

		return polygon;
	}

}

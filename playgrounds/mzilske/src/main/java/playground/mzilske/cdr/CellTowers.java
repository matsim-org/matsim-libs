package playground.mzilske.cdr;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.shapefile.ShpFiles;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileException;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.shapefile.shp.ShapefileWriter;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

import d4d.RunScenario;

public class CellTowers {
	

	Map<String,CellTower> cellTowers = new HashMap<String, CellTower>();
	
	private Map<Coordinate, Geometry> siteToCell;

	private GeometryCollection clippedCells;

	private MultiPoint sites;
	

	GeometryFactory gf = new GeometryFactory();


	public CellTowers(Map<String, CellTower> cellTowerMap) {
		this.cellTowers.putAll(cellTowerMap);
		buildCells();
	}

	private void voronoiStatistics() {
		for (CellTower cellTower : cellTowers.values()) {
			Geometry cell = getCell(cellTower.id);
			System.out.println(cell.getArea());
			Coordinate[] nearestPoints = new DistanceOp(gf.createPoint(coordinate(cellTower)), sites).nearestPoints();
			System.out.println(cellTower.coord + " --- " + nearestPoints[0] + " --- " + nearestPoints[1]);
		}

	}
	
	private Coordinate coordinate(CellTower cellTower) {
		return new Coordinate(cellTower.coord.getX(), cellTower.coord.getY());
	}
	

	
	public Geometry getCell(String cellTowerId) {
		return siteToCell.get(coordinate(cellTowers.get(cellTowerId)));
	}
	

	private Geometry getIvoryCoast() {
		try {
			ShapefileReader r = new ShapefileReader(new ShpFiles("/Users/zilske/d4d/10m-admin-0-countries/ci.shp"), true, true);
			Geometry shape = (Geometry)r.nextRecord().shape(); // do stuff 
			r.close();
			return JTS.transform(shape, CRS.findMathTransform(MGC.getCRS(TransformationFactory.WGS84), MGC.getCRS("EPSG:3395"),true)) ;
		} catch (ShapefileException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (MismatchedDimensionException e) {
			throw new RuntimeException(e);
		} catch (TransformException e) {
			throw new RuntimeException(e);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void getLinksCrossingCells(Network network) {
		Set<Link> linksCrossingCells = new HashSet<Link>();
		PreparedGeometry preparedCells = PreparedGeometryFactory.prepare(clippedCells.getBoundary());
		for (Link link : network.getLinks().values()) {
			if (preparedCells.intersects(gf.createLineString(new Coordinate[]{
					new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()), 
					new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY())
			}))) {
				linksCrossingCells.add(link);
			}
			System.out.println("Links crossing cells: " + linksCrossingCells.size() + " of " + network.getLinks().size());
		}
	}
	
	private void buildCells() {
		List<Polygon> unrolledCells = new ArrayList<Polygon>();

		VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
		Collection<Point> coords = new ArrayList<Point>();
		for (CellTower cellTower : cellTowers.values()) {
			coords.add(gf.createPoint(coordinate(cellTower)));
		}
		sites = new MultiPoint(coords.toArray(new Point[]{}), gf);
		vdb.setSites(sites);
		Geometry ivoryCoast = getIvoryCoast();
		vdb.setClipEnvelope(ivoryCoast.getEnvelopeInternal());
		GeometryCollection diagram = (GeometryCollection) vdb.getDiagram(gf);
		siteToCell = new HashMap<Coordinate, Geometry>();
		for (int i=0; i<diagram.getNumGeometries(); i++) {
			Polygon cell = (Polygon) diagram.getGeometryN(i);
			Coordinate siteCoordinate = (Coordinate) cell.getUserData();
			Geometry clippedCell = cell.intersection(ivoryCoast);
			siteToCell.put(siteCoordinate, clippedCell); // user data of the cell is set to the Coordinate of the site by VoronoiDiagramBuilder
			if (clippedCell instanceof GeometryCollection) {
				GeometryCollection multiCell = (GeometryCollection) clippedCell;
				for (int j=0; j<multiCell.getNumGeometries(); j++) {
					Polygon cellPart = (Polygon) multiCell.getGeometryN(j);
					unrolledCells.add(cellPart);
				}
				System.out.println("Added multicell with " +multiCell.getNumGeometries()+ " parts.");
			} else {
				unrolledCells.add((Polygon) clippedCell);
			}
		}
		clippedCells = new MultiPolygon(unrolledCells.toArray(new Polygon[]{}),gf);
		writeToShapefile(clippedCells, "clipped");
		writeToShapefile(diagram, "unclipped");
		voronoiStatistics();
	}


	private void writeToShapefile(GeometryCollection clippedSites, String baseFilename) {
		FileOutputStream shp = null;
		FileOutputStream shx = null;
		try {
			shp = new FileOutputStream(baseFilename+".shp");
			shx = new FileOutputStream(baseFilename+".shx");
			ShapefileWriter writer = new ShapefileWriter( shp.getChannel(),shx.getChannel());
			writer.write(clippedSites, ShapeType.POLYGON);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				shp.close();
				shx.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	
}

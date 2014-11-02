package playground.mzilske.cdr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.mzilske.cdr.ZoneTracker.Zone;
import playground.mzilske.util.ReadableQuadTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

public class Zones {

	public Map<String, CellTower> cellTowers = new HashMap<String, CellTower>();
	
	public ReadableQuadTree<CellTower> quadTree;
	
	private Map<Coordinate, Geometry> siteToCell;


	private MultiPoint sites;


	GeometryFactory gf = new GeometryFactory();

	private List<Polygon> unrolledCells;


	public Zones(Map<String, CellTower> cellTowerMap) {
		this.cellTowers.putAll(cellTowerMap);
		Map<CellTower, Coord> coords = new HashMap<CellTower, Coord>();
		for (CellTower cellTower : cellTowerMap.values()) {
			coords.put(cellTower, cellTower.coord);
		}
		quadTree = new ReadableQuadTree<CellTower>(coords);
		buildCells();
	}

	private void voronoiStatistics() {
		for (CellTower cellTower : cellTowers.values()) {
			Geometry cell = getCell(cellTower.id);
			System.out.println(cell.getArea());
//			Coordinate[] nearestPoints = new DistanceOp(gf.createPoint(coordinate(cellTower)), sites).nearestPoints();
//			System.out.println(cellTower.coord + " --- " + nearestPoints[0] + " --- " + nearestPoints[1]);
		}

	}

	private Coordinate coordinate(CellTower cellTower) {
		return new Coordinate(cellTower.coord.getX(), cellTower.coord.getY());
	}



	public Geometry getCell(String cellTowerId) {
		return siteToCell.get(coordinate(cellTowers.get(cellTowerId)));
	}


//	private Geometry getIvoryCoast() {
//		try {
//			ShapefileReader r = new ShapefileReader(new ShpFiles("/Users/zilske/d4d/10m-admin-0-countries/ci.shp"), true, true);
//			Geometry shape = (Geometry)r.nextRecord().shape(); // do stuff 
//			r.close();
//			return JTS.transform(shape, CRS.findMathTransform(MGC.getCRS(TransformationFactory.WGS84), MGC.getCRS("EPSG:3395"),true)) ;
//		} catch (ShapefileException e) {
//			throw new RuntimeException(e);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		} catch (MismatchedDimensionException e) {
//			throw new RuntimeException(e);
//		} catch (TransformException e) {
//			throw new RuntimeException(e);
//		} catch (FactoryException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	private void getLinksCrossingCells(Network network) {
//
//		GeometryCollection clippedCells = new MultiPolygon(unrolledCells.toArray(new Polygon[]{}),gf);
//		Set<Link> linksCrossingCells = new HashSet<Link>();
//		PreparedGeometry preparedCells = PreparedGeometryFactory.prepare(clippedCells.getBoundary());
//		for (Link link : network.getLinks().values()) {
//			if (preparedCells.intersects(gf.createLineString(new Coordinate[]{
//					new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()), 
//					new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY())
//			}))) {
//				linksCrossingCells.add(link);
//			}
//			System.out.println("Links crossing cells: " + linksCrossingCells.size() + " of " + network.getLinks().size());
//		}
//	}

	public void buildCells() {
		
	//	contractCells();

		unrolledCells = new ArrayList<Polygon>();

		VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
		Collection<Point> coords = new ArrayList<Point>();
		for (CellTower cellTower : cellTowers.values()) {
//			if (cellTower.nSightings == 0) {
//				throw new RuntimeException();
//			}
			coords.add(gf.createPoint(coordinate(cellTower)));
		}
		sites = new MultiPoint(coords.toArray(new Point[]{}), gf);
		vdb.setSites(sites);
		// Geometry ivoryCoast = getIvoryCoast();
		// vdb.setClipEnvelope(ivoryCoast.getEnvelopeInternal());
		GeometryCollection diagram = (GeometryCollection) vdb.getDiagram(gf);
		siteToCell = new HashMap<Coordinate, Geometry>();
		for (int i=0; i<diagram.getNumGeometries(); i++) {
			Polygon cell = (Polygon) diagram.getGeometryN(i);
			Coordinate siteCoordinate = (Coordinate) cell.getUserData();
			// Geometry clippedCell = cell.intersection(ivoryCoast);
			Geometry clippedCell = cell; // don't need to clip right now.
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
		for (CellTower cellTower : cellTowers.values()) {
			cellTower.cell = getCell(cellTower.id);
		}
		//		writeToShapefile(clippedCells, D4DConsts.WORK_DIR + "clipped");
		//		writeToShapefile(diagram, D4DConsts.WORK_DIR + "unclipped");
		// writeToShapefile();
		voronoiStatistics();
	}

	private void contractCells() {
		//This is just a shortcut which only works because of nSightings == 0
		// might want to contract by another criterion, in which case we would have to
		// "remap" the deleted cellTower to its nearest neighbor (and possibly move it)
		// --> delaunay contraction
		List<String> cellsWithoutCalls  = new ArrayList<String>();
		for (Entry<String, CellTower> entry : cellTowers.entrySet()) {
			if (entry.getValue().nSightings == 0) {
				cellsWithoutCalls.add(entry.getKey());
			}
		}
		for(String cellWithoutCalls : cellsWithoutCalls) {
			System.out.println(cellWithoutCalls + " ///");
			cellTowers.remove(cellWithoutCalls);
		}
	}

	public Id<Zone> locate(Coord coord) {
//		double dist = Double.POSITIVE_INFINITY;
//		CellTower shortest = null;
//		for (CellTower cellTower : cellTowers.values()) {
//			double thisDist = CoordUtils.calcDistance(coord, cellTower.coord);
//			if (thisDist < dist) {
//				shortest = cellTower;
//				dist = thisDist;
//			}
//		}
		
		CellTower shortest = quadTree.get(coord.getX(), coord.getY());
		
		if (shortest == null) {
			throw new RuntimeException();
		}
		return Id.create(shortest.id, Zone.class);
	}


}



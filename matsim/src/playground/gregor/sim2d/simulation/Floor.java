package playground.gregor.sim2d.simulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.sim2d.simulation.Agent2D.AgentState;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.operation.distance.DistanceOp;

public class Floor {

	private static final double STATIC_FORCE_RESOLUTION = 0.05;

	//	private static final double TIME_RES = 0

	private static final double Bp = 1;
	private static final double App = 1000.;
	private static final double Apw = 3000.;
	private static final Logger log = Logger.getLogger(Floor.class);
	private final MultiPolygon structure;
	private final NetworkLayer graph;
	private final Set<Agent2D> agents = new HashSet<Agent2D>();
	private final Map<Agent2D,Force> agentForceMapping = new HashMap<Agent2D, Force>();
	private boolean intialized = false;
	private QuadTree<Force> staticForceQuadTree;

	private FeatureType ft;

	private FeatureType ftLine;

	private FeatureType ftPoint;

	double tau = 2;
	private List<double[]> forceInfos;

	private final Force nullForce = new Force();

	public Floor(MultiPolygon structure, NetworkLayer subnet) {
		this.structure = structure;
		this.graph = subnet;
	}

	public void move() {
		if (!this.intialized) {
			init();
		}
		updateForces();
		moveAgents();
	}

	private void moveAgents() {
		for (Agent2D agent : this.agents) {
			Force force = this.agentForceMapping.get(agent);
			Coordinate oldPos = agent.getPosition();
			agent.setPosition(oldPos.x+force.x,oldPos.y+force.y);
		}

	}

	private void updateForces() {

		//DEBUG
		this.forceInfos = new ArrayList<double[]>();


		for (Agent2D agent : this.agents) {
			Force force = this.agentForceMapping.get(agent);
			force.dynX = 0;
			force.dynY = 0;
			for (Agent2D other : this.agents) {
				if (other.equals(agent)) {
					continue;
				}
				double x = agent.getPosition().x - other.getPosition().x;
				double y = agent.getPosition().y - other.getPosition().y;
				double length = Math.sqrt(Math.pow(x,2)+Math.pow(y,2 ));
				if (length == 0 || length > Bp) {
					continue;
				}
				double exp = Math.exp(length/Bp);
				x *= exp/length;
				y *= exp/length;
				force.dynX += x;
				force.dynY += y;
			}
			double [] tmp4 = {agent.getPosition().x,agent.getPosition().y,App * force.dynX/agent.getWeight(),App * force.dynY/agent.getWeight(),3.f};
			this.forceInfos.add(tmp4);
		}
		for (Agent2D agent : this.agents) {		
			Force force = this.agentForceMapping.get(agent);
			if (agent.getState() == AgentState.ACTING) {
				//				Force force = this.agentForceMapping.get(agent);
				force.x = 0.;
				force.y = 0.;
			}
			else if  (agent.getState() == AgentState.MOVING) {
				updateAgentForce(agent);
			}
		}
	}

	private void updateAgentForce(Agent2D agent) {
		Link link = agent.getCurrentLink();
		Force force = this.agentForceMapping.get(agent);
		if (agent.getPosition().distance(MGC.coord2Coordinate(link.getToNode().getCoord())) < 0.1) {
			link = agent.chooseNextLink();
		}
		if (link == null) {
			force.x = 0;
			force.y = 0;
		} else {
			Coordinate dest = MGC.coord2Coordinate(link.getToNode().getCoord());
			dest.x -= agent.getPosition().x;
			dest.y -= agent.getPosition().y;
			double norm = Math.sqrt(dest.x * dest.x + dest.y * dest.y);
			double scale = agent.getDisiredVelocity()/norm;
			if (scale > 1) {
				agent.chooseNextLink();
			}
			Force desired = new Force();
			desired.x = dest.x * scale;
			desired.y = dest.y * scale;

			double [] tmp1 = {agent.getPosition().x,agent.getPosition().y,force.x, force.y,0.f};
			Force staticForce = null;
			if(this.staticForceQuadTree.get(agent.getPosition().x, agent.getPosition().y,Bp).size() == 0){
				staticForce = this.nullForce;
			} else {
				staticForce  = this.staticForceQuadTree.get(agent.getPosition().x, agent.getPosition().y);
			}

			
			force.x += (desired.x - force.x)/this.tau + Apw * staticForce.x/agent.getWeight() + Apw * force.dynX/agent.getWeight();
			force.y += (desired.y - force.y)/this.tau + App * staticForce.y/agent.getWeight()+ App * force.dynY/agent.getWeight();

			double [] tmp2 = {agent.getPosition().x,agent.getPosition().y,(desired.x - force.x)/this.tau,(desired.y - force.y)/this.tau,1.f};
			double [] tmp3 = {agent.getPosition().x,agent.getPosition().y,Apw* staticForce.x/agent.getWeight(),Apw* staticForce.y/agent.getWeight(),1.99f};

			this.forceInfos.add(tmp1);
			this.forceInfos.add(tmp2);
			this.forceInfos.add(tmp3);


			double norm2= Math.sqrt(Math.pow(force.x,2)  + Math.pow(force.y,2));
			if (norm2 > agent.getDisiredVelocity()) {
				force.x *= agent.getDisiredVelocity()/norm2;
				force.y *= agent.getDisiredVelocity()/norm2;
			}
		}

	}

	public void addAgent(Agent2D agent) {
		this.agents .add(agent);
		Force force = new Force();
		this.agentForceMapping.put(agent, force);
	}

	public Set<Agent2D> getAgents() {
		return this.agents;
	}

	private void init() {
		calcStaticForces();
		this.intialized = true;
	}

	private void calcStaticForces() {
		Geometry geo = this.structure.getEnvelope();
		Envelope e = new Envelope();
		for (Coordinate c : geo.getCoordinates()) {
			e.expandToInclude(c);
		}

		List<Coordinate []> debug = new ArrayList<Coordinate []>();
		GeometryFactory geofac = new GeometryFactory();
		this.staticForceQuadTree = new QuadTree<Force>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		int numOfGeos = 0;
		for (double x = e.getMinX(); x <= e.getMaxX(); x += STATIC_FORCE_RESOLUTION) {
			for (double y = e.getMinY(); y <= e.getMaxY(); y += STATIC_FORCE_RESOLUTION) {
				//				this.structure.distance(g)
				DistanceOp op = new DistanceOp(this.structure,geofac.createPoint(new Coordinate(x,y)));
				Coordinate[] coords = op.closestPoints();
				Force force = new Force();
				force.x = coords[0].x - coords[1].x;
				force.y = coords[0].y - coords[1].y;
				if (force.x == 0 && force.y == 0) {
					continue;
				}
				double length = Math.sqrt(Math.pow(force.x,2)+Math.pow(force.y,2 ));
				if (length >Bp) {
					continue;
				} else {
					double exp = Math.exp(length/Bp);
					force.x *= -exp/length;
					force.y *= -exp/length;
				}
				this.staticForceQuadTree.put(x, y, force);
				//				double length2 = Math.sqrt(Math.pow(force.x,2)+Math.pow(force.y,2 ));
				//				if (length2 > 1.0001 || length2 < 0.99999) 	{
				//					return;
				//				}
				//				coords[0].x = coords[1].x +force.x;///length;
				//				coords[0].y = coords[1].y + force.y;///length;

				//								Coordinate [] dbCoords = { coords[1], new Coordinate((coords[1].x+force.x),(coords[1].y+force.y)),new Coordinate((coords[1].x+force.x+0.01),(coords[1].y+force.y+0.01))}; 
				//								debug.add(dbCoords);
				numOfGeos++;
			}
		}
		log.info("static forces caclulated. Number of force points: " + numOfGeos );
		//				debug(debug);
	}

	private void debug(List<Coordinate[] > coords) {
		initFeatures();		
		List<Feature> fts = new ArrayList<Feature>();
		GeometryFactory geofac = new GeometryFactory();
		for (Coordinate [] coord : coords) {
			LineString ls = geofac.createLineString(coord);
			try {
				fts.add(this.ftLine.create(new Object [] {ls,0,0}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}

		try {
			ShapeFileWriter.writeGeometries(fts, "../../tmp/staticForces.shp");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, targetCRS);
		AttributeType l = DefaultAttributeTypeFactory.newAttributeType(
				"LineString", LineString.class, true, null, null, targetCRS);
		AttributeType po = DefaultAttributeTypeFactory.newAttributeType(
				"Point", Point.class, true, null, null, targetCRS);
		AttributeType z = AttributeTypeFactory.newAttributeType(
				"dblAvgZ", Double.class);
		AttributeType t = AttributeTypeFactory.newAttributeType(
				"dblAvgT", Double.class);

		Exception ex;
		try {
			this.ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, z, t }, "Polygon");
			this.ftLine = FeatureTypeFactory.newFeatureType(new AttributeType[] { l, z, t }, "Line");
			this.ftPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] { po, z, t }, "Point");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);

	}

	private static class Force {
		double x = 0;
		double y = 0;
		double dynX = 0;
		double dynY = 0;
	}
	//	private static class AgentForce {
	//		Force velocity;
	//	}

	//DEBUG
	public List<double[]> getForceInfos() {
		return this.forceInfos ;
	}

	public double getAgentVelocity(Agent2D agent) {
		Force force = this.agentForceMapping.get(agent);
		
		return Math.sqrt(Math.pow(force.x, 2)+Math.pow(force.x, 2));
	}
}

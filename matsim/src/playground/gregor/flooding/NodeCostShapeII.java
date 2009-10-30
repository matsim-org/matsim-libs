package playground.gregor.flooding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.evacuation.flooding.NodeRiskCostsFromNetcdf;
import org.matsim.evacuation.riskaversion.RiskCostCalculator;
import org.matsim.evacuation.riskaversion.RiskCostFromFloodingData;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import com.vividsolutions.jts.geom.Point;

public class NodeCostShapeII {

	private final String netcdf;
	private final NetworkLayer network;
	private final String output;
	private FeatureType ftPoint;
	private CoordinateReferenceSystem targetCRS;

	public NodeCostShapeII(String netcdf, String output, NetworkLayer network) {
		this.netcdf = netcdf;
		this.network = network;
		this.output = output;
	}

	public void run() {
		initFeatures();
		this.targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM47S);

		FloodingReader fr = new FloodingReader(this.netcdf);
		fr.setReadTriangles(true);
		List<FloodingReader> frs = new ArrayList<FloodingReader>();
		frs.add(fr);
		NodeRiskCostsFromNetcdf nc = new NodeRiskCostsFromNetcdf(frs, 250.);
		Collection<Feature> fts = new ArrayList<Feature>();
		for (NodeImpl n : this.network.getNodes().values()) {
			double cost = nc.getNodeRiskCost(n);
			if (cost == 0) {
				continue;
			}
			Point p = MGC.coord2Point(n.getCoord());
			try {
				fts.add(this.ftPoint.create(new Object[] { p, cost }));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}
		try {
			ShapeFileWriter.writeGeometries(fts, this.output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initFeatures() {
		AttributeType point = DefaultAttributeTypeFactory.newAttributeType(
				"Point", Point.class, true, null, null, this.targetCRS);
		AttributeType dblCost = AttributeTypeFactory.newAttributeType(
				"dblCost", Double.class);

		Exception ex;
		try {
			this.ftPoint = FeatureTypeFactory.newFeatureType(
					new AttributeType[] { point, dblCost }, "Point");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);

	}

	public static void main(String[] args) {
		String netcdf = "../../inputs/flooding/flooding_old.sww";
		String net = "../../inputs/networks/padang_net_evac_v20080618.xml";
		String output = "./plans/nodeCosts.shp";
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(net);
		new NodeCostShapeII(netcdf, output, network).run();
	}

}

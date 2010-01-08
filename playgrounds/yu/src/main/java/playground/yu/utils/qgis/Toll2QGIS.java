/**
 * 
 */
package playground.yu.utils.qgis;

import java.io.IOException;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author yu
 * 
 */
public class Toll2QGIS extends MATSimNet2QGIS {
	public Toll2QGIS(String netFilename, String coordRefSys) {
		super(netFilename, coordRefSys);
	}

	public static class Toll2PolygonGraph extends Network2PolygonGraph {
		private RoadPricingScheme toll;

		public Toll2PolygonGraph(NetworkLayer network,
				CoordinateReferenceSystem crs, RoadPricingScheme toll) {
			super(network, crs);
			this.toll = toll;
		}

		@Override
		public Collection<Feature> getFeatures() throws SchemaException,
				NumberFormatException, IllegalAttributeException {
			for (int i = 0; i < attrTypes.size(); i++)
				defaultFeatureTypeFactory.addType(attrTypes.get(i));
			FeatureType ftRoad = defaultFeatureTypeFactory.getFeatureType();

			for (Link link : toll.getLinks()) {
				// if (link != null) {
				LinearRing lr = getLinearRing(link);
				Polygon p = new Polygon(lr, null, this.geofac);
				MultiPolygon mp = new MultiPolygon(new Polygon[] { p },
						this.geofac);
				int size = 8 + parameters.size();
				Object[] o = new Object[size];
				o[0] = mp;
				o[1] = link.getId().toString();
				o[2] = link.getFromNode().getId().toString();
				o[3] = link.getToNode().getId().toString();
				o[4] = link.getLength();
				o[5] = link
						.getCapacity(org.matsim.core.utils.misc.Time.UNDEFINED_TIME)
						/ network.getCapacityPeriod() * 3600.0;
				o[6] = (((LinkImpl) link).getType() != null) ? Integer
						.parseInt(((LinkImpl) link).getType()) : 0;
				o[7] = link.getFreespeed(0);
				for (int i = 0; i < parameters.size(); i++) {
					o[i + 8] = parameters.get(i).get(link.getId());
				}
				// parameters.get(link.getId().toString()) }
				Feature ft = ftRoad.create(o, "network");
				features.add(ft);
				// }
			}
			return features;
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// String netFilename = "../matsimTests/scoringTest/network.xml";
		String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";

		Toll2QGIS t2q = new Toll2QGIS(netFilename, ch1903);

		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(t2q
				.getNetwork());
		try {
			tollReader.parse(tollFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		t2q.setN2g(new Toll2PolygonGraph(t2q.getNetwork(), t2q.crs, tollReader
				.getScheme()));
		t2q.writeShapeFile("../matsimTests/toll/ivtch-osm_toll.shp");
	}
}

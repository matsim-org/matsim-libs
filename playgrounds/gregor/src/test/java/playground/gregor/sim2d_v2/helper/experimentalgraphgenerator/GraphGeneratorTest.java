package playground.gregor.sim2d_v2.helper.experimentalgraphgenerator;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;


public class GraphGeneratorTest {




	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testGraphGenerator() {


		String refNetworkFile =  this.utils.getInputDirectory() + "/network.xml";
		String testNetworkFile = this.utils.getOutputDirectory() + "/network.xml";

		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);

		GeometryFactory geofac = new GeometryFactory();
		//line string
		LineString ls1 = geofac.createLineString(new Coordinate[]{new Coordinate(5.2,4.01,0),new Coordinate(0.1,4,0),new Coordinate(0,0,0),new Coordinate(2.1,-.1,0)});
		//linear ring
		LineString ls2 = geofac.createLineString(new Coordinate[]{new Coordinate(5.1,0.1),new Coordinate(8.05,-.1),new Coordinate(8,4.1),new Coordinate(5.11,4),new Coordinate(5,3.1),new Coordinate(7.1,3.2),new Coordinate(6.9,1),new Coordinate(4.9,1.1),new Coordinate(5.1,0.11)});

		Collection<Geometry> geos = new ArrayList<Geometry>();
		geos.add(ls1);
		geos.add(ls2);
		Envelope e = new Envelope(new Coordinate(0,0), new Coordinate(9,5));


		new GraphGenerator(sc,geos,e).run();


		new NetworkWriter(sc.getNetwork()).write(testNetworkFile);


		Assert.assertEquals("Chechking numnber of links:",CRCChecksum.getCRCFromFile(refNetworkFile),CRCChecksum.getCRCFromFile(testNetworkFile));

	}
}

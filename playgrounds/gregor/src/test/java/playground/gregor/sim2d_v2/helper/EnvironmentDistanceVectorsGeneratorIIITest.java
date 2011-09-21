package playground.gregor.sim2d_v2.helper;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.io.EnvironmentDistancesWriter;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

public class EnvironmentDistanceVectorsGeneratorIIITest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testEnvironmentDistanceVectorsGeneratorIII() {
		String testEnvDists = this.utils.getOutputDirectory() + "staticEnvDistances.xml.gz";
		String refEnvDists = this.utils.getInputDirectory() + "staticEnvDistances.xml.gz";

		Config c = ConfigUtils.createConfig();
		Sim2DConfigGroup s = new Sim2DConfigGroup();
		c.addModule("sim2d", s);
		s.setFloorShapeFile(this.utils.getInputDirectory() +"/floorplan.shp");

		EnvironmentDistanceVectorsGeneratorIII gen = new EnvironmentDistanceVectorsGeneratorIII(c);
		gen.setResolution(1);
		gen.setIncr(2*Math.PI/8);
		StaticEnvironmentDistancesField fl = gen.generate();
		new EnvironmentDistancesWriter().write(testEnvDists, fl);

		Assert.assertEquals(CRCChecksum.getCRCFromFile(refEnvDists), CRCChecksum.getCRCFromFile(testEnvDists));
	}
}

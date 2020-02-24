package org.matsim.core.config;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

public class MaterializeConfigTest {

	private static final Logger log = Logger.getLogger(MaterializeConfigTest.class);
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testMaterializeAfterReadParameterSets() {
		{
			// generate a test config that sets two values away from their defaults, and
			// write it to file:
			Config config = ConfigUtils.createConfig();
			TestConfigGroup configGroup = ConfigUtils.addOrGetModule(config,
					TestConfigGroup.class);
			for (int i=0; i < 2; i++) {
				TestParameterSet set = (TestParameterSet) configGroup.createParameterSet("blabla");
				set.setParameter("life is wonderful");
			}

			ConfigUtils.writeConfig(config, utils.getOutputDirectory() + "ad-hoc-config.xml");
		}

		{
			// load config file without materializing the config group
			Config config = ConfigUtils.loadConfig(new String[] { utils.getOutputDirectory() + "ad-hoc-config.xml" });

			// materialize the config group
			TestConfigGroup configGroup = ConfigUtils.addOrGetModule(config,
					TestConfigGroup.class);

			// this should have two parameter sets here
			Assert.assertEquals(2, configGroup.getParameterSets("blabla").size());

			// check if you are getting back the values from the config file:
			for (TestParameterSet set : (Iterable<TestParameterSet>) configGroup.getParameterSets("blabla")) {
				Assert.assertEquals("unexpected value for parameter in parameter set", "life is wonderful", set.getParameter());
			}

		}
	}


}

class TestConfigGroup extends ReflectiveConfigGroup {
	public static String GROUP_NAME = "my group";
	private int myField = 42;

	public TestConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter("myField")
	public int getMyField() {
		return myField;
	}

	@StringSetter("myField")
	public void setMyField(int myField) {
		this.myField = myField;
	}



	@Override
	public ConfigGroup createParameterSet(String type) {
		return new TestParameterSet(type);
	}
}

class TestParameterSet extends ReflectiveConfigGroup {
	private String parameter = "42";

	public TestParameterSet(String type) {
		super(type);
	}

	@StringGetter("parameter")
	public String getParameter() {
		return parameter;
	}

	@StringSetter("parameter")
	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
}
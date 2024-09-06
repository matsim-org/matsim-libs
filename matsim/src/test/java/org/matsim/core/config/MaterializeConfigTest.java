package org.matsim.core.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

public class MaterializeConfigTest {

	private static final Logger log = LogManager.getLogger(MaterializeConfigTest.class);
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	final void testMaterializeAfterReadParameterSets() {
		{
			// generate a test config that sets two values away from their defaults, and
			// write it to file:
			Config config = ConfigUtils.createConfig();
			TestConfigGroup configGroup = ConfigUtils.addOrGetModule(config,
					TestConfigGroup.class);
			for (int i=0; i < 2; i++) {
				TestParameterSet set = (TestParameterSet) configGroup.createParameterSet("blabla");
				set.setParameter("life is wonderful");
				configGroup.addParameterSet(set);
			}

			Assertions.assertEquals(
				2, configGroup.getParameterSets("blabla").size(), "unexpected number of parameter sets in initial config group");

			ConfigUtils.writeConfig(config, utils.getOutputDirectory() + "ad-hoc-config.xml");
		}

		{
			// load config file without materializing the config group
			Config config = ConfigUtils.loadConfig(new String[] { utils.getOutputDirectory() + "ad-hoc-config.xml" });

			{
				ConfigGroup configGroup = config.getModule(TestConfigGroup.GROUP_NAME);
				Assertions.assertEquals(
					2, configGroup.getParameterSets("blabla").size(), "unexpected number of parameter sets in non materialized config group");
			}

			// materialize the config group
			TestConfigGroup configGroup = ConfigUtils.addOrGetModule(config,
					TestConfigGroup.class);

			// this should have two parameter sets here
			Assertions.assertEquals(
				2, configGroup.getParameterSets("blabla").size(), "unexpected number of parameter sets in materialized config group");

			// check if you are getting back the values from the config file:
			for (TestParameterSet set : (Iterable<TestParameterSet>) configGroup.getParameterSets("blabla")) {
				Assertions.assertEquals("life is wonderful", set.getParameter(), "unexpected value for parameter in parameter set");
			}

		}
	}


}

class TestConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "my group";
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

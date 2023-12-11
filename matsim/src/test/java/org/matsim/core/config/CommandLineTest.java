package org.matsim.core.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.util.NoSuchElementException;

public class CommandLineTest{

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	void testStandardUsage() {

		String outDir = utils.getOutputDirectory() ;
		final String configFilename = outDir + "/config.xml";

		// write some config:
		ConfigUtils.writeConfig( ConfigUtils.createConfig(), configFilename );

		String [] args = {configFilename, "--something=abc"} ;

		Config config = ConfigUtils.loadConfig( args ) ;
		CommandLine cmd = ConfigUtils.getCommandLine( args );

		Assertions.assertEquals( "abc", cmd.getOption( "something" ).get() );

	}

	@Test
	void testTypo() {
		assertThrows(NoSuchElementException.class, () -> {

			final String configFilename = utils.getOutputDirectory() + "/config.xml";

			// write some config:
			ConfigUtils.writeConfig(ConfigUtils.createConfig(), configFilename);

			String [] args = {configFilename, "--something=abc"} ;

			Config config = ConfigUtils.loadConfig(args) ;
			CommandLine cmd = ConfigUtils.getCommandLine(args);

			Assertions.assertEquals("abc", cmd.getOption("someting").get());

		});

	}

	@Test
	void testAdditionalConfigGroup() {

		final String configFilename = utils.getOutputDirectory() + "/config.xml";

		{
			// write some config:
			final Config config = ConfigUtils.createConfig() ;
			MockConfigGroup mockConfigGroup = ConfigUtils.addOrGetModule( config, MockConfigGroup.class ) ;
			mockConfigGroup.setAbc( -13 );
			ConfigUtils.writeConfig( config, configFilename );
		}
		{
			String[] args = {configFilename, "--config:mockConfigGroup.abc=28"};
			Config config = ConfigUtils.loadConfig( args );
			MockConfigGroup mcg = ConfigUtils.addOrGetModule( config, MockConfigGroup.class ) ;
			Assertions.assertEquals( 28., mcg.getAbc(), 0. );
		}
	}

	@Test
	void testSetParameterInAllParameterSets() {

		final String configFilename = utils.getOutputDirectory() + "/config.xml";

		{
			// write some config:
			final Config config = ConfigUtils.createConfig();
			MockConfigGroup mockConfigGroup = ConfigUtils.addOrGetModule(config, MockConfigGroup.class);
			MockParameterSet mockParameterSetA = new MockParameterSet();
			mockParameterSetA.test = "a";
			mockConfigGroup.addParameterSet(mockParameterSetA);
			MockParameterSet mockParameterSetB = new MockParameterSet();
			mockParameterSetB.test = "b";
			mockConfigGroup.addParameterSet(mockParameterSetB);
			ConfigUtils.writeConfig(config, configFilename);
		}
		{
			String[] args = { configFilename, "--config:mockConfigGroup.mockSet[*=*].test=c" };
			Config config = ConfigUtils.loadConfig(args);
			MockConfigGroup mockConfigGroup = ConfigUtils.addOrGetModule(config, MockConfigGroup.class);
			Assertions.assertEquals(2, mockConfigGroup.getParameterSets(MockParameterSet.SET_TYPE).size());
			for (ConfigGroup parameterSet : mockConfigGroup.getParameterSets(MockParameterSet.SET_TYPE)) {
				Assertions.assertEquals("c", ((MockParameterSet) parameterSet).test);
			}
		}
	}

	@Test
	void testNotYetExistingAdditionalConfigGroup() {
		assertThrows(RuntimeException.class, () -> {
			final String configFilename = utils.getOutputDirectory() + "/config.xml";
			{
				// write some config:
				final Config config = ConfigUtils.createConfig() ;
				ConfigUtils.writeConfig(config, configFilename);
			}
			{
				String[] args = {configFilename, "--config:mockConfigGroup.abc=28"};
				Config config = ConfigUtils.loadConfig(args);
				// (fails in the above line because CommandLine can not deal with the additional config group when it does not know about it

			}
		});
	}

	@Test
	void testFixNotYetExistingAdditionalConfigGroup() {
		final String configFilename = utils.getOutputDirectory() + "/config.xml";
		{
			// write some config:
			final Config config = ConfigUtils.createConfig() ;
			ConfigUtils.writeConfig( config, configFilename );
		}
		{
			String[] args = {configFilename, "--config:mockConfigGroup.abc=28"};
			Config config = ConfigUtils.loadConfig( args, new MockConfigGroup() );
			MockConfigGroup mcg = ConfigUtils.addOrGetModule( config, MockConfigGroup.class ) ;
			Assertions.assertEquals( 28., mcg.getAbc(), 0. );
		}
	}

	private static class MockConfigGroup extends ReflectiveConfigGroup {

		MockConfigGroup(){
			super("mockConfigGroup");
		}

		double abc = 0.;

		@StringSetter("abc")
		public void setAbc( double val ) {
			this.abc = val ;
		}

		@StringGetter("abc")
		public double getAbc() {
			return this.abc ;
		}

		@Override
		public ConfigGroup createParameterSet(final String type) {
			switch (type) {
				case MockParameterSet.SET_TYPE:
					return new MockParameterSet();
				default:
					throw new IllegalArgumentException(type);
			}
		}

		@Override
		public void addParameterSet(final ConfigGroup set) {
			switch (set.getName()) {
				case MockParameterSet.SET_TYPE:
					super.addParameterSet((MockParameterSet) set);
					break;
				default:
					throw new IllegalArgumentException(set.getName());
			}
		}

		@Override
		protected void checkParameterSet(final ConfigGroup module) {
			switch (module.getName()) {
				case MockParameterSet.SET_TYPE:
					if (!(module instanceof MockParameterSet)) {
						throw new RuntimeException("wrong class for " + module);
					}
					break;
				default:
					throw new IllegalArgumentException(module.getName());
			}
		}

	}

	private static class MockParameterSet extends ReflectiveConfigGroup {

		public static final String SET_TYPE = "mockSet";

		public MockParameterSet() {
			super(SET_TYPE);
		}

		@Parameter
		@Comment("test")
		String test;

	}

}

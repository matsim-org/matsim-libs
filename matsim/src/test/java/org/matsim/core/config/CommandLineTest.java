package org.matsim.core.config;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import java.util.NoSuchElementException;

public class CommandLineTest{

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testStandardUsage() {

		String outDir = utils.getOutputDirectory() ;
		final String configFilename = outDir + "/config.xml";

		// write some config:
		ConfigUtils.writeConfig( ConfigUtils.createConfig(), configFilename );

		String [] args = {configFilename, "--something=abc"} ;

		Config config = ConfigUtils.loadConfig( args ) ;
		CommandLine cmd = ConfigUtils.getCommandLine( args );

		Assert.assertEquals( "abc", cmd.getOption( "something" ).get() );

	}

	@Test( expected = NoSuchElementException.class )
	public void testTypo() {

		final String configFilename = utils.getOutputDirectory() + "/config.xml";

		// write some config:
		ConfigUtils.writeConfig( ConfigUtils.createConfig(), configFilename );

		String [] args = {configFilename, "--something=abc"} ;

		Config config = ConfigUtils.loadConfig( args ) ;
		CommandLine cmd = ConfigUtils.getCommandLine( args );

		Assert.assertEquals( "abc", cmd.getOption( "someting" ).get() );

	}
	@Test
	public void testAdditionalConfigGroup() {

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
			Assert.assertEquals( 28., mcg.getAbc(), 0. );
		}
	}
	@Test( expected = RuntimeException.class )
	public void testNotYetExistingAdditionalConfigGroup() {
		final String configFilename = utils.getOutputDirectory() + "/config.xml";
		{
			// write some config:
			final Config config = ConfigUtils.createConfig() ;
			ConfigUtils.writeConfig( config, configFilename );
		}
		{
			String[] args = {configFilename, "--config:mockConfigGroup.abc=28"};
			Config config = ConfigUtils.loadConfig( args );
			// (fails in the above line because CommandLine can not deal with the additional config group when it does not know about it

		}
	}

	@Test
	public void testFixNotYetExistingAdditionalConfigGroup() {
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
			Assert.assertEquals( 28., mcg.getAbc(), 0. );
		}
	}

	private static class MockConfigGroup extends ReflectiveConfigGroup {
		MockConfigGroup(){
			super( "mockConfigGroup" );
		}
		double abc = 0. ;
		@StringSetter("abc")
		public void setAbc( double val ) {
			this.abc = val ;
		}
		@StringGetter("abc")
		public double getAbc() {
			return this.abc ;
		}
	}

}

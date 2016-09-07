package playground.ivt.maxess.tripbased;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.testcases.MatsimTestUtils;
import playground.ivt.maxess.prepareforbiogeme.tripbased.PrismicConversionConfigGroup;
import playground.ivt.maxess.prepareforbiogeme.tripbased.RunPrismicTripChoiceSetConversion;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * More a regression/integration test than a unit test
 */
public class RunPrismicTripChoiceSetConversionTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test @Ignore( "see MATSIM-558" )
	public void runTest() {
		final PrismicConversionConfigGroup group = new PrismicConversionConfigGroup();
		final Config config =
				utils.loadConfig(
						// any scenario with facilities and public transport
						"test/scenarios/siouxfalls-2014-reduced/config_default.xml",
						group);
		group.setOutputPath( utils.getOutputDirectory()+"output/" );
		group.setActivityType( "secondary" );
		group.setNumberOfThreads( 2 );

		final String configFile = utils.getOutputDirectory()+"/input_config.xml";
		new ConfigWriter( config ).write( configFile );
		RunPrismicTripChoiceSetConversion.main(configFile);
		Assert.assertEquals(
				"Unexpected number of lines",
				6,
				nLines( utils.getOutputDirectory()+"output/data.dat"  ) );
	}

	private int nLines(String file) {
		final BufferedReader r = IOUtils.getBufferedReader( file );
		int i = 0;
		try {
			while ( r.readLine() != null ) i++;
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
		return i;
	}

}

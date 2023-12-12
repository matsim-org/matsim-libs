package org.matsim.application.prepare;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;


public class ShapeFileTextLookupTest {

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void main() {

        Path input = Path.of(utils.getClassInputDirectory(), "verkehrszellen.csv");
        Path output = Path.of(utils.getOutputDirectory(), "output.csv");
        Assumptions.assumeTrue(Files.exists(input));

        CommandLine cli = new CommandLine(new ShapeFileTextLookup());
        int ret = cli.execute(
                input.toString(),
                "--output", output.toString(),
                "--shp-charset", "UTF8",
                "--shp", Path.of(utils.getClassInputDirectory(), "NUTS3", "NUTS3_2010_DE.shp").toString(),
                "--csv-column", "Verkehrszellenname",
                "--shp-column", "NUTS_NAME"
        );

        assertThat(ret).isEqualTo(0);

        assertThat(output)
                .exists();

    }
}

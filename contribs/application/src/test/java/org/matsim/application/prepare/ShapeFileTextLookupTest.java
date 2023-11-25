package org.matsim.application.prepare;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import picocli.CommandLine;

public class ShapeFileTextLookupTest {

  @Rule public MatsimTestUtils utils = new MatsimTestUtils();

  @Test
  public void main() {

    Path input = Path.of(utils.getClassInputDirectory(), "verkehrszellen.csv");
    Path output = Path.of(utils.getOutputDirectory(), "output.csv");
    Assume.assumeTrue(Files.exists(input));

    CommandLine cli = new CommandLine(new ShapeFileTextLookup());
    int ret =
        cli.execute(
            input.toString(),
            "--output",
            output.toString(),
            "--shp-charset",
            "UTF8",
            "--shp",
            Path.of(utils.getClassInputDirectory(), "NUTS3", "NUTS3_2010_DE.shp").toString(),
            "--csv-column",
            "Verkehrszellenname",
            "--shp-column",
            "NUTS_NAME");

    assertThat(ret).isEqualTo(0);

    assertThat(output).exists();
  }
}

package playground.dziemke.analysis;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;
import playground.dziemke.accessibility.OTPMatrix.CSVReader;

import java.io.File;
import java.io.IOException;

/**
 * @author gthunig
 */
public class TripAnalyzerBasicIT {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void testTripAnalyzerBasic() {
        String[] args = new String[3];
        args[0] = "../../examples/scenarios/equil/network.xml";
        File network = new File(args[0]);
        try {
            System.out.println(network.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        args[1] = utils.getInputDirectory() + "10.events.xml";
        args[2] = utils.getOutputDirectory();
        TripAnalyzerBasic.main(args);

        //Check average values
        String activityTypes = args[2] + "activityTypes.txt";
        Assert.assertEquals("Unexpected Checksum with activityTypes.txt",
                315799506L, CRCChecksum.getCRCFromFile(activityTypes));
        String averageTripSpeedBeeline = args[2] + "averageTripSpeedBeeline.txt";
        CSVReader reader = new CSVReader(averageTripSpeedBeeline, " ");
        String[] line = reader.readLine();
        while (!line[0].equals("Average")) {
            line = reader.readLine();
        }
        Assert.assertEquals(54.0723282313473D, Double.parseDouble(line[2]), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Unexpected Checksum with averageTripSpeedBeeline.txt",
                2392786094L, CRCChecksum.getCRCFromFile(averageTripSpeedBeeline));
        String averageTripSpeedBeelineCumulative = args[2] + "averageTripSpeedBeelineCumulative.txt";
        reader = new CSVReader(averageTripSpeedBeelineCumulative, " ");
        line = reader.readLine();
        while (!line[0].equals("Average")) {
            line = reader.readLine();
        }
        Assert.assertEquals(54.0723282313473D, Double.parseDouble(line[2]), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Unexpected Checksum with averageTripSpeedBeelineCumulative.txt",
                1145957025L, CRCChecksum.getCRCFromFile(averageTripSpeedBeelineCumulative));
        String averageTripSpeedRouted = args[2] + "averageTripSpeedRouted.txt";
        reader = new CSVReader(averageTripSpeedRouted, " ");
        line = reader.readLine();
        while (!line[0].equals("Average")) {
            line = reader.readLine();
        }
        Assert.assertEquals(98.35892482335204D, Double.parseDouble(line[2]), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Unexpected Checksum with averageTripSpeedRouted.txt",
                3753203302L, CRCChecksum.getCRCFromFile(averageTripSpeedRouted));
        String departureTime = args[2] + "departureTime.txt";
        reader = new CSVReader(departureTime, " ");
        line = reader.readLine();
        while (!line[0].equals("Average")) {
            line = reader.readLine();
        }
        Assert.assertEquals(Double.NaN, Double.parseDouble(line[2]), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Unexpected Checksum with activityTypes.txt",
                1408739987L, CRCChecksum.getCRCFromFile(departureTime));
        String otherInformation = args[2] + "otherInformation.txt";
        reader = new CSVReader(otherInformation, " ");
        line = reader.readLine();
        while (!line[0].equals("Number")) {
            line = reader.readLine();
        }
        Assert.assertEquals(0.0D, Double.parseDouble(line[8].split("\t")[1]), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Unexpected Checksum with otherInformation.txt",
                2704521663L, CRCChecksum.getCRCFromFile(otherInformation));
        String tripDistanceBeeline = args[2] + "tripDistanceBeeline.txt";
        reader = new CSVReader(tripDistanceBeeline, " ");
        line = reader.readLine();
        while (!line[0].equals("Average")) {
            line = reader.readLine();
        }
        Assert.assertEquals(20.0D, Double.parseDouble(line[2]), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Unexpected Checksum with tripDistanceBeeline.txt",
                918885130L, CRCChecksum.getCRCFromFile(tripDistanceBeeline));
        String tripDistanceBeelineCumulative = args[2] + "tripDistanceBeelineCumulative.txt";
        reader = new CSVReader(tripDistanceBeelineCumulative, " ");
        line = reader.readLine();
        while (!line[0].equals("Average")) {
            line = reader.readLine();
        }
        Assert.assertEquals(20.0D, Double.parseDouble(line[2]), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Unexpected Checksum with tripDistanceBeelineCumulative.txt",
                918885130L, CRCChecksum.getCRCFromFile(tripDistanceBeelineCumulative));
        String tripDistanceRouted = args[2] + "tripDistanceRouted.txt";
        reader = new CSVReader(tripDistanceRouted, " ");
        line = reader.readLine();
        while (!line[0].equals("Average")) {
            line = reader.readLine();
        }
        Assert.assertEquals(44.99980999999998D, Double.parseDouble(line[2]), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Unexpected Checksum with tripDistanceRouted.txt",
                3495656168L, CRCChecksum.getCRCFromFile(tripDistanceRouted));
        String tripDuration = args[2] + "tripDuration.txt";
        reader = new CSVReader(tripDuration, " ");
        line = reader.readLine();
        while (!line[0].equals("Average")) {
            line = reader.readLine();
        }
        Assert.assertEquals(27.263333333333335D, Double.parseDouble(line[2]), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Unexpected Checksum with tripDuration.txt",
                13202731L, CRCChecksum.getCRCFromFile(tripDuration));
        String tripDurationCumulative = args[2] + "tripDurationCumulative.txt";
        reader = new CSVReader(tripDuration, " ");
        line = reader.readLine();
        while (!line[0].equals("Average")) {
            line = reader.readLine();
        }
        Assert.assertEquals(27.263333333333335D, Double.parseDouble(line[2]), MatsimTestUtils.EPSILON);
        Assert.assertEquals("Unexpected Checksum with tripDurationCumulative.txt",
                1662768883L, CRCChecksum.getCRCFromFile(tripDurationCumulative));
    }
}

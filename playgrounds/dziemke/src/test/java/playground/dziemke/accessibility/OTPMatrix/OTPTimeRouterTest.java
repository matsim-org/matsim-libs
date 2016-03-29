package playground.dziemke.accessibility.OTPMatrix;

import com.vividsolutions.jts.geom.Coordinate;
import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.SyntheticRasterPopulation;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class OTPTimeRouterTest {

    private static final Logger log = LoggerFactory.getLogger(OTPTimeRouterTest.class);
    private static final double EPSILON = 1e-10;

    @Test
    public void testMatrixRouting() throws Exception {

        final Calendar calendar = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        TimeZone timeZone = TimeZone.getTimeZone("America/San_Francisco");
        df.setTimeZone(timeZone);
        calendar.setTimeZone(timeZone);
        try {
            calendar.setTime(df.parse("2016-02-02"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calendar.add(Calendar.SECOND, (7*60*55));

        SyntheticRasterPopulation rasterPop = new SyntheticRasterPopulation();
        rasterPop.top = 33.9538;
        rasterPop.left = -117.4495;
        rasterPop.bottom = 33.9244;
        rasterPop.right = -117.3978;
        rasterPop.cols = 2;
        rasterPop.rows = 2;
        rasterPop.setup();

        String input_dir = "input/testMatrixRouting/";

        Iterator<Individual> iterator = rasterPop.iterator();
        List<Individual> individuals = new ArrayList<>();
        while (iterator.hasNext()) {
            individuals.add(iterator.next());
        }
        double[] actuals = new double[individuals.size()*individuals.size()];

        Graph graph = OTPMatrixRouter.loadGraph(input_dir);

        double[] expecteds = new double[16];
        expecteds[0] = 0.0;
        expecteds[1] = 417.0;
        expecteds[2] = 1642.0;
        expecteds[3] = 1578.0;
        expecteds[4] = 1804.0;
        expecteds[5] = 0.0;
        expecteds[6] = 1449.0;
        expecteds[7] = 1385.0;
        expecteds[8] = 1631.0;
        expecteds[9] = 2585.0;
        expecteds[10] = 0.0;
        expecteds[11] = 2413.0;
        expecteds[12] = 2477.0;
        expecteds[13] = 1801.0;
        expecteds[14] = 1482.0;
        expecteds[15] = 0.0;

        for (int i = 0; i < individuals.size(); i++) {
            for (int e = 0; e < individuals.size(); e++) {
                Coordinate origin = new Coordinate(individuals.get(i).lat, individuals.get(i).lon);
                Coordinate destination = new Coordinate(individuals.get(e).lat, individuals.get(e).lon);
                actuals[i*individuals.size()+e] = OTPMatrixRouter.getSingleRouteTime(graph, calendar, origin, destination);
            }
        }

        Assert.assertArrayEquals(expecteds, actuals, EPSILON);

        log.info("Shutdown");
	}

}

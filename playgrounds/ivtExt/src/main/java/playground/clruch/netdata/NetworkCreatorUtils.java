/**
 * 
 */
package playground.clruch.netdata;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.VectorQ;
import ch.ethz.idsc.tensor.red.Norm;

/** @author Claudio Ruch */
public enum NetworkCreatorUtils {
    ;

    /* package */ static String linkToID(Link link) {
        return link.getId().toString();
    }

    /** @param tensor of length 2 (x,y)
     * @return Coord from tensor */
    public static Coord fromTensor(Tensor tensor) {
        VectorQ.ofLength(tensor, 2); // ensure that vector of length 2;
        return new Coord(tensor.Get(0).number().doubleValue(), //
                tensor.Get(1).number().doubleValue());

    }

    /** @param coord
     * @return Tensor of length 2 (x,y) from Coord */
    /* package */ static Tensor fromCoord(Coord coord) {
        return Tensors.vectorDouble(coord.getX(), coord.getY());
    }

    /* package */ static double distance(Tensor t1, Tensor t2) {
        return Norm._2.of(t1.subtract(t2)).number().doubleValue();
    }

    /** @param population
     * @param network
     * @return double of length m,2 with m datapoints and their x,y coordinate where datapoits represent all
     *         Activities of agents in population. */
    public static double[][] fromPopulation(Population population, Network network) {
        // FOR ALL activities find positions, record in list and store in array
        List<double[]> dataList = new ArrayList<>();

        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElem : plan.getPlanElements()) {
                    if (planElem instanceof Activity) {
                        double x = network.getLinks().get(((Activity) planElem).getLinkId()).getCoord().getX();
                        double y = network.getLinks().get(((Activity) planElem).getLinkId()).getCoord().getY();
                        dataList.add(new double[] { x, y });
                    }
                }
            }
        }

        final double data[][] = new double[dataList.size()][2];
        for (int i = 0; i < dataList.size(); ++i) {
            data[i][0] = dataList.get(i)[0];
            data[i][1] = dataList.get(i)[1];
        }

        return data;

    }

}

package playground.fseccamo.dispatcher;

import ch.ethz.idsc.jmex.Container;
import ch.ethz.idsc.jmex.DoubleArray;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.Primitives;
import ch.ethz.idsc.tensor.red.KroneckerDelta;
import playground.clruch.prep.PopulationRequestSchedule;

enum MpcUtils {
    ;

    public static Container getContainerInit(VirtualNetwork virtualNetwork, int samplingPeriod, int numberOfVehicles) throws Exception {
        final int m = virtualNetwork.getvLinksCount();
        final int n = virtualNetwork.getvNodesCount();

        Container container = new Container("init");
        { // directed graph incidence matrix
            Tensor matrix = Tensors.matrix((i, j) -> KroneckerDelta.of(virtualNetwork.getVirtualLink(j).getTo().getIndex(), i), n, m);
            double[] array = Primitives.toArrayDouble(Transpose.of(matrix));
            DoubleArray doubleArray = new DoubleArray("E_in", new int[] { n, m }, array);
            container.add(doubleArray);
        }
        {
            Tensor matrix = Tensors.matrix((i, j) -> KroneckerDelta.of(virtualNetwork.getVirtualLink(j).getFrom().getIndex(), i), n, m);
            double[] array = Primitives.toArrayDouble(Transpose.of(matrix));
            DoubleArray doubleArray = new DoubleArray("E_out", new int[] { n, m }, array);
            container.add(doubleArray);
        }
        {
            double[] array = new double[] { samplingPeriod };
            DoubleArray doubleArray = new DoubleArray("Ts", new int[] { 1 }, array);
            container.add(doubleArray);
        }
        {
            Tensor matrix = Tensors.vector(i -> Tensors.vector( //
                    virtualNetwork.getVirtualNode(i).getCoord().Get(0).number().doubleValue(), //
                    virtualNetwork.getVirtualNode(i).getCoord().Get(1).number().doubleValue() //
            ), n);
            // System.out.println(Pretty.of(matrix));
            double[] array = Primitives.toArrayDouble(Transpose.of(matrix));
            DoubleArray doubleArray = new DoubleArray("voronoiCenter", new int[] { n, 2 }, array);
            container.add(doubleArray);
        }
        {
            double[] array = new double[] { numberOfVehicles };
            GlobalAssert.that(0 < numberOfVehicles);
            DoubleArray doubleArray = new DoubleArray("N_cars", new int[] { 1 }, array);
            container.add(doubleArray);
        }
        final Tensor populationRequestSchedule = PopulationRequestSchedule.importDefault();
        // TODO in the future use to tune:
        final int expectedRequestCount = populationRequestSchedule.length();
        {
            double[] array = Primitives.toArrayDouble(Transpose.of(populationRequestSchedule));
            DoubleArray doubleArray = new DoubleArray("requestSchedule", new int[] { populationRequestSchedule.length(), 3 }, array);
            container.add(doubleArray);
        }
        {
            double[] array = new double[] { expectedRequestCount };
            GlobalAssert.that(0 < expectedRequestCount);
            DoubleArray doubleArray = new DoubleArray("expectedRequestCount", new int[] { 1 }, array);
            container.add(doubleArray);
        }
        return container;
    }
}

package playground.joel.analysis;

import java.io.File;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Quantile;
import playground.clruch.gfx.RequestWaitingVirtualNodeFunction;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.netdata.VirtualNetwork;

/**
 * Created by Joel on 05.04.2017.
 */
class VirtualNetworkAnalysis {
    final StorageSupplier storageSupplier;
    final int size;
    final VirtualNetwork virtualNetwork;
    Tensor summary = Tensors.empty();
    Tensor totalWaitTimeQuantile = Tensors.empty();
    Tensor totalWaitTimeMean = Tensors.empty();

    VirtualNetworkAnalysis(StorageSupplier storageSupplier, VirtualNetwork virtualNetwork) {
        this.storageSupplier = storageSupplier;
        size = storageSupplier.size();
        this.virtualNetwork = virtualNetwork;
    }

    static Tensor quantiles(Tensor submission) {
        if (3 < submission.length()) {
            return Quantile.of(submission, Tensors.vectorDouble(.1, .5, .95));
        } else {
            return Array.zeros(3);
        }
    }

    static Tensor means(Tensor submission) {
        if (3 < submission.length()) {
            return Mean.of(submission);
        } else {
            return Mean.of(Array.zeros(1));
        }
    }

    public void analyze() throws Exception {
        Tensor table = Tensors.empty();
        MatsimStaticDatabase db = MatsimStaticDatabase.INSTANCE;
        for (int index = 0; index < size; ++index) {
            SimulationObject ref = storageSupplier.getSimulationObject(index);
            Tensor eval = new RequestWaitingVirtualNodeFunction(db, virtualNetwork, //
                    RequestWaitingVirtualNodeFunction::meanOrZero).evaluate(ref);
            table.append(eval);
            if (ref.now % 10000 == 0)
                System.out.println(ref.now);
        }
        Export.of(new File(AnalyzeAll.RELATIVE_DIRECTORY, "customerWaitingPerVNode.csv"), table);
    }
}

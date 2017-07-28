package playground.clruch.analysis;

import java.io.File;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Join;
import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Quantile;
import playground.clruch.gfx.RequestWaitingVirtualNodeFunction;
import playground.clruch.gfx.VirtualNodeFunction;
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
        final MatsimStaticDatabase db = MatsimStaticDatabase.INSTANCE;
        // ---
        final Tensor tableMean = Tensors.empty();
        final VirtualNodeFunction vnf_mean = new RequestWaitingVirtualNodeFunction(db, virtualNetwork, //
                RequestWaitingVirtualNodeFunction::meanOrZero);
        // ---
        final Tensor tableMedian = Tensors.empty();
        final VirtualNodeFunction vnf_median = new RequestWaitingVirtualNodeFunction(db, virtualNetwork, //
                RequestWaitingVirtualNodeFunction::medianOrZero);
        // ---
        final Tensor tableMax = Tensors.empty();
        final VirtualNodeFunction vnf_max = new RequestWaitingVirtualNodeFunction(db, virtualNetwork, //
                RequestWaitingVirtualNodeFunction::maxOrZero);
        // ---
        for (int index = 0; index < size; ++index) {
            SimulationObject ref = storageSupplier.getSimulationObject(index);
            tableMean.append(Join.of(Tensors.vector(ref.now), vnf_mean.evaluate(ref)));
            tableMedian.append(Join.of(Tensors.vector(ref.now), vnf_median.evaluate(ref)));
            tableMax.append(Join.of(Tensors.vector(ref.now), vnf_max.evaluate(ref)));
            if (ref.now % 10000 == 0)
                System.out.println(ref.now);
        }
        Export.of(new File(AnalyzeAll.RELATIVE_DIRECTORY, "RequestWaitingVirtualNode_Mean.csv"), tableMean);
        Export.of(new File(AnalyzeAll.RELATIVE_DIRECTORY, "RequestWaitingVirtualNode_Median.csv"), tableMedian);
        Export.of(new File(AnalyzeAll.RELATIVE_DIRECTORY, "RequestWaitingVirtualNode_Max.csv"), tableMax);
    }
}

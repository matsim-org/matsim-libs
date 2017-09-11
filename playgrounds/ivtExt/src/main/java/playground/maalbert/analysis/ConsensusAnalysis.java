package playground.maalbert.analysis;

import static playground.clruch.utils.NetworkLoader.loadNetwork;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import com.google.inject.Inject;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Quantile;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;

class ConsensusAnalysis {
    StorageSupplier storageSupplier;
    int size;
    // String dataPath;
    String[] args;

    // ConsensusAnalysis(String[] argmnts, StorageSupplier storageSupplierIn, String datapath){
    ConsensusAnalysis(String[] argmnts, StorageSupplier storageSupplierIn) {
        storageSupplier = storageSupplierIn;
        size = storageSupplier.size();
        // dataPath = datapath;
        args = argmnts;
    }

    @Inject
    private Network network;

    public void analyze() throws Exception {
        System.out.print("Saving Wait Times...");

        // ==============================================================================================================
        // INIT
        // ==============================================================================================================

        Tensor MeanWaitTimes = Tensors.empty();
        Tensor Q10waitTimes = Tensors.empty();
        Tensor Q50waitTimes = Tensors.empty();
        Tensor Q95waitTimes = Tensors.empty();
        Tensor NoRequests = Tensors.empty();

        // DEBUG START

        Network network = loadNetwork(new File(args[0]));
        final File virtualnetworkFile = new File("vN_40vS_L1_v2\\virtualNetwork.xml");
        GlobalAssert.that(virtualnetworkFile.isFile());
        // VirtualNetwork virtualNetwork = VirtualNetworkIO.fromXML(network, virtualnetworkFile);

        VirtualNetwork virtualNetwork = null;
        GlobalAssert.that(virtualNetwork != null);
        // TODO fromXML is deprecated, load differently.

        MatsimStaticDatabase.initializeSingletonInstance( //
                network, ReferenceFrame.IDENTITY);

        Map<Link, Integer> linkIntegerMap = MatsimStaticDatabase.INSTANCE.getLinkInteger();

        // TODO remove loop and do more elegantly
        Map<Integer, Link> rev_linkIntegerMap = new HashMap<>();
        for (Map.Entry<Link, Integer> entry : linkIntegerMap.entrySet()) {
            rev_linkIntegerMap.put(entry.getValue(), entry.getKey());
        }

        int N_vStations = virtualNetwork.getvNodesCount();

        for (int index = 0; index < size; ++index) {

            SimulationObject s = storageSupplier.getSimulationObject(index);
            final long now = s.now;

            // ==========================================================================================================
            // Compute Wait Times and System Imbalance
            // ==========================================================================================================
            // Wait Times Per Virtual Station
            {
                Tensor MeanWaitTimes_tmp = Tensors.empty();
                Tensor Q10waitTimes_tmp = Tensors.empty();
                Tensor Q50waitTimes_tmp = Tensors.empty();
                Tensor Q95waitTimes_tmp = Tensors.empty();
                Tensor NoRequests_tmp = Tensors.empty();

                for (int i = 0; i < N_vStations; i++) {
                    // Get wait times per vNode
                    VirtualNode vStation = virtualNetwork.getVirtualNode(i);
                    Tensor waitTimes_i = Tensor
                            .of(s.requests.stream().filter(r -> virtualNetwork.getVirtualNode(rev_linkIntegerMap.get(r.fromLinkIndex)) == vStation)
                                    .map(rc -> RealScalar.of(now - rc.submissionTime)));

                    // Compute Statistics
                    NoRequests_tmp.append(RealScalar.of(waitTimes_i.length()));

                    if (waitTimes_i.length() < 1) {
                        MeanWaitTimes_tmp.append(RealScalar.of(0));
                        Q10waitTimes_tmp.append(RealScalar.of(0));
                        Q50waitTimes_tmp.append(RealScalar.of(0));
                        Q95waitTimes_tmp.append(RealScalar.of(0));
                    } else {
                        MeanWaitTimes_tmp.append(Mean.of(waitTimes_i));
                        Q10waitTimes_tmp.append(Quantile.of(waitTimes_i, RealScalar.of(.1)));
                        Q50waitTimes_tmp.append(Quantile.of(waitTimes_i, RealScalar.of(.5)));
                        Q95waitTimes_tmp.append(Quantile.of(waitTimes_i, RealScalar.of(.95)));
                    }
                }
                MeanWaitTimes.append(MeanWaitTimes_tmp);
                Q10waitTimes.append(Q10waitTimes_tmp);
                Q50waitTimes.append(Q50waitTimes_tmp);
                Q95waitTimes.append(Q95waitTimes_tmp);
                NoRequests.append(NoRequests_tmp);
            }
            // System Imbalance Per Virtual Station
            // TODO

            // ==========================================================================================================
            // END
            // ==========================================================================================================
        }

        {
            AnalyzeMarc.saveFile(MeanWaitTimes, "MeanWaitTimes");
            AnalyzeMarc.saveFile(Q10waitTimes, "Q10WaitTimes");
            AnalyzeMarc.saveFile(Q50waitTimes, "Q50WaitTimes");
            AnalyzeMarc.saveFile(Q95waitTimes, "Q95WaitTimes");
            AnalyzeMarc.saveFile(NoRequests, "NoRequests");
        }
        System.out.print("Done.\n");
    }
}

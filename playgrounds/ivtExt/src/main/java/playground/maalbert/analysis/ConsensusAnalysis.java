package playground.maalbert.analysis;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Quantile;
import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import playground.clruch.export.AVStatus;
import playground.clruch.gfx.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkIO;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;

import java.io.File;
import java.util.*;

import static playground.clruch.utils.NetworkLoader.loadNetwork;

class ConsensusAnalysis {
    StorageSupplier storageSupplier;
    int size;
    //  String dataPath;
    String[] args;



    // ConsensusAnalysis(String[] argmnts, StorageSupplier storageSupplierIn, String datapath){
    ConsensusAnalysis(String[] argmnts, StorageSupplier storageSupplierIn){
        storageSupplier = storageSupplierIn;
        size = storageSupplier.size();
        // dataPath = datapath;
        args = argmnts;
    }

    @Inject
    private Network network;

    public void analyze() throws Exception {
        System.out.print("Saving Wait Times...");

        //==============================================================================================================
        // INIT
        //==============================================================================================================
        //Wait Time Analysis
        Tensor MeanWaitTimes = Tensors.empty();
        Tensor Q10waitTimes  = Tensors.empty();
        Tensor Q50waitTimes  = Tensors.empty();
        Tensor Q95waitTimes  = Tensors.empty();
        //Imbalance Analysis
        Tensor openRequests        = Tensors.empty();
        Tensor systemImbalance     = Tensors.empty();
        Tensor availableVehicles   = Tensors.empty();
        Tensor rebalancingVehicles = Tensors.empty();


        //DEBUG START

        Network network = loadNetwork(args);
        final File virtualnetworkFile = new File("vN_40_L1_final\\virtualNetwork.xml");
        GlobalAssert.that(virtualnetworkFile.isFile());
        VirtualNetwork virtualNetwork = VirtualNetworkIO.fromXML(network, virtualnetworkFile);


        MatsimStaticDatabase.initializeSingletonInstance( //
                network, ReferenceFrame.IDENTITY);

        Map<Link, Integer> linkIntegerMap = MatsimStaticDatabase.INSTANCE.getLinkInteger();

        //TODO remove loop and do more elegantly
        Map<Integer, Link> rev_linkIntegerMap = new HashMap<>();
        for(Map.Entry<Link, Integer> entry : linkIntegerMap.entrySet()){
            rev_linkIntegerMap.put(entry.getValue(), entry.getKey());
        }

        int N_vStations = virtualNetwork.getvNodesCount();

        for (int index = 0; index < size; ++index) {

            SimulationObject s = storageSupplier.getSimulationObject(index);
            final long now = s.now;

            //==========================================================================================================
            // Compute Wait Times and System Imbalance
            //==========================================================================================================
            // Wait Times Per Virtual Station
            {
                Tensor MeanWaitTimes_tmp = Tensors.empty();
                Tensor Q10waitTimes_tmp  = Tensors.empty();
                Tensor Q50waitTimes_tmp  = Tensors.empty();
                Tensor Q95waitTimes_tmp  = Tensors.empty();
                Tensor openRequests_tmp    = Tensors.empty();
                Tensor systemImbalance_tmp = Tensors.empty();

                for (int i = 0; i < N_vStations; i++) {
                    //Get wait times per vNode
                    VirtualNode vStation = virtualNetwork.getVirtualNode(i);
                    Tensor waitTimes_i = Tensor.of(s.requests.stream().filter(r -> virtualNetwork.getVirtualNode(rev_linkIntegerMap.get(r.fromLinkIndex)) == vStation).map(rc -> RealScalar.of(now - rc.submissionTime)));

                    //Compute Wait Time Statistics
                    Scalar openRequests_i = RealScalar.of(waitTimes_i.length());

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

                    //System Imbalance Per Virtual Station
                    Scalar stayVehicles_i       = RealScalar.of(s.vehicles.stream().filter(v->v.avStatus == AVStatus.STAY && //
                            virtualNetwork.getVirtualNode(rev_linkIntegerMap.get(v.linkIndex))== vStation).count());
                    Scalar rebalanceVehicles_i  = RealScalar.of(s.vehicles.stream().filter(v->v.avStatus == AVStatus.REBALANCEDRIVE && //
                            virtualNetwork.getVirtualNode(rev_linkIntegerMap.get(v.destinationLinkIndex))== vStation).count());
                    Scalar drive2custVehicles_i =  RealScalar.of(s.vehicles.stream().filter(v->v.avStatus == AVStatus.DRIVETOCUSTMER && //
                            virtualNetwork.getVirtualNode(rev_linkIntegerMap.get(v.destinationLinkIndex))== vStation).count());

                    Scalar availableVehicles_i  = stayVehicles_i.add(drive2custVehicles_i); //divertableNotRebalancingVehicles
                    Scalar systemImbalance_i    = openRequests_i.subtract(availableVehicles_i).subtract(rebalanceVehicles_i);


                    systemImbalance_tmp.append(systemImbalance_i);
                    openRequests_tmp.append(openRequests_i);
                }
                MeanWaitTimes.append(MeanWaitTimes_tmp);
                Q10waitTimes.append(Q10waitTimes_tmp);
                Q50waitTimes.append(Q50waitTimes_tmp);
                Q95waitTimes.append(Q95waitTimes_tmp);
                openRequests.append(openRequests_tmp);
                systemImbalance.append(systemImbalance_tmp);
            }

            //==========================================================================================================
            // END
            //==========================================================================================================
        }

        {
            AnalyzeMarc.saveFile(MeanWaitTimes,"MeanWaitTimes");
            AnalyzeMarc.saveFile(Q10waitTimes,"Q10WaitTimes");
            AnalyzeMarc.saveFile(Q50waitTimes,"Q50WaitTimes");
            AnalyzeMarc.saveFile(Q95waitTimes,"Q95WaitTimes");
            AnalyzeMarc.saveFile(openRequests,"NoRequests");
            AnalyzeMarc.saveFile(systemImbalance,"systemImbalance");
        }
        System.out.print("Done.\n");
    }
}




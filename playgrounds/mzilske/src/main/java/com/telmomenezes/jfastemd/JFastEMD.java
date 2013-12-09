/**
 * This class computes the Earth Mover's Distance, using the EMD-HAT algorithm
 * created by Ofir Pele and Michael Werman.
 * 
 * This implementation is strongly based on the C++ code by the same authors,
 * that can be found here:
 * http://www.cs.huji.ac.il/~ofirpele/FastEMD/code/
 * 
 * Some of the author's comments on the original were kept or edited for 
 * this context.
 */



package com.telmomenezes.jfastemd;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * @author Telmo Menezes (telmo@telmomenezes.com)
 * @author Ofir Pele
 *
 */
public class JFastEMD {
    /**
     * This interface is similar to Rubner's interface. See:
     * http://www.cs.duke.edu/~tomasi/software/emd.htm
     *
     * To get the same results as Rubner's code you should set extra_mass_penalty to 0,
     * and divide by the minimum of the sum of the two signature's weights. However, I
     * suggest not to do this as you lose the metric property and more importantly, in my
     * experience the performance is better with emd_hat. for more on the difference
     * between emd and emd_hat, see the paper:
     * A Linear Time Histogram Metric for Improved SIFT Matching
     * Ofir Pele, Michael Werman
     * ECCV 2008
     *
     * To get shorter running time, set the ground distance function to
     * be a thresholded distance. For example: min(L2, T). Where T is some threshold.
     * Note that the running time is shorter with smaller T values. Note also that
     * thresholding the distance will probably increase accuracy. Finally, a thresholded
     * metric is also a metric. See paper:
     * Fast and Robust Earth Mover's Distances
     * Ofir Pele, Michael Werman
     * ICCV 2009
     *
     * If you use this code, please cite the papers.
     */
    static public double distance(Signature signature1, Signature signature2, double extraMassPenalty) {

        Vector<Double> P = new Vector<Double>();
        Vector<Double> Q = new Vector<Double>();
        for (int i = 0; i < signature1.getNumberOfFeatures() + signature2.getNumberOfFeatures(); i++) {
            P.add(0.0);
            Q.add(0.0);
        }
        for (int i = 0; i < signature1.getNumberOfFeatures(); i++) {
            P.set(i, signature1.getWeights()[i]);
        }
        for (int j = 0; j < signature2.getNumberOfFeatures(); j++) {
            Q.set(j + signature1.getNumberOfFeatures(), signature2.getWeights()[j]);
        }

        Vector<Vector<Double>> C = new Vector<Vector<Double>>();
        for (int i = 0; i < P.size(); i++) {
            Vector<Double> vec = new Vector<Double>();
            for (int j = 0; j < P.size(); j++) {
                vec.add(0.0);
            }
            C.add(vec);
        }
        for (int i = 0; i < signature1.getNumberOfFeatures(); i++) {
            for (int j = 0; j < signature2.getNumberOfFeatures(); j++) {
                double dist = signature1.getFeatures()[i]
                        .groundDist(signature2.getFeatures()[j]);
                assert (dist >= 0);
                C.get(i).set(j + signature1.getNumberOfFeatures(), dist);
                C.get(j + signature1.getNumberOfFeatures()).set(i, dist);
            }
        }

        return emdHat(P, Q, C, extraMassPenalty);
    }
    

    static private long emdHatImplLongLongInt(Vector<Long> Pc, Vector<Long> Qc,
            Vector<Vector<Long>> C, long extraMassPenalty) {

        int N = Pc.size();
        assert (Qc.size() == N);

        // Ensuring that the supplier - P, have more mass.
        // Note that we assume here that C is symmetric
        Vector<Long> P;
        Vector<Long> Q;
        long absDiffSumPSumQ;
        long sumP = 0;
        long sumQ = 0;
        for (int i = 0; i < N; i++)
            sumP += Pc.get(i);
        for (int i = 0; i < N; i++)
            sumQ += Qc.get(i);
        if (sumQ > sumP) {
            P = Qc;
            Q = Pc;
            absDiffSumPSumQ = sumQ - sumP;
        } else {
            P = Pc;
            Q = Qc;
            absDiffSumPSumQ = sumP - sumQ;
        }

        // creating the b vector that contains all vertexes
        Vector<Long> b = new Vector<Long>();
        for (int i = 0; i < 2 * N + 2; i++) {
            b.add(0l);
        }
        int THRESHOLD_NODE = 2 * N;
        int ARTIFICIAL_NODE = 2 * N + 1; // need to be last !
        for (int i = 0; i < N; i++) {
            b.set(i, P.get(i));
        }
        for (int i = N; i < 2 * N; i++) {
            b.set(i, Q.get(i - N));
        }

        // remark*) I put here a deficit of the extra mass, as mass that flows
        // to the threshold node
        // can be absorbed from all sources with cost zero (this is in reverse
        // order from the paper,
        // where incoming edges to the threshold node had the cost of the
        // threshold and outgoing
        // edges had the cost of zero)
        // This also makes sum of b zero.
        b.set(THRESHOLD_NODE, -absDiffSumPSumQ);
        b.set(ARTIFICIAL_NODE, 0l);

        long maxC = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                assert (C.get(i).get(j) >= 0);
                if (C.get(i).get(j) > maxC)
                    maxC = C.get(i).get(j);
            }
        }
        if (extraMassPenalty == -1)
            extraMassPenalty = maxC;

        Set<Integer> sourcesThatFlowNotOnlyToThresh = new HashSet<Integer>();
        Set<Integer> sinksThatGetFlowNotOnlyFromThresh = new HashSet<Integer>();
        long preFlowCost = 0;

        // regular edges between sinks and sources without threshold edges
        Vector<List<Edge>> c = new Vector<List<Edge>>();
        for (int i = 0; i < b.size(); i++) {
            c.add(new LinkedList<Edge>());
        }
        for (int i = 0; i < N; i++) {
            if (b.get(i) == 0)
                continue;
            for (int j = 0; j < N; j++) {
                if (b.get(j + N) == 0)
                    continue;
                if (C.get(i).get(j) == maxC)
                    continue;
                c.get(i).add(new Edge(j + N, C.get(i).get(j)));
            }
        }

        // checking which are not isolated
        for (int i = 0; i < N; i++) {
            if (b.get(i) == 0)
                continue;
            for (int j = 0; j < N; j++) {
                if (b.get(j + N) == 0)
                    continue;
                if (C.get(i).get(j) == maxC)
                    continue;
                sourcesThatFlowNotOnlyToThresh.add(i);
                sinksThatGetFlowNotOnlyFromThresh.add(j + N);
            }
        }

        // converting all sinks to negative
        for (int i = N; i < 2 * N; i++) {
            b.set(i, -b.get(i));
        }

        // add edges from/to threshold node,
        // note that costs are reversed to the paper (see also remark* above)
        // It is important that it will be this way because of remark* above.
        for (int i = 0; i < N; ++i) {
            c.get(i).add(new Edge(THRESHOLD_NODE, 0));
        }
        for (int j = 0; j < N; ++j) {
            c.get(THRESHOLD_NODE).add(new Edge(j + N, maxC));
        }

        // artificial arcs - Note the restriction that only one edge i,j is
        // artificial so I ignore it...
        for (int i = 0; i < ARTIFICIAL_NODE; i++) {
            c.get(i).add(new Edge(ARTIFICIAL_NODE, maxC + 1));
            c.get(ARTIFICIAL_NODE).add(new Edge(i, maxC + 1));
        }

        // remove nodes with supply demand of 0
        // and vertexes that are connected only to the
        // threshold vertex
        int currentNodeName = 0;
        // Note here it should be vector<int> and not vector<int>
        // as I'm using -1 as a special flag !!!
        int REMOVE_NODE_FLAG = -1;
        Vector<Integer> nodesNewNames = new Vector<Integer>();
        Vector<Integer> nodesOldNames = new Vector<Integer>();
        for (int i = 0; i < b.size(); i++) {
            nodesNewNames.add(REMOVE_NODE_FLAG);
            nodesOldNames.add(0);
        }
        for (int i = 0; i < N * 2; i++) {
            if (b.get(i) != 0) {
                if (sourcesThatFlowNotOnlyToThresh.contains(i)
                        || sinksThatGetFlowNotOnlyFromThresh.contains(i)) {
                    nodesNewNames.set(i, currentNodeName);
                    nodesOldNames.add(i);
                    currentNodeName++;
                } else {
                    if (i >= N) {
                        preFlowCost -= (b.get(i) * maxC);
                    }
                    b.set(THRESHOLD_NODE, b.get(THRESHOLD_NODE) + b.get(i)); // add mass(i<N) or deficit (i>=N)
                }
            }
        }
        nodesNewNames.set(THRESHOLD_NODE, currentNodeName);
        nodesOldNames.add(THRESHOLD_NODE);
        currentNodeName++;
        nodesNewNames.set(ARTIFICIAL_NODE, currentNodeName);
        nodesOldNames.add(ARTIFICIAL_NODE);
        currentNodeName++;

        Vector<Long> bb = new Vector<Long>();
        for (int i = 0; i < currentNodeName; i++) {
            bb.add(0l);
        }
        int j = 0;
        for (int i = 0; i < b.size(); i++) {
            if (nodesNewNames.get(i) != REMOVE_NODE_FLAG) {
                bb.set(j, b.get(i));
                j++;
            }
        }

        Vector<List<Edge>> cc = new Vector<List<Edge>>();
        for (int i = 0; i < bb.size(); i++) {
            cc.add(new LinkedList<Edge>());
        }
        for (int i = 0; i < c.size(); i++) {
            if (nodesNewNames.get(i) == REMOVE_NODE_FLAG)
                continue;
            for (Edge it : c.get(i)) {
                if (nodesNewNames.get(it._to) != REMOVE_NODE_FLAG) {
                    cc.get(nodesNewNames.get(i)).add(
                            new Edge(nodesNewNames.get(it._to), it._cost));
                }
            }
        }

        MinCostFlow mcf = new MinCostFlow();

        long myDist;

        Vector<List<Edge0>> flows = new Vector<List<Edge0>>(bb.size());
        for (int i = 0; i < bb.size(); i++) {
            flows.add(new LinkedList<Edge0>());
        }

        long mcfDist = mcf.compute(bb, cc, flows);

        myDist = preFlowCost + // pre-flowing on cases where it was possible
                mcfDist + // solution of the transportation problem
                (absDiffSumPSumQ * extraMassPenalty); // emd-hat extra mass penalty

        return myDist;
    }

    static private double emdHat(Vector<Double> P, Vector<Double> Q, Vector<Vector<Double>> C,
            double extraMassPenalty) {

        // This condition should hold:
        // ( 2^(sizeof(CONVERT_TO_T*8)) >= ( MULT_FACTOR^2 )
        // Note that it can be problematic to check it because
        // of overflow problems. I simply checked it with Linux calc
        // which has arbitrary precision.
        double MULT_FACTOR = 1000000;

        // Constructing the input
        int N = P.size();
        Vector<Long> iP = new Vector<Long>();
        Vector<Long> iQ = new Vector<Long>();
        Vector<Vector<Long>> iC = new Vector<Vector<Long>>();
        for (int i = 0; i < N; i++) {
            iP.add(0l);
            iQ.add(0l);
            Vector<Long> vec = new Vector<Long>();
            for (int j = 0; j < N; j++) {
                vec.add(0l);
            }
            iC.add(vec);
        }

        // Converting to CONVERT_TO_T
        double sumP = 0.0;
        double sumQ = 0.0;
        double maxC = C.get(0).get(0);
        for (int i = 0; i < N; i++) {
            sumP += P.get(i);
            sumQ += Q.get(i);
            for (int j = 0; j < N; j++) {
                if (C.get(i).get(j) > maxC)
                    maxC = C.get(i).get(j);
            }
        }
        double minSum = Math.min(sumP, sumQ);
        double maxSum = Math.max(sumP, sumQ);
        double PQnormFactor = MULT_FACTOR / maxSum;
        double CnormFactor = MULT_FACTOR / maxC;
        for (int i = 0; i < N; i++) {
            iP.set(i, (long) (Math.floor(P.get(i) * PQnormFactor + 0.5)));
            iQ.set(i, (long) (Math.floor(Q.get(i) * PQnormFactor + 0.5)));
            for (int j = 0; j < N; j++) {
                iC.get(i)
                        .set(j,
                                (long) (Math.floor(C.get(i).get(j)
                                        * CnormFactor + 0.5)));
            }
        }

        // computing distance without extra mass penalty
        double dist = emdHatImplLongLongInt(iP, iQ, iC, 0);
        // unnormalize
        dist = dist / PQnormFactor;
        dist = dist / CnormFactor;

        // adding extra mass penalty
        if (extraMassPenalty == -1)
            extraMassPenalty = maxC;
        dist += (maxSum - minSum) * extraMassPenalty;
        
        return dist;
    }
}
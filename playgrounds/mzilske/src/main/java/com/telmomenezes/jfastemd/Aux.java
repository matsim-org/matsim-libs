package com.telmomenezes.jfastemd;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;


/**
 * @author Telmo Menezes (telmo@telmomenezes.com)
 *
 */
class Edge {
    Edge(int to, long cost) {
        _to = to;
        _cost = cost;
    }

    int _to;
    long _cost;
}

class Edge0 {
    Edge0(int to, long cost, long flow) {
        _to = to;
        _cost = cost;
        _flow = flow;
    }

    int _to;
    long _cost;
    long _flow;
}

class Edge1 {
    Edge1(int to, long reduced_cost) {
        _to = to;
        _reduced_cost = reduced_cost;
    }

    int _to;
    long _reduced_cost;
}

class Edge2 {
    Edge2(int to, long reduced_cost, long residual_capacity) {
        _to = to;
        _reduced_cost = reduced_cost;
        _residual_capacity = residual_capacity;
    }

    int _to;
    long _reduced_cost;
    long _residual_capacity;
}

class Edge3 {
    Edge3() {
        _to = 0;
        _dist = 0;
    }

    Edge3(int to, long dist) {
        _to = to;
        _dist = dist;
    }

    int _to;
    long _dist;
}

class MinCostFlow {

    int numNodes;
    Vector<Integer> nodesToQ;

    // e - supply(positive) and demand(negative).
    // c[i] - edges that goes from node i. first is the second nod
    // x - the flow is returned in it
    long compute(Vector<Long> e, Vector<List<Edge>> c, Vector<List<Edge0>> x) {
        assert (e.size() == c.size());
        assert (x.size() == c.size());

        numNodes = e.size();
        nodesToQ = new Vector<Integer>();
        for (int i = 0; i < numNodes; i++) {
            nodesToQ.add(0);
        }

        // init flow
        for (int from = 0; from < numNodes; ++from) {
            for (Edge it : c.get(from)) {
                x.get(from).add(new Edge0(it._to, it._cost, 0));
                x.get(it._to).add(new Edge0(from, -it._cost, 0));
            }
        }

        // reduced costs for forward edges (c[i,j]-pi[i]+pi[j])
        // Note that for forward edges the residual capacity is infinity
        Vector<List<Edge1>> rCostForward = new Vector<List<Edge1>>();
        for (int i = 0; i < numNodes; i++) {
            rCostForward.add(new LinkedList<Edge1>());
        }
        for (int from = 0; from < numNodes; ++from) {
            for (Edge it : c.get(from)) {
                rCostForward.get(from).add(new Edge1(it._to, it._cost));
            }
        }

        // reduced costs and capacity for backward edges
        // (c[j,i]-pi[j]+pi[i])
        // Since the flow at the beginning is 0, the residual capacity is
        // also zero
        Vector<List<Edge2>> rCostCapBackward = new Vector<List<Edge2>>();
        for (int i = 0; i < numNodes; i++) {
            rCostCapBackward.add(new LinkedList<Edge2>());
        }
        for (int from = 0; from < numNodes; ++from) {
            for (Edge it : c.get(from)) {
                rCostCapBackward.get(it._to).add(
                        new Edge2(from, -it._cost, 0));
            }
        }

        // Max supply TODO:demand?, given U?, optimization-> min out of
        // demand,supply
        long U = 0;
        for (int i = 0; i < numNodes; i++) {
            if (e.get(i) > U)
                U = e.get(i);
        }
        long delta = (long) (Math.pow(2.0,
                Math.ceil(Math.log((double) (U)) / Math.log(2.0))));

        Vector<Long> d = new Vector<Long>();
        Vector<Integer> prev = new Vector<Integer>();
        for (int i = 0; i < numNodes; i++) {
            d.add(0l);
            prev.add(0);
        }
        delta = 1;
        while (true) { // until we break when S or T is empty
            long maxSupply = 0;
            int k = 0;
            for (int i = 0; i < numNodes; i++) {
                if (e.get(i) > 0) {
                    if (maxSupply < e.get(i)) {
                        maxSupply = e.get(i);
                        k = i;
                    }
                }
            }
            if (maxSupply == 0)
                break;
            delta = maxSupply;

            int[] l = new int[1];
            computeShortestPath(d, prev, k, rCostForward, rCostCapBackward,
                    e, l);

            // find delta (minimum on the path from k to l)
            // delta= e[k];
            // if (-e[l]<delta) delta= e[k];
            int to = l[0];
            do {
                int from = prev.get(to);
                assert (from != to);

                // residual
                int itccb = 0;
                while ((itccb < rCostCapBackward.get(from).size())
                        && (rCostCapBackward.get(from).get(itccb)._to != to)) {
                    itccb++;
                }
                if (itccb < rCostCapBackward.get(from).size()) {
                    if (rCostCapBackward.get(from).get(itccb)._residual_capacity < delta)
                        delta = rCostCapBackward.get(from).get(itccb)._residual_capacity;
                }

                to = from;
            } while (to != k);

            // augment delta flow from k to l (backwards actually...)
            to = l[0];
            do {
                int from = prev.get(to);
                assert (from != to);

                // TODO - might do here O(n) can be done in O(1)
                int itx = 0;
                while (x.get(from).get(itx)._to != to) {
                    itx++;
                }
                x.get(from).get(itx)._flow += delta;

                // update residual for backward edges
                int itccb = 0;
                while ((itccb < rCostCapBackward.get(to).size())
                        && (rCostCapBackward.get(to).get(itccb)._to != from)) {
                    itccb++;
                }
                if (itccb < rCostCapBackward.get(to).size()) {
                    rCostCapBackward.get(to).get(itccb)._residual_capacity += delta;
                }
                itccb = 0;
                while ((itccb < rCostCapBackward.get(from).size())
                        && (rCostCapBackward.get(from).get(itccb)._to != to)) {
                    itccb++;
                }
                if (itccb < rCostCapBackward.get(from).size()) {
                    rCostCapBackward.get(from).get(itccb)._residual_capacity -= delta;
                }

                // update e
                e.set(to, e.get(to) + delta);
                e.set(from, e.get(from) - delta);

                to = from;
            } while (to != k);
        }

        // compute distance from x
        long dist = 0;
        for (int from = 0; from < numNodes; from++) {
            for (Edge0 it : x.get(from)) {
                dist += (it._cost * it._flow);
            }
        }
        return dist;
    }

    void computeShortestPath(Vector<Long> d, Vector<Integer> prev,
            int from, Vector<List<Edge1>> costForward,
            Vector<List<Edge2>> costBackward, Vector<Long> e, int[] l) {
        // Making heap (all inf except 0, so we are saving comparisons...)
        Vector<Edge3> Q = new Vector<Edge3>();
        for (int i = 0; i < numNodes; i++) {
            Q.add(new Edge3());
        }

        Q.get(0)._to = from;
        nodesToQ.set(from, 0);
        Q.get(0)._dist = 0;

        int j = 1;
        // TODO: both of these into a function?
        for (int i = 0; i < from; ++i) {
            Q.get(j)._to = i;
            nodesToQ.set(i, j);
            Q.get(j)._dist = Long.MAX_VALUE;
            j++;
        }

        for (int i = from + 1; i < numNodes; i++) {
            Q.get(j)._to = i;
            nodesToQ.set(i, j);
            Q.get(j)._dist = Long.MAX_VALUE;
            j++;
        }

        Vector<Boolean> finalNodesFlg = new Vector<Boolean>();
        for (int i = 0; i < numNodes; i++) {
            finalNodesFlg.add(false);
        }
        do {
            int u = Q.get(0)._to;

            d.set(u, Q.get(0)._dist); // final distance
            finalNodesFlg.set(u, true);
            if (e.get(u) < 0) {
                l[0] = u;
                break;
            }

            heapRemoveFirst(Q, nodesToQ);

            // neighbors of u
            for (Edge1 it : costForward.get(u)) {
                assert (it._reduced_cost >= 0);
                long alt = d.get(u) + it._reduced_cost;
                int v = it._to;
                if ((nodesToQ.get(v) < Q.size())
                        && (alt < Q.get(nodesToQ.get(v))._dist)) {
                    heapDecreaseKey(Q, nodesToQ, v, alt);
                    prev.set(v, u);
                }
            }
            for (Edge2 it : costBackward.get(u)) {
                if (it._residual_capacity > 0) {
                    assert (it._reduced_cost >= 0);
                    long alt = d.get(u) + it._reduced_cost;
                    int v = it._to;
                    if ((nodesToQ.get(v) < Q.size())
                            && (alt < Q.get(nodesToQ.get(v))._dist)) {
                        heapDecreaseKey(Q, nodesToQ, v, alt);
                        prev.set(v, u);
                    }
                }
            }

        } while (Q.size() > 0);

        for (int _from = 0; _from < numNodes; ++_from) {
            for (Edge1 it : costForward.get(_from)) {
                if (finalNodesFlg.get(_from)) {
                    it._reduced_cost += d.get(_from) - d.get(l[0]);
                }
                if (finalNodesFlg.get(it._to)) {
                    it._reduced_cost -= d.get(it._to) - d.get(l[0]);
                }
            }
        }

        // reduced costs and capacity for backward edges
        // (c[j,i]-pi[j]+pi[i])
        for (int _from = 0; _from < numNodes; ++_from) {
            for (Edge2 it : costBackward.get(_from)) {
                if (finalNodesFlg.get(_from)) {
                    it._reduced_cost += d.get(_from) - d.get(l[0]);
                }
                if (finalNodesFlg.get(it._to)) {
                    it._reduced_cost -= d.get(it._to) - d.get(l[0]);
                }
            }
        }
    }

    void heapDecreaseKey(Vector<Edge3> Q, Vector<Integer> nodes_to_Q,
            int v, long alt) {
        int i = nodes_to_Q.get(v);
        Q.get(i)._dist = alt;
        while (i > 0 && Q.get(PARENT(i))._dist > Q.get(i)._dist) {
            swapHeap(Q, nodes_to_Q, i, PARENT(i));
            i = PARENT(i);
        }
    }

    void heapRemoveFirst(Vector<Edge3> Q, Vector<Integer> nodes_to_Q) {
        swapHeap(Q, nodes_to_Q, 0, Q.size() - 1);
        Q.remove(Q.size() - 1);
        heapify(Q, nodes_to_Q, 0);
    }

    void heapify(Vector<Edge3> Q, Vector<Integer> nodes_to_Q, int i) {
        do {
            // TODO: change to loop
            int l = LEFT(i);
            int r = RIGHT(i);
            int smallest;
            if ((l < Q.size()) && (Q.get(l)._dist < Q.get(i)._dist)) {
                smallest = l;
            } else {
                smallest = i;
            }
            if ((r < Q.size()) && (Q.get(r)._dist < Q.get(smallest)._dist)) {
                smallest = r;
            }

            if (smallest == i)
                return;

            swapHeap(Q, nodes_to_Q, i, smallest);
            i = smallest;

        } while (true);
    }

    void swapHeap(Vector<Edge3> Q, Vector<Integer> nodesToQ, int i, int j) {
        Edge3 tmp = Q.get(i);
        Q.set(i, Q.get(j));
        Q.set(j, tmp);
        nodesToQ.set(Q.get(j)._to, j);
        nodesToQ.set(Q.get(i)._to, i);
    }

    int LEFT(int i) {
        return 2 * (i + 1) - 1;
    }

    int RIGHT(int i) {
        return 2 * (i + 1); // 2 * (i + 1) + 1 - 1
    }

    int PARENT(int i) {
        return (i - 1) / 2;
    }
}
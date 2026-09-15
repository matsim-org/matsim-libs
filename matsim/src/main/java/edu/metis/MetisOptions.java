package edu.metis;

/**
 * Class holding options for the METIS library.
 */
public final class MetisOptions {

    ObjType objType = ObjType.CUT;
    CoarseningType coarseningType = CoarseningType.SHEM;
    InitialPartitionType initialPartitionType = InitialPartitionType.GROW;
    RefinementType refinementType = RefinementType.GREEDY;
    int seed = 42;

    private MetisOptions() {
    }

    public static MetisOptions of() {
        return new MetisOptions();
    }

    public MetisOptions setObjType(ObjType objType) {
        this.objType = objType;
        return this;
    }

    public MetisOptions setCoarseningType(CoarseningType coarseningType) {
        this.coarseningType = coarseningType;
        return this;
    }

    public MetisOptions setInitialPartitionType(InitialPartitionType initialPartitionType) {
        this.initialPartitionType = initialPartitionType;
        return this;
    }

    public MetisOptions setRefinementType(RefinementType refinementType) {
        this.refinementType = refinementType;
        return this;
    }

    public MetisOptions setSeed(int seed) {
        this.seed = seed;
        return this;
    }

    /**
     * Specifies the type of objective
     */
    public enum ObjType {
        /**
         * Edge-cut minimization.
         */
        CUT(metis_h.METIS_OBJTYPE_CUT()),
        /**
         * Total communication volume minimization.
         */
        VOL(metis_h.METIS_OBJTYPE_VOL());

        final int value;

        ObjType(int value) {
            this.value = value;
        }
    }

    /**
     * Specifies the matching scheme to be used during coarsening.
     */
    public enum CoarseningType {

        /**
         * Random matching.
         */
        RM(metis_h.METIS_CTYPE_RM()),
        /**
         * Sorted heavy-edge matching.
         */
        SHEM(metis_h.METIS_CTYPE_SHEM());

        final int value;

        CoarseningType(int value) {
            this.value = value;
        }
    }

    /**
     * Determines the algorithm used during initial partitioning.
     */
    public enum InitialPartitionType {
        /**
         * Grows a bisection using a greedy strategy.
         */
        GROW(metis_h.METIS_IPTYPE_GROW()),
        /**
         * Computes a bisection at random followed by a refinement.
         */
        RANDOM(metis_h.METIS_IPTYPE_RANDOM()),

        /**
         * Derives a separator from an edge cut.
         */
        EDGE(metis_h.METIS_IPTYPE_EDGE()),

        /**
         * Grow a bisection using a greedy node-based strategy.
         */
        NODE(metis_h.METIS_IPTYPE_NODE());

        final int value;

        InitialPartitionType(int value) {
            this.value = value;
        }
    }

    /**
     * Determines the algorithm used for refinement.
     */
    public enum RefinementType {

        /**
         * FM-based cut refinement.
         */
        FM(metis_h.METIS_RTYPE_FM()),
        /**
         * Greedy-based cut and volume refinement.
         */
        GREEDY(metis_h.METIS_RTYPE_GREEDY()),
        /**
         * Two-sided node FM refinement.
         */
        SEP2SIDED(metis_h.METIS_RTYPE_SEP2SIDED()),
        /**
         * One-sided node FM refinement.
         */
        SEP1SIDED(metis_h.METIS_RTYPE_SEP1SIDED());

        final int value;

        RefinementType(int value) {
            this.value = value;
        }
    }

}

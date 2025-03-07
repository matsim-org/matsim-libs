package org.matsim.contrib.dvrp.load;

import com.google.common.base.Preconditions;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class IntegersLoad implements DvrpLoad {
    private final int[] values;

    IntegersLoad(int[] values) {
        this.values = values;
    }

    @Override
    public DvrpLoad add(DvrpLoad other) {
        IntegersLoad otherLoad = check(other);

        int[] result = new int[values.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = values[i] + otherLoad.values[i];
        }

        return new IntegersLoad(result);
    }

    @Override
    public DvrpLoad subtract(DvrpLoad other) {
        IntegersLoad otherLoad = check(other);

        int[] result = new int[values.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = values[i] - otherLoad.values[i];
        }

        return new IntegersLoad(result);
    }

    @Override
    public boolean fitsIn(DvrpLoad other) {
        IntegersLoad otherLoad = check(other);

        int[] result = new int[values.length];
        for (int i = 0; i < result.length; i++) {
            if (values[i] > otherLoad.values[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < values.length; i++) {
            if (values[i] > 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Number getElement(int i) {
        Preconditions.checkArgument(i >= 0 && i < values.length);
        return values[i];
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof IntegersLoad otherLoad) {
            if (values.length == otherLoad.values.length) {
                for (int i = 0; i < values.length; i++) {
                    if (values[i] != otherLoad.values[i]) {
                        return false;
                    }
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;

        for (int k = 0; k < values.length; k++) {
            hashCode += k * 1000 + values[k];
        }

        return hashCode;
    }

    private IntegersLoad check(DvrpLoad load) {
        if (load instanceof IntegersLoad otherLoad) {
            Preconditions.checkArgument(values.length == otherLoad.values.length,
                    "Passed load does not have the correct number of dimensions");
            return otherLoad;
        }

        throw new IllegalStateException("Passed load is not an IntegersLoad");
    }
}

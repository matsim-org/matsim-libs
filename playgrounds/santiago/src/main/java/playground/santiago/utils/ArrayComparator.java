package playground.santiago.utils;

import java.util.Comparator;

public class ArrayComparator implements Comparator<double[]>{

    private final int columnToSort;
    private final boolean ascending;

    public ArrayComparator(int columnToSort, boolean ascending) {
        this.columnToSort = columnToSort;
        this.ascending = ascending;
    }

    public int compare(double[] c1, double[] c2) {
        int cmp = Double.compare(c1[columnToSort], c2[columnToSort]);
        return ascending ? cmp : -cmp;
    }
}

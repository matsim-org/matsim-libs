// code by jph
package playground.clruch.net;

import java.io.File;

public class IterationFolder {
    /**
     * for instance, itDir can be
     * /media/datahaki/data/ethz/2017_03_09_Sioux_HU/output/simobj/it.02
     */
    public final File itDir;
    public final StorageSupplier storageSupplier;

    public IterationFolder(File itDir, StorageUtils storageUtils) {
        this.itDir = itDir;
        storageSupplier = new StorageSupplier(storageUtils.getFrom(itDir));
    }

    @Override
    public String toString() {
        String string = itDir.getName();
        String digits = string.substring(string.length() - 2, string.length());
        return "" + Integer.parseInt(digits);
    }
}

package playground.clruch.net;

import java.io.File;

public class IterationStorage {
    public final File dir;

    public IterationStorage(File dir) {
        this.dir = dir;
    }

    @Override
    public String toString() {
        String string = dir.getName();
        String digits = string.substring(string.length() - 2, string.length());
        return "" + Integer.parseInt(digits);
    }
}

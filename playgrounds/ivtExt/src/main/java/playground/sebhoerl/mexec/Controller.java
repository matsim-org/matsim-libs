package playground.sebhoerl.mexec;

import java.util.Collection;

public interface Controller {
    String getId();

    String getClassName();
    String getClassPath();

    void save();
}

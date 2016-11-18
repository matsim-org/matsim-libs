package playground.sebhoerl.mexec.local;

import playground.sebhoerl.mexec.Controller;
import playground.sebhoerl.mexec.data.ControllerData;

public class LocalController implements Controller {
    final private ControllerData data;
    final private LocalEnvironment environment;

    public LocalController(LocalEnvironment environment, ControllerData data) {
        this.data = data;
        this.environment = environment;
    }

    @Override
    public String getId() {
        return data.id;
    }

    @Override
    public String getClassName() {
        return data.className;
    }

    @Override
    public String getClassPath() {
        return data.classPath;
    }

    @Override
    public void save() {
        environment.save();
    }
}

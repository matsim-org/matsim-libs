package playground.sebhoerl.mexec.local;

import playground.sebhoerl.mexec.Controller;
import playground.sebhoerl.mexec.data.ControllerData;
import playground.sebhoerl.mexec.generic.AbstractController;

public class LocalController extends AbstractController implements Controller {
    final private LocalEnvironment environment;

    public LocalController(LocalEnvironment environment, ControllerData data) {
        super(data);
        this.environment = environment;
    }

    @Override
    public void save() {
        environment.save();
    }
}

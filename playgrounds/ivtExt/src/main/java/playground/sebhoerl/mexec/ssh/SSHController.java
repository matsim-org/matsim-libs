package playground.sebhoerl.mexec.ssh;

import playground.sebhoerl.mexec.Controller;
import playground.sebhoerl.mexec.data.ControllerData;
import playground.sebhoerl.mexec.generic.AbstractController;

public class SSHController extends AbstractController implements Controller {
    final private SSHEnvironment environment;

    public SSHController(SSHEnvironment environment, ControllerData data) {
        super(data);
        this.environment = environment;
    }

    @Override
    public void save() {
        environment.save();
    }
}

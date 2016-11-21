package playground.sebhoerl.mexec.generic;

import playground.sebhoerl.mexec.Controller;
import playground.sebhoerl.mexec.Environment;
import playground.sebhoerl.mexec.data.ControllerData;
import playground.sebhoerl.mexec.local.LocalEnvironment;

public abstract class AbstractController<DataType extends ControllerData> implements Controller {
    final protected DataType data;

    public AbstractController(DataType data) {
        this.data = data;
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
}

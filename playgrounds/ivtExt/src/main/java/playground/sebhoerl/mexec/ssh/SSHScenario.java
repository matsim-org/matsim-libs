package playground.sebhoerl.mexec.ssh;

import playground.sebhoerl.mexec.ConfigUtils;
import playground.sebhoerl.mexec.Scenario;
import playground.sebhoerl.mexec.data.ScenarioData;
import playground.sebhoerl.mexec.generic.AbstractScenario;
import playground.sebhoerl.mexec.placeholders.PlaceholderUtils;
import playground.sebhoerl.mexec.ssh.utils.SSHFile;
import playground.sebhoerl.mexec.ssh.utils.SSHUtils;

import java.io.IOException;
import java.util.Set;

public class SSHScenario extends AbstractScenario implements Scenario {
    final private SSHEnvironment environment;
    final private SSHFile path;
    final private SSHUtils ssh;

    public SSHScenario(SSHEnvironment environment, ScenarioData data, SSHFile path, SSHUtils ssh) {
        super(data);
        this.environment = environment;
        this.path = path;
        this.ssh = ssh;
    }

    @Override
    public Set<String> getAvailablePlaceholders() {
        SSHFile configPath = new SSHFile(path, data.mainConfigPath);

        try {
            return PlaceholderUtils.findPlaceholdersInConfig(ConfigUtils.loadConfig(ssh.read(configPath)));
        } catch (IOException e) {
            throw new RuntimeException("Could not open file " + configPath);
        }
    }

    @Override
    public void save() {
        environment.save();
    }
}

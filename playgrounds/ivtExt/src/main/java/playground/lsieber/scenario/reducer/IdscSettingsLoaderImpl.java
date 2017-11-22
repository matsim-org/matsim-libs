package playground.lsieber.scenario.reducer;

import java.io.IOException;

import org.matsim.core.config.Config;

public class IdscSettingsLoaderImpl extends IdscSettingsLoader {

    public IdscSettingsLoaderImpl() throws IOException {

    }

    @Override
    public Config getConfig() {
        Config hConfig = super.getConfig();
        hConfig.getClass();
        return hConfig;
    }

}

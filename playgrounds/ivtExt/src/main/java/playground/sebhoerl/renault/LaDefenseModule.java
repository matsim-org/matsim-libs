package playground.sebhoerl.renault;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class LaDefenseModule extends AbstractModule {
    static public final String LADEFENSE = "la_defense";

    @Override
    public void install() {}

    @Provides @Singleton @Named(LaDefenseModule.LADEFENSE)
    public Set<Id<Node>> provideLaDefenseNodeFilter(Config mainConfig, LaDefenseConfigGroup ldConfig) {
        File basePath = new File(mainConfig.getContext().getPath()).getParentFile();
        String ldPath = ldConfig.getNodeFilterInputPath();

        HashSet<Id<Node>> filter = new HashSet<>();

        if (ldPath == null) {
            Logger.getLogger(LaDefenseModule.class).warn("No node filter for La Defense defined!");
        } else {
            File sourcePath = new File(basePath, ldPath);

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sourcePath)));

                String line = null;
                while ((line = reader.readLine()) != null) {
                    filter.add(Id.createNodeId(line.trim()));
                }

            } catch (IOException e) {
                throw new RuntimeException("Could not open La defense filter file");
            }
        }

        return filter;
    }
}

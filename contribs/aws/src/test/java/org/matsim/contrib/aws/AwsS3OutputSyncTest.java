package org.matsim.contrib.aws;

import com.google.inject.Singleton;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.nio.file.Files;

/**
 * @author nkuehnel / MOIA
 */
public class AwsS3OutputSyncTest {

    // set to existing bucket
    private static final String BUCKET = "...";
    private static final String PREFIX = "matsim-test";
    private static final Region REGION = Region.EU_CENTRAL_1;


    @RegisterExtension
    private MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    @Disabled
    public void testSync() {



        final Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));

        AwsConfig awsConfig = new AwsConfig();
        awsConfig.setS3Bucket(BUCKET);
        awsConfig.setOutputS3KeyPrefix(PREFIX);
        awsConfig.setS3BucketRegion(REGION.toString());
        config.addModule(awsConfig);

        ControllerConfigGroup controllerConfigGroup = config.controller();
        controllerConfigGroup.setLastIteration(0);
        try {
            controllerConfigGroup.setOutputDirectory(Files.createTempDirectory("matsimtest").toAbsolutePath().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Controler controler = new Controler( config );

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addControlerListenerBinding().toProvider(() -> new AwsS3OutputSync(controllerConfigGroup, awsConfig)).in(Singleton.class);
            }
        });
        controler.run();
    }
}

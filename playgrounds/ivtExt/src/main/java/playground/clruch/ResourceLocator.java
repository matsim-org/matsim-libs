package playground.clruch;

import java.io.File;

import playground.clruch.utils.GlobalAssert;

public class ResourceLocator {

    public static final ResourceLocator INSTANCE = new ResourceLocator();

    public File gheatDirectory;

    private ResourceLocator() {
        String username = System.getProperty("user.name");
        if (username.equals("datahaki")) {
            setUpDatahaki();
        }
        if (username.equals("clruch")) {
            setUpDatahaki();
        }
        if (username.equals("joel")) {
            setUpDatahaki();
        }
        if (username.equals("malbert")) {
            setUpDatahaki();
        }
        GlobalAssert.that(gheatDirectory.isDirectory());
    }

    private void setUpDatahaki() {
        gheatDirectory = new File("/home/datahaki/3rdparty/GHEAT-JAVA/JavaHeatMaps/heatmaps/src/main/resources/res/etc");
    }

    private void setUpClruch() {
        gheatDirectory = new File("/home/datahaki/3rdparty/GHEAT-JAVA/JavaHeatMaps/heatmaps/src/main/resources/res/etc");
    }

    private void setUpJoel() {
        gheatDirectory = new File("/home/datahaki/3rdparty/GHEAT-JAVA/JavaHeatMaps/heatmaps/src/main/resources/res/etc");
    }

    private void setUpMalbert() {
        gheatDirectory = new File("/home/datahaki/3rdparty/GHEAT-JAVA/JavaHeatMaps/heatmaps/src/main/resources/res/etc");
    }

}

package otherPackage;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

public class SupersonicOsmNetworkReaderBuilderTest {

  @Test
  public void testPublicApi() {

    SupersonicOsmNetworkReader reader =
        new SupersonicOsmNetworkReader.Builder()
            .setCoordinateTransformation(new IdentityTransformation())
            .build();

    assertNotNull(reader);
  }
}

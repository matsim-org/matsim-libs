package otherPackage;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

public class SupersonicOsmNetworkReaderBuilderTest {

	@Test
	void testPublicApi() {

		SupersonicOsmNetworkReader reader = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(new IdentityTransformation())
				.build();

		assertNotNull(reader);
	}
}

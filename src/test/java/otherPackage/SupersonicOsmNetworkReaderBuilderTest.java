package otherPackage;

import org.junit.Test;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.osmNetworkReader.SupersonicOsmNetworkReader;

import static org.junit.Assert.assertNotNull;

public class SupersonicOsmNetworkReaderBuilderTest {

	@Test
	public void testPublicApi() {

		var reader = SupersonicOsmNetworkReader.builder()
				.network(NetworkUtils.createNetwork())
				.coordinateTransformation(new IdentityTransformation())
				.build();

		assertNotNull(reader);
	}
}

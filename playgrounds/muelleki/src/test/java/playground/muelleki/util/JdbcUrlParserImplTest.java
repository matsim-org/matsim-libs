package playground.muelleki.util;

import junit.framework.TestCase;

public class JdbcUrlParserImplTest extends TestCase {
	public void testPostgresSetManually() {
		JdbcUrlParser up = new JdbcUrlParserImpl();
		up.setUrl("jdbc:postgresql://albion.ethz.ch:1234/sustaincity");
		assertEquals(up.getDbType(), "postgresql");
		assertEquals(up.getHost(), "albion.ethz.ch");
		assertEquals(up.getPort(), "1234");
		assertEquals(up.getDbName(), "sustaincity");
	}

	public void testPostgresSetByConstructor() {
		JdbcUrlParser up = new JdbcUrlParserImpl("jdbc:postgresql:sustaincity");
		assertEquals(up.getDbType(), "postgresql");
		assertEquals(up.getHost(), "localhost");
		assertEquals(up.getPort(), "5432");
		assertEquals(up.getDbName(), "sustaincity");
	}

	public void testUnknown() {
		JdbcUrlParser up = new JdbcUrlParserImpl("jdbc:unk:///");
		assertEquals(up.getDbType(), null);
		assertEquals(up.getHost(), null);
		assertEquals(up.getPort(), null);
		assertEquals(up.getDbName(), null);
	}
}

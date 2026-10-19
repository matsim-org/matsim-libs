package org.matsim.contrib.aws;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.spi.URLStreamHandlerProvider;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the SPI provider is correctly discovered via ServiceLoader.
 * Does not require AWS credentials.
 */
public class S3SpiDiscoveryTest {

    @Test
    void testProviderIsDiscoveredByServiceLoader() {
        ServiceLoader<URLStreamHandlerProvider> loader = ServiceLoader.load(URLStreamHandlerProvider.class);
        boolean found = false;
        for (URLStreamHandlerProvider provider : loader) {
            if (provider instanceof S3URLStreamHandlerProvider) {
                found = true;
                break;
            }
        }
        assertTrue(found, "S3URLStreamHandlerProvider should be discoverable via ServiceLoader");
    }

    @Test
    void testProviderReturnsHandlerForS3() {
        S3URLStreamHandlerProvider provider = new S3URLStreamHandlerProvider();
        assertNotNull(provider.createURLStreamHandler("s3"));
    }

    @Test
    void testProviderReturnsNullForOtherProtocols() {
        S3URLStreamHandlerProvider provider = new S3URLStreamHandlerProvider();
        assertNull(provider.createURLStreamHandler("http"));
        assertNull(provider.createURLStreamHandler("file"));
        assertNull(provider.createURLStreamHandler("ftp"));
    }

    @Test
    void testS3UrlIsParseable() throws Exception {
        URL url = new URL("s3://my-bucket/path/to/file.xml.gz");
        assertEquals("s3", url.getProtocol());
        assertEquals("my-bucket", url.getHost());
        assertEquals("/path/to/file.xml.gz", url.getPath());
    }
}
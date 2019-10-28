package org.matsim.contrib.signals.data;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Attempt to provide the readURL, readFile, readStream functionality in a consolidated way.  This may be more generally useful,
 * e.g. as AbstractMatsimXsdReader.  kai, nov'18
 */
public abstract class AbstractSignalsReader implements MatsimReader{
    private static final Logger log = Logger.getLogger( AbstractSignalsReader.class ) ;

    @Override
    public final void readFile( String filename ){
        log.info("starting unmarshalling " + filename);
        try ( InputStream stream = IOUtils.getInputStream(IOUtils.resolveFileOrResource(filename) )){
            readStream(stream);
        } catch ( IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public final void readURL( URL url ){
        throw new RuntimeException( "not implemented" );
    }

    public final void readStream( InputStream stream ) {
        read( new InputSource(stream) ) ;
    }

    abstract public void read( InputSource source ) ;
}

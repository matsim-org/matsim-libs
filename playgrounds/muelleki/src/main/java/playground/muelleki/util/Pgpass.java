/**
 *  Pgpass.java
 *  Adapted from http://svn.dcache.org/dCache/trunk/modules/dCache/diskCacheV111/util/Pgpass.java
 */

package playground.muelleki.util;

import  java.io.*;
import org.apache.log4j.Logger;

/**
 * @author  Vladimir Podstavkov
 */
public class Pgpass {

    private String _pwdfile;
	private Logger _log = Logger.getLogger(Pgpass.class);

    public Pgpass() {
        this("~/.pgpass");
    }

    public Pgpass(String pwdfile) {
        _pwdfile = pwdfile;
    }

    private static String process(String line, String hostname, String port, String database, String username) {
        if (line.charAt(0) != '#') {
            String[] sa = line.split(":", -1);
            if (sa[0].equals("*") || sa[0].equals(hostname) || (sa[0].equals("") && (hostname.equals("localhost") || hostname.equals("127.0.0.1"))))
                if (sa[1].equals("*") || sa[1].equals(port))
                    if (sa[2].equals("*") || sa[2].equals(database))
                        if (sa[3].equals("*") || sa[3].equals(username))
                            return sa[4];
        }
        return null;
    }

    public String getPgpass(String hostname, String port, String database, String username) {
        BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(_pwdfile));
		} catch (FileNotFoundException e) {
			_log.info(String.format("File %s not found", _pwdfile), e);
			return null;
		}
        String line, r = null;
        try {
			while ((line = in.readLine()) != null && r == null) {
			    r = process(line, hostname, port, database, username);
			}
	        in.close();
		} catch (IOException e) {
			_log.error(String.format("Error reading file %s", _pwdfile), e);
			return null;
		}
        return r;
    }

    public String getPgpass(String url, String username) {
    	JdbcUrlParser up = new JdbcUrlParserImpl(url);
    	if (!up.getDbType().equals("postgresql"))
    		return null;
        return getPgpass(up.getHost(), up.getPort(), up.getDbName(), username);
    }
}

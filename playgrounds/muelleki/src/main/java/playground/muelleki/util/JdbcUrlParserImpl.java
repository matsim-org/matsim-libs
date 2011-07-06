/**
 * Sequoia: Database clustering technology.
 * Copyright (C) 2006 Continuent, Inc.
 * Contact: sequoia@continuent.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * Initial developer(s): Emmanuel Cecchet.
 * Contributor(s): Kirill Müller ________.
 */
package playground.muelleki.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class defines a BasicUrlParser, which provides a default implementation
 * of the JdbcUrlParser interface.  
 * 
 * @author <a href="mailto:robert.hodges@continuent.com">Robert Hodges</a>
 * @version 1.0
 */
public class JdbcUrlParserImpl implements JdbcUrlParser {
    private String dbType;
    private String host;
    private String port;
    private String dbName;

    // Pattern and defaults for parsing mysql URLs. 
    private static final String MYSQL_PREFIX = "jdbc:mysql";
    private static Pattern mysqlPattern = Pattern
            .compile("jdbc:mysql://([a-zA-Z0-9_\\-.]+|)((:(\\d+))|)/([a-zA-Z][a-zA-Z0-9_\\-]*)(\\?.*)?");

    // Pattern and defaults for parsing PostgreSQL URLs.  
    private static final String POSTGRESQL_PREFIX = "jdbc:postgresql";
    private static Pattern postgresqlPattern = Pattern
            .compile("jdbc:postgresql:((//([a-zA-Z0-9_\\-.]+|\\[[a-fA-F0-9:]+])((:(\\d+))|))/|)([^\\s?]*).*$");

    /**
     * Creates a new <code>BasicUrlParser</code> object. 
     */
    public JdbcUrlParserImpl() {
    }

    /**
     * Creates a new <code>BasicUrlParser</code> object and sets its URL. 
     */
    public JdbcUrlParserImpl(String url) {
    	setUrl(url);
    }

    /**
     * {@inheritDoc}
     * @see org.continuent.sequoia.controller.backup.backupers.JdbcUrlParser#setUrl(java.lang.String)
     */
    public void setUrl(String url) {
        parse(url);
    }

    /**
     * Parses the URL using available patterns. 
     * 
     * @param url The URL to parse. 
     */
    protected void parse(String url) {
        // Assign defaults.  
        dbType = null;
        host = null;
        port = null;
        dbName = null;

        if (url.startsWith(MYSQL_PREFIX)) {
            dbType = "mysql";
            host = "localhost";
            port = "3306";

            Matcher matcher = mysqlPattern.matcher(url);
            if (matcher.matches()) {
                if (matcher.group(1) != null
                        && matcher.group(1).length() > 0)
                    host = matcher.group(1);
                if (matcher.group(4) != null
                        && matcher.group(4).length() > 0)
                    port = matcher.group(4);
                dbName = matcher.group(5);
            }
        } else if (url.startsWith(POSTGRESQL_PREFIX)) {
            dbType = "postgresql";
            host = "localhost";
            port = "5432";

            Matcher matcher = postgresqlPattern.matcher(url);
            if (matcher.matches()) {
                if (matcher.group(3) != null)
                    host = matcher.group(3);
                if (matcher.group(6) != null)
                    port = matcher.group(6);
                dbName = matcher.group(7);
            }
        } else {
        }
    }

    /**
     * {@inheritDoc}
     * @see org.continuent.sequoia.controller.backup.backupers.JdbcUrlParser#getDbName()
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * {@inheritDoc}
     * @see org.continuent.sequoia.controller.backup.backupers.JdbcUrlParser#getDbType()
     */
    public String getDbType() {
        return dbType;
    }

    /**
     * {@inheritDoc}
     * @see org.continuent.sequoia.controller.backup.backupers.JdbcUrlParser#getHost()
     */
    public String getHost() {
        return host;
    }

    /**
     * {@inheritDoc}
     * @see org.continuent.sequoia.controller.backup.backupers.JdbcUrlParser#getPort()
     */
    public String getPort() {
        return port;
    }
}

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

/**
 * This denotes a class used to parse JDBC URLs.  The parsing extracts key 
 * elements of the URL including the subprotocol, database, host, and port.  
 * Implementation should supply reasonable default values.  If no value can
 * be assigned to a property, a null should be assigned instead. 
 * 
 * @author <a href="mailto:robert.hodges@continuent.com">Robert Hodges</a>
 * @version 1.0
 */
public interface JdbcUrlParser {
    /**
     * Loads a URL to be parsed.  Setting this loads all getter values.
     *  
     * @param url JDBC URL that we would like to parse
     */
    public abstract void setUrl(String url);

    /**
     * Returns the database name.
     * 
     * @return Returns the dbName.
     */
    public abstract String getDbName();

    /**
     * Returns the database type (actually the JDBC subprotocol).
     * 
     * @return Returns the dbType.
     */
    public abstract String getDbType();

    /**
     * Returns the host name. 
     * 
     * @return Returns the host.
     */
    public abstract String getHost();

    /**
     * Returns the network port. 
     * 
     * @return Returns the port.
     */
    public abstract String getPort();
}

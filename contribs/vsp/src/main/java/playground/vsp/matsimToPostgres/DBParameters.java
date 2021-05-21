package playground.vsp.matsimToPostgres;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBParameters {
    private String host = null;
    private String databaseName = null;
    private String user = null;
    private String password = null;

    public DBParameters(String dbParametersXML){
        // Instantiate the Factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // parse XML file
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(dbParametersXML));
            doc.getDocumentElement().normalize();

            // create XPath
            XPathFactory xpathfactory = XPathFactory.newInstance();
            XPath xpath = xpathfactory.newXPath();

            // String host = (String) xpath.compile("//param[@name='host']/@value").evaluate(doc, XPathConstants.STRING);
            this.host = (String) xpath.compile("//param[@name='host']/@value").evaluate(doc, XPathConstants.STRING);
            this.databaseName = (String) xpath.compile("//param[@name='databaseName']/@value").evaluate(doc, XPathConstants.STRING);
            this.user = (String) xpath.compile("//param[@name='user']/@value").evaluate(doc, XPathConstants.STRING);
            this.password = (String) xpath.compile("//param[@name='password']/@value").evaluate(doc, XPathConstants.STRING);
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    public String getHost() {
        return host;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Connection createDBConnection() {

        // Instantiate the Factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        try {
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", password);

            String url = "jdbc:postgresql://" + host + "/" + databaseName;
            return DriverManager.getConnection(url, props);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }


    }
}

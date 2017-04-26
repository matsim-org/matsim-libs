package playground.sebhoerl.mexec.placeholders;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import playground.sebhoerl.mexec.Config;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.*;

public class PlaceholderUtils {
    public static Set<String> getPlaceholders(Parameter parameter) {
        Set<String> names = new HashSet<>();

        for (ParameterElement element : parameter.getElements()) {
            if (element instanceof PlaceholderElement) {
                names.add(((PlaceholderElement) element).getPlaceholderName());
            }
        }

        return names;
    }

    public static Set<String> findPlaceholdersInConfig(Config config) {
        Set<String> names = new HashSet<>();
        ParameterParser parser = new ParameterParser();

        config.traverse(new Config.Traverser() {
            @Override
            public void processParameter(String name, String value, Element element) {
                Parameter parameter = parser.parse(name, value);
                names.addAll(getPlaceholders(parameter));
            }
        });

        return names;
    }

    public static Config transformConfig(Config config, final Map<String, String> placeholders) {
        try {
            ParameterParser parser = new ParameterParser();

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactoryImpl.newInstance();
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();

            DocumentType doctype = config.getDocument().getDoctype();
            doctype = builder.getDOMImplementation().createDocumentType(doctype.getName(), doctype.getPublicId(), doctype.getSystemId());

            Document copyDocument = builder.getDOMImplementation().createDocument(null, "config", doctype);

            Node copyRoot = copyDocument.importNode(config.getDocument().getDocumentElement(), true);
            copyDocument.removeChild(copyDocument.getDocumentElement());
            copyDocument.appendChild(copyRoot);

            Config copy = new Config(copyDocument);

            copy.traverse(new Config.Traverser() {
                @Override
                public void processParameter(String name, String value, Element element) {
                    Parameter parameter = parser.parse(name, value);
                    try {
                        element.setAttribute("value", parameter.process(placeholders));
                    } catch (Parameter.PlaceholderSubstitutionException e) {
                        throw new RuntimeException(e.toString());
                    }
                }
            });

            return copy;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Error while configuring parser");
        }
    }
}

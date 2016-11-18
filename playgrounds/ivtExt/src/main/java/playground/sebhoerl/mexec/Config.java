package playground.sebhoerl.mexec;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

public class Config {
    final private Document document;

    public Config(Document document) {
        this.document = document;
    }

    public Document getDocument() {
        return document;
    }

    private Element getModuleElement(String moduleName, boolean throwException) {
        Element root = document.getDocumentElement();
        NodeList children = root.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (node instanceof Element) {
                Element element = (Element) node;

                if (element.getTagName().equals("module") && element.getAttribute("name").equals(moduleName)) {
                    return element;
                }
            }
        }

        if (throwException) {
            throw new RuntimeException("Module \"" + moduleName + "\" not found in config");
        }

        return null;
    }

    private Element getParameterSetElement(String moduleName, String type, int index, boolean throwException) {
        Element module = getModuleElement(moduleName, throwException);
        NodeList children = module.getChildNodes();

        int current = 0;
        boolean foundType = false;

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (node instanceof Element) {
                Element element = (Element) node;

                if (element.getTagName().equals("parameterset") && element.getAttribute("type").equals(type)) {
                    foundType = true;

                    if (current == index) {
                        return element;
                    }
                }
            }
        }

        if (throwException) {
            if (!foundType) {
                throw new RuntimeException("ParameterSet type \"" + type + "\" not found in module \"" + moduleName + "\"");
            } else {
                throw new RuntimeException("Only " + current + " parametersets of type \"" + type + "\" not found in module \"" + moduleName + "\" [index " + index + " requested]");
            }
        }

        return null;
    }

    private Element getParameterElement(Element parent, String parameterName) {
        NodeList nodes = parent.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node instanceof Element) {
                Element element = (Element) node;

                if (element.getTagName().equals("param") && element.getAttribute("name").equals(parameterName)) {
                    return element;
                }
            }
        }

        return null;
    }

    private Element getParameterElement(String moduleName, String parameterName, boolean throwException) {
        Element parent = getModuleElement(moduleName, throwException);
        Element param = getParameterElement(parent, parameterName);

        if (param == null && throwException) {
            throw new RuntimeException("Parameter \"" + moduleName + "\" not found in module \"" + moduleName + "\"");
        }

        return param;
    }

    private Element getParameterElement(String moduleName, String parameterSetType, int parameterSetIndex, String parameterName, boolean throwException) {
        Element parent = getParameterSetElement(moduleName, parameterSetType, parameterSetIndex, throwException);
        Element param = getParameterElement(parent, parameterName);

        if (param == null && throwException) {
            throw new RuntimeException("Parameter \"" + moduleName + "\" not found in parameterset \"" + parameterSetType + "\"{" + parameterSetIndex + "} module \"" + moduleName + "\"");
        }

        return param;
    }

    public Element getOrCreateModuleElement(String moduleName) {
        Element module = getModuleElement(moduleName, false);

        if (module == null) {
            module = document.createElement("module");
            module.setAttribute("name", moduleName);
            document.getDocumentElement().appendChild(module);
        }

        return module;
    }

    public Element getOrCreateParameterElement(Element parent, String parameterName) {
        Element param = getParameterElement(parent, parameterName);

        if (param == null) {
            param = document.createElement("param");
            param.setAttribute("name", parameterName);
            parent.appendChild(param);
        }

        return param;
    }

    public Element createParameterSetElement(Element module, String parameterSetType) {
        Element paramset = document.createElement("parameterset");
        paramset.setAttribute("type", parameterSetType);
        module.appendChild(paramset);

        return paramset;
    }

    public List<String> getModules() {
        NodeList nodes = document.getElementsByTagName("module");
        List<String> modules = new LinkedList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            modules.add(element.getAttribute("name"));
        }

        return modules;
    }

    public String getParameter(String moduleName, String parameterName) {
        return getParameterElement(moduleName, parameterName, true).getAttribute("value");
    }

    public boolean hasParameter(String moduleName, String parameterName) {
        return getParameterElement(moduleName, parameterName, false) != null;
    }

    public void setParameter(String moduleName, String parameterName, String parameterValue) {
        getOrCreateParameterElement(getOrCreateModuleElement(moduleName), parameterName).setAttribute("value", parameterValue);
    }

    public void removeParameter(String moduleName, String parameterName) {
        Element param = getParameterElement(moduleName, parameterName, false);

        if (param != null) {
            Element module = getModuleElement(moduleName, false);
            module.removeChild(param);
        }
    }

    public Collection<String> getParameters(Element parent) {
        List<String> names = new LinkedList<>();

        NodeList nodes = parent.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node instanceof Element) {
                Element element = (Element) node;
                names.add(element.getAttribute("name"));
            }
        }

        return names;
    }

    public Collection<String> getParameters(String moduleName) {
        return getParameters(getModuleElement(moduleName, true));
    }

    public Collection<String> getParameters(String moduleName, String parameterSetType, int parameterSetIndex) {
        return getParameters(getParameterSetElement(moduleName, parameterSetType, parameterSetIndex, true));
    }

    public String getParameter(String moduleName, String parameterSetType, int parameterSetIndex, String parameterName) {
        Element element = getParameterElement(moduleName, parameterSetType, parameterSetIndex, parameterName, true);
        return element.getAttribute("value");
    }

    public void setParameter(String moduleName, String parameterSetType, int parameterSetIndex, String parameterName, String parameterValue) {
        Element set = getParameterSetElement(moduleName, parameterSetType, parameterSetIndex, true);
        Element param = getOrCreateParameterElement(set, parameterName);
        param.setAttribute("value", parameterValue);
    }

    public void removeParameter(String moduleName, String parameterSetType, int parameterSetIndex, String parameterName) {
        Element set = getParameterSetElement(moduleName, parameterSetType, parameterSetIndex, true);
        Element param = getParameterElement(set, parameterName);

        if (param != null) {
            set.removeChild(param);
        }
    }

    public int addParameterSet(String moduleName, String parameterSetType) {
        createParameterSetElement(getOrCreateModuleElement(moduleName), parameterSetType);
        return countParameterSets(moduleName, parameterSetType) - 1;
    }

    public void removeParameterSet(String moduleName, String parameterSetType, int parameterSetIndex) {
        Element set = getParameterSetElement(moduleName, parameterSetType, parameterSetIndex, false);

        if (set != null) {
            Element module = getModuleElement(moduleName, true);
            module.removeChild(set);
        }
    }

    public int countParameterSets(String moduleName, String parameterSetType) {
        Element module = getModuleElement(moduleName, true);
        NodeList nodes = module.getChildNodes();
        int count = 0;

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node instanceof Element) {
                Element element = (Element) node;

                if (element.getTagName().equals("parameterset") && element.getAttribute("type").equals(parameterSetType)) {
                    count += 1;
                }
            }
        }

        return count;
    }

    public interface Traverser {
        void processParameter(String name, String value, Element element);
    }

    void traverseParameterSet(Element module, Traverser traverser) {
        NodeList children = module.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child instanceof Element) {
                Element element = (Element) child;

                if (element.getTagName().equals("param")) {
                    traverser.processParameter(element.getAttribute("name"), element.getAttribute("value"), element);
                }
            }
        }
    }

    void traverseModule(Element module, Traverser traverser) {
        NodeList children = module.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child instanceof Element) {
                Element element = (Element) child;

                if (element.getTagName().equals("param")) {
                    traverser.processParameter(element.getAttribute("name"), element.getAttribute("value"), element);
                } else if (element.getTagName().equals("parameterset")) {
                    traverseParameterSet(element, traverser);
                }
            }
        }
    }

    public void traverse(Traverser traverser) {
        Element root = document.getDocumentElement();

        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            Node moduleNode = root.getChildNodes().item(i);

            if (moduleNode instanceof Element) {
                Element moduleElement = (Element) moduleNode;

                if (moduleElement.getTagName().equals("module")) {
                    traverseModule(moduleElement, traverser);
                }
            }
        }
    }
}

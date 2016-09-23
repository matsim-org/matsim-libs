package playground.sebhoerl.av.logic.service;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.utils.io.AbstractMatsimWriter;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ServiceTracker extends AbstractMatsimWriter implements MobsimAfterSimStepListener, BeforeMobsimListener, AfterMobsimListener {
    final private OutputDirectoryHierarchy controlerIO;
    final private int interval;
    
    final private LinkedList<Service> services = new LinkedList<>();    
    
    private ServiceManager serviceManager;
    private boolean trackIteration = false;

    public ServiceTracker(int interval, OutputDirectoryHierarchy controlerIO) {
        this.interval = interval;
        this.controlerIO = controlerIO;
    }
    
    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }
    
    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        services.clear();
        serviceManager = null;
        
        trackIteration = event.getIteration() % interval == 0;
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
        if (trackIteration) {
            services.addAll(serviceManager.getFinishedServices());
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        if (!trackIteration) return;
        
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    
            Document document = documentBuilder.newDocument();
            Element schedules = document.createElement("services");
            document.appendChild(schedules);
            
            for (Service service : services) {
                schedules.appendChild(buildXmlService(document, service));
            }
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            
            DOMSource source = new DOMSource(document);
            
            this.openFile(controlerIO.getIterationFilename(event.getIteration(), "av_services.xml.gz"));
            StreamResult result = new StreamResult(this.writer);
            
            transformer.transform(source, result);
        } catch (ParserConfigurationException | TransformerFactoryConfigurationError | TransformerException e) {
            throw new RuntimeException("Error while dumping AV schedules");
        } finally {
            this.close();
        }
    }
    
    private Map<String, String> readRequest(Request request) {
        Map<String, String> attributes = new HashMap<>();
        
        for (Field field : request.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            
            try {
                attributes.put(field.getName(), field.get(request).toString());
            } catch (DOMException | IllegalArgumentException | IllegalAccessException e) {}
            
            field.setAccessible(false);
        }
        
        return attributes;
    }
    
    private Map<String, String> readService(Service service) {
        Map<String, String> attributes = new HashMap<>();
        
        for (Field field : service.getClass().getDeclaredFields()) {
            boolean include = false;
            include |= field.getName().endsWith("Time");
            include |= field.getName().endsWith("Distance");
            include |= field.getName().endsWith("Agent");
            include |= field.getName().equals("startLinkId");
            include |= field.getName().equals("id");
            
            if (include) {
                field.setAccessible(true);
                
                try {
                    Object value = field.get(service);
                    
                    if (value instanceof MobsimAgent) {
                        value = (Object)((MobsimAgent)value).getId().toString();
                    }
                    
                    attributes.put(field.getName(), value.toString());
                } catch (DOMException | IllegalArgumentException | IllegalAccessException e) {}
                
                field.setAccessible(false);
            }
        }
        
        return attributes;
    }
    
    private void applyXmlAttributes(Element element, Map<String, String> attributes) {
        for (String name : attributes.keySet()) {
            element.setAttribute(name, attributes.get(name));
        }
    }
    
    private Element buildXmlRequest(Document document, Request request) {
        Element element = document.createElement("request");
        applyXmlAttributes(element, readRequest(request));
        return element;
    }
    
    private Element buildXmlService(Document document, Service service) {
        Element element = document.createElement("service");
        element.appendChild(buildXmlRequest(document, service.getRequest()));
        applyXmlAttributes(element, readService(service));
        return element;
    }
}

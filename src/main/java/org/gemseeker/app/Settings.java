package org.gemseeker.app;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 * @author Gem
 */
public class Settings {

    private static Settings instance;
    private Document doc;
    
    private Settings() throws ParserConfigurationException,
            SAXException, IOException {
        File file = new File(Utils.SETTINGS_FILE);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.parse(file);
        doc.getDocumentElement().normalize();
    }
    
    public static Settings getInstance() throws ParserConfigurationException,
            SAXException, IOException {
        if (instance == null) instance = new Settings();
        return instance;
    }
    
    public String getDatabaseValue(String tag) {
        return get("database", tag);
    }
    
    public void setDatabaseValue(String tag, Object value) {
        set("database", tag, String.valueOf(value));
    }
    
    private String get(String parentTag, String valueTag) {
        Node node = doc.getElementsByTagName(parentTag).item(0);
        if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            return ((Element) node).getElementsByTagName(valueTag).item(0).getTextContent();
        }
        return null;
    }
    
    private void set(String parentTag, String valueTag, String value) {
        Node node = doc.getElementsByTagName(parentTag).item(0);
        if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
            ((Element) node).getElementsByTagName(valueTag).item(0).setTextContent(value);
        }
    }
    
    public void save() throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(Utils.SETTINGS_FILE));
        transformer.transform(source, result);
    }
}

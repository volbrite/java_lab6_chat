package pl.wroc.pwr.java.client.socket;

import pl.wroc.pwr.java.client.gui.HistoriaRamka;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class Historia {

    public String filePath;

    public Historia(String filePath) {
        this.filePath = filePath;
    }

    public void addMessage(Wiadomosc msg, String time) {

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(filePath);

            Node data = doc.getFirstChild();

            Element message = doc.createElement("wiadomosc");
            Element wysylajacy = doc.createElement("wysylajacy");
            wysylajacy.setTextContent(msg.wysylajacy);
            Element zawartosc = doc.createElement("zawartosc");
            zawartosc.setTextContent(msg.zawartosc);
            Element odbiorca = doc.createElement("odbiorca");
            odbiorca.setTextContent(msg.odbiorca);
            Element data1 = doc.createElement("data");
            data1.setTextContent(time);

            message.appendChild(wysylajacy);
            message.appendChild(zawartosc);
            message.appendChild(odbiorca);
            message.appendChild(data1);

            data.appendChild(message);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath));
            transformer.transform(source, result);

        } catch (Exception ex) {
            System.out.println("Podczas modyfikowania pliku XML wystąpił wyjątek.\n" + ex.getMessage());
        }
    }

    public void wypelnijRamke(HistoriaRamka frame) {

        DefaultTableModel model = (DefaultTableModel) frame.jTable1.getModel();

        try {
            File fXmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("message");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    model.addRow(new Object[]{getTagValue("wysylajacy", eElement), getTagValue("zawartosc", eElement), getTagValue("odbiorca", eElement), getTagValue("time", eElement)});
                }
            }
        } catch (Exception ex) {
            System.out.println("Podczas odczytu wystąpił wyjątek.\n" + ex.getMessage());
        }
    }

    public static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = nlList.item(0);
        return nValue.getNodeValue();
    }
}

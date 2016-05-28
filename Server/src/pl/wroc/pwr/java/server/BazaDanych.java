package pl.wroc.pwr.java.server;

import java.io.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

public class BazaDanych {

    public String filePath;

    public BazaDanych(String filePath) {
        this.filePath = filePath;
    }

    public boolean uzytkownikIstnieje(String nazwaUzytkownika) {

        try {
            File fXmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("uzytkownik");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (getTagValue("nazwaUzytkownika", eElement).equals(nazwaUzytkownika)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception ex) {
            System.out.println("Wystąpił bład przy odczycie z bazy : uzytkownikIstnieje()\n" + ex.getMessage());
            return false;
        }
    }

    public boolean sprawdzLogin(String nazwaUzytkownika, String haslo) {

        if (!uzytkownikIstnieje(nazwaUzytkownika)) {
            return false;
        }

        try {
            File fXmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("uzytkownik");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if (getTagValue("nazwaUzytkownika", eElement).equals(nazwaUzytkownika) && getTagValue("haslo", eElement).equals(haslo)) {
                        return true;
                    }
                }
            }
            System.out.println("Znaleziono");
            return false;
        } catch (Exception ex) {
            System.out.println("Błąd odczytu z bazy danych : uzytkownikIstnieje()\n" + ex.getMessage());
            return false;
        }
    }

    public void dodajUzytkownika(String nazwaUzytkownika, String haslo) {

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(filePath);

            Node data = doc.getFirstChild();

            Element nowyUser = doc.createElement("uzytkownik");
            Element nowaNazwaUsera = doc.createElement("nazwaUzytkownika");
            nowaNazwaUsera.setTextContent(nazwaUzytkownika);
            Element noweHaslo = doc.createElement("haslo");
            noweHaslo.setTextContent(haslo);

            nowyUser.appendChild(nowaNazwaUsera);
            nowyUser.appendChild(noweHaslo);
            data.appendChild(nowyUser);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePath));
            transformer.transform(source, result);

        } catch (Exception ex) {
            System.out.println("Błąd zapisu do bazy XML.\n" + ex.getMessage());
        }
    }

    public static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = (Node) nlList.item(0);
        return nValue.getNodeValue();
    }
}

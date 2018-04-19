package utils;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public abstract class XMLUtils {
	
	public static NodeList getElementsByTagName(File xmlFile,String tag) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlFile);
		
		return doc.getElementsByTagName(tag);
		
	}

	public static List<File> getModules(File xmlFile) throws Exception{
		List<File> result=new ArrayList<File>();
		
		if(!xmlFile.exists())
			return result;
		
		NodeList nList=getElementsByTagName(xmlFile,"packaging");
		Node node=nList.item(0);
		if(node == null)
			result.add(xmlFile.getParentFile());
		else if(!node.getTextContent().equals("pom"))
			result.add(xmlFile.getParentFile());
		
		nList=getElementsByTagName(xmlFile,"module");
		for(int i=0; i<nList.getLength();i++) {
			File module=new File(xmlFile.getParentFile(),nList.item(i).getTextContent());
			result.addAll(getModules(new File(module,"pom.xml")));
		}
		
		return result;
	}
	
	/*public void addDpendencies(List<File> modules) throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		
		for(File module:modules) {
			File pom=new File(module,"pom.xml");
			Document doc = dBuilder.parse(pom);
			
			NodeList nList=doc.getElementsByTagName("packaging");
			Node n=nList.item(0);
			if(n!=null && n.getTextContent().equals("pom"))
				continue;
			
			Dependency dependency=new Dependency();
		}
	}*/
	
	
	public static void addPlugins(List<File> modules) throws SAXException, IOException, ParserConfigurationException, TransformerException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		
		
		for(File module: modules){
			File pom=new File(module,"pom.xml");
			Document doc = dBuilder.parse(pom);
			
			NodeList nList=doc.getElementsByTagName("packaging");
			Node node=nList.item(0);
			if(node!=null && node.getTextContent().equals("pom"))
				continue;
			
			Element plugin=doc.createElement("plugin");
			
			Element aux=doc.createElement("artifactId");
			aux.appendChild(doc.createTextNode("maven-assembly-plugin"));
			plugin.appendChild(aux);
			
			aux=doc.createElement("executions");
			Element aux1=doc.createElement("execution");
			Element aux2=doc.createElement("phase");
			aux2.appendChild(doc.createTextNode("package"));
			aux1.appendChild(aux2);
			
			aux2=doc.createElement("goals");
			Element aux3=doc.createElement("goal");
			aux3.appendChild(doc.createTextNode("single"));
			aux2.appendChild(aux3);
			aux1.appendChild(aux2);
			aux.appendChild(aux1);
			plugin.appendChild(aux);
				
			aux=doc.createElement("configuration");
			aux1=doc.createElement("descriptorRefs");
			aux2=doc.createElement("descriptorRef");
			aux2.appendChild(doc.createTextNode("jar-with-dependencies"));
			aux1.appendChild(aux2);
			aux.appendChild(aux1);
			plugin.appendChild(aux);
			
			
			nList=doc.getElementsByTagName("build");
			node=nList.item(0);
			if(node!=null){
				Node n;
				for(n=node.getFirstChild();n!=null;n=n.getNextSibling()) {
					if(n.getNodeName().equals("plugins")) {
						n.appendChild(plugin);
						break;
					}
				}
				if(n==null) {
					aux=doc.createElement("plugins");
					aux.appendChild(plugin);
					node.appendChild(aux);
				}
			}
				
			else{
				aux=doc.getDocumentElement();
				aux1=doc.createElement("build");
				aux2=doc.createElement("plugins");
				aux2.appendChild(plugin);
				aux1.appendChild(aux2);
				aux.appendChild(aux1);
				
			}
				
			DOMSource source = new DOMSource(doc);
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        Transformer transformer = transformerFactory.newTransformer();
	        StreamResult result = new StreamResult(pom);
	        transformer.transform(source, result);
		}
	}
}

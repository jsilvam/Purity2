package utils;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


public abstract class XMLUtils {

	public static List<File> getModules(File xmlFile) throws Exception{
		if(!xmlFile.exists())
			return new ArrayList<File>();
		
		List<File> result=new ArrayList<File>();
		result.add(xmlFile.getParentFile());
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(xmlFile);
		
		NodeList nList=doc.getElementsByTagName("module");
		
		for(int i=0; i<nList.getLength();i++) {
			File module=new File(xmlFile.getParentFile(),nList.item(i).getTextContent());
			result.add(module);
			result.addAll(getModules(new File(module,"pom.xml")));
		}
		
		return result;
	}
}

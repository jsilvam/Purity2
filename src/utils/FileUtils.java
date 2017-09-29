package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public abstract class FileUtils {
	
	public static List<String> listClass(File srcFolder){
		if(!srcFolder.exists())
			return new ArrayList<String>();
		
		List<String> result= new ArrayList<String>();
		
		File[] files=srcFolder.listFiles();
		
		for(File file:files) {
			if(file.isDirectory())
				result.addAll(listClass(file,file.getName()));
			else
				if(file.getName().toLowerCase().endsWith(".java") && 
						!file.getName().toLowerCase().equals("package-info.java"))
					result.add(file.getName().substring(0, file.getName().lastIndexOf(".")));
				
		}
		
		return result;
		
	}
	
	public static List<String> listClass(File srcFolder,String name){
		List<String> result= new ArrayList<String>();
		
		File[] files=srcFolder.listFiles();
		
		
		for(File file:files) {
			if(file.isDirectory())
				result.addAll(listClass(file,name+"."+file.getName()));
			else
				if(file.getName().toLowerCase().endsWith(".java") && 
						!file.getName().toLowerCase().equals("package-info.java"))
					result.add(name+"."+file.getName().substring(0, file.getName().lastIndexOf(".")));
				
		}
		
		return result;
		
	}
	
	public static File findSingleFile(File srcFolder,String regex) throws IOException{
		
		File[] files=srcFolder.listFiles();
		File f;
		for(File file:files) {
			if(file.isDirectory()) { 
				if((f=findSingleFile(file,regex))!=null)
					return f;
			}
			else 
				if(file.getName().matches(regex))
					return file;
		}
		return null;
	}
	
	public static List<File> findFiles(File srcFolder,String regex) {
		List<File> result=new ArrayList<File>();
		File[] files=srcFolder.listFiles();
		for(File file:files) {
			if(file.isDirectory())
				result.addAll(findFiles(file,regex));
			else 
				if(file.getName().matches(regex))
					result.add(file);
		}
		return result;
	}
	
	public static List<File> getModules(File xmlFile) throws Exception{
		List<File> result=new ArrayList<File>();
		
		if(!xmlFile.exists())
			return result;
		
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
	
	public static void copyFile(File sourceFile, File destinationFile) throws IOException {
		FileInputStream inputStream = new FileInputStream(sourceFile);
		FileOutputStream outputStream = new FileOutputStream(destinationFile);
		FileChannel inChannel = inputStream.getChannel();
		FileChannel outChannel = outputStream.getChannel();
		try {	
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {
			inChannel.close();
			outChannel.close();
			inputStream.close();
			outputStream.close();
		}
	}
}

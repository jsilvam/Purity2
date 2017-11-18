package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;



public abstract class FileUtils {
	
	public static List<String> listClasses(File srcFolder){
		if(!srcFolder.exists())
			return new ArrayList<String>();
		
		List<String> result= new ArrayList<String>();
		
		File[] files=srcFolder.listFiles();
		
		for(File file:files) {
			if(file.isDirectory())
				result.addAll(listClasses(file,file.getName()));
			else
				if(file.getName().toLowerCase().endsWith(".java") && 
						!file.getName().toLowerCase().equals("package-info.java"))
					result.add(file.getName().substring(0, file.getName().lastIndexOf(".")));
				
		}
		
		return result;
		
	}
	
	public static List<String> listClasses(File srcFolder,String name){
		List<String> result= new ArrayList<String>();
		
		File[] files=srcFolder.listFiles();
		
		
		for(File file:files) {
			if(file.isDirectory())
				result.addAll(listClasses(file,name+"."+file.getName()));
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
	
	public static void runProcess(String command) throws IOException, InterruptedException {
		
		Process p=Runtime.getRuntime().exec(command);
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		while ((s=reader.readLine()) != null)
			System.out.println(s);
		p.waitFor();	
	}
	
	public static void runProcess(String command, int timeLimit) throws InterruptedException, IOException {
		Process p=Runtime.getRuntime().exec(command);
		
		
		
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		
		Thread t=new Thread() {
			
			@Override
			public void run() {
				try {
					for(int i=0;i<timeLimit;i++) {
						if(!p.isAlive())
							this.interrupt();
						Thread.sleep(1000);
					}
					p.destroy();
				} catch (InterruptedException e) {
				}
			}
		};
		
		t.start();
		try {
			while ((s=reader.readLine()) != null)
				System.out.println(s);
		} catch (IOException e) {
		}
		p.waitFor();
		Thread.sleep(1000);
	}

}

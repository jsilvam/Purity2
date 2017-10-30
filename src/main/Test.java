package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import utils.FileUtils;
import utils.XMLUtils;

public class Test {
	
	public boolean compare(File report1, File report2) throws ParserConfigurationException, SAXException, IOException {
		NodeList nList=XMLUtils.getElementsByTagName(report1, "failure");
		List<String> l1=new ArrayList<String>();
		System.out.println("source");
		for(int i=0;i<nList.getLength();i++) {
			l1.add(((Element)nList.item(i)).getAttribute("type"));
			System.out.println(l1.get(i));
			System.out.println(nList.item(i).getTextContent());
		}
		
		nList=XMLUtils.getElementsByTagName(report2, "failure");
		List<String> l2=new ArrayList<String>();
		System.out.println("Target");
		for(int i=0;i<nList.getLength();i++) {
			l2.add(((Element)nList.item(i)).getAttribute("type"));
			System.out.println(l2.get(i));
			System.out.println(nList.item(i).getTextContent());
		}
		
		return l1.equals(l2);
	}
	
	
	public File getCommonMethods(File sourceFolder, File targetFolder) throws Exception {
		File file=new File(sourceFolder.getParentFile(),"methodsList.txt");
		FileWriter fw= new FileWriter(file);
		
		Map<String,Class> sourceClasses=getClasses(sourceFolder);
		Map<String,Class> targetClasses=getClasses(targetFolder);
		
		for(Class sourceClass: sourceClasses.values()) {
			//skip if the target doesn't contains this class
			if(!targetClasses.containsKey(sourceClass.getName())) {
				System.out.println("Not common class: "+sourceClass                                                                                                                                 );
				continue;
			}
			
			System.out.println("Common Class: "+sourceClass);
			
			Class targetClass= targetClasses.get(sourceClass.getName());
		
			for (Constructor constructor : sourceClass.getConstructors()) {
				
					fw.write("cons : "+getSignature(constructor)+"\n");
					System.out.println("Common constructor: "+constructor);
				
				/*if (Arrays.asList(targetClass.getConstructors()).contains(constructor)) {
					fw.write("cons : "+getSignature(constructor)+"\n");
					System.out.println("Common constructor: "+constructor);
				}*/
			}
			
			for (Method method: sourceClass.getMethods()) {
				if (method.getDeclaringClass().equals(sourceClass)) {
					fw.write("method : "+getSignature(method)+"\n");
						System.out.println("Common method: "+method.getName());
				}
				/*if (Arrays.asList(targetClass.getMethods()).contains(method))
					fw.write("method : "+getSignature(method)+"\n");{
						System.out.println("Common method: "+method);
				}*/
			}
			fw.flush();
		}	
		fw.close();
		return file;
	}
	
	
	private String getSignature(Executable m) {
		String signature=m.getDeclaringClass().getName()+".";
		if(m instanceof Constructor)
			signature+="<init>(";
		else
			signature+=m.getName()+"(";
		Class[] c=m.getParameterTypes();
		for(int i=0;i<c.length;i++) {
			signature+=c[i].getName();
			if(i<(c.length-1))
				signature+=", ";
		}
		
		signature+=")";
		return signature;
	}
	
	
	private Map<String,Class> getClasses(File projectFolder) throws Exception {
		
		Map<String,Class> classes=new HashMap<String,Class>();
		List<String> cls=getClassesName(projectFolder);
		List<URL> urls=getCompiledFiles(projectFolder);
		
		
		ClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]));
		
		for(String c:cls) {
			Class clazz;
			try {
				clazz=cl.loadClass(c);
				classes.put(clazz.getName(), clazz);
			}catch(ClassNotFoundException e) {
			}
		}
		return classes;
	}
	
	private static List<String> getClassesName(File projectFolder) throws Exception {
		List<File> modules=XMLUtils.getModules(new File(projectFolder,"pom.xml"));
		List<String> classes=new ArrayList<String>();
		for(File module:modules) {
			classes.addAll(FileUtils.listClasses(new File(module,"src/main/java")));
		}
		return classes;
	}
	
	private static List<URL> getCompiledFiles(File projectFolder) throws Exception {
		List<File> modules=XMLUtils.getModules(new File(projectFolder,"pom.xml"));
		List<URL> result=new ArrayList<URL>();
		for(File module:modules) {
			File folder=new File(module,"target");
			if(folder.exists())
				result.add(FileUtils.findSingleFile(folder, ".*jar-with-dependencies.jar").toURI().toURL());
		}
		return result;
	}
	
	
	
	public File genarateTests(List<File> projectFiles,File methodList, int timeLimit, File outputDir) throws IOException, InterruptedException {
		File outDir=new File(outputDir,"tempTest");
		if(!outDir.exists())
			outDir.mkdirs();
		File command=new File(outDir,"generateCommand.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("java -ea");
		fw.write(" -classpath jars/randoop/*");
		for(File file:projectFiles)
			fw.write(":"+file);
		fw.write(" randoop.main.Main gentests");
		fw.write(" --methodlist="+methodList);
		fw.write(" --timelimit="+timeLimit);
		fw.write(" --ignore-flaky-tests=true");
		fw.write(" --junit-output-dir="+outDir);
		fw.flush();
		fw.close();
		
		runProcess("bash "+command);
		return outDir;
		//return FileUtils.findFiles(outDir, "RegressionTest.*.java");
	}
	
	
	public void compileTests(List<File> projectFiles,File testFolder) throws IOException, InterruptedException{
		File command=new File(testFolder,"compileCommand.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("javac -classpath jars/junit/*");
		for(File file:projectFiles)
			fw.write(":"+file);
		fw.write(" $(find "+testFolder+"/* | grep .java)");
		fw.flush();
		fw.close();
		
		runProcess("bash "+command);
		//return FileUtils.findFiles(testFiles.get(0).getParentFile(), "RegressionTest.*.class");
		
	}
	
	
	
	public File runTests(List<File> projectFiles,File testFolder) throws IOException, InterruptedException {
		File XMLReport= new File(testFolder,"junit_report.xml");
		for(int i=0;XMLReport.exists();i++){
			XMLReport= new File(testFolder,"junit_report.xml"+i);
		}
		File command=new File(testFolder,"runCommand.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("java -classpath jars/junit/*:");
		fw.write(testFolder+"/.");
		for(File file:projectFiles)
			fw.write(":"+file);
		fw.write(" -Dorg.schmant.task.junit4.target="+XMLReport);
		fw.write(" barrypitman.junitXmlFormatter.Runner RegressionTest");
		fw.flush();
		fw.close();
		
		runProcess("bash "+command);
		return XMLReport;
		/*JUnitCore junit = new JUnitCore();
		List<Result> result=new ArrayList<Result>();
		for(File test:tests) {
			result.add(junit.run(Class.forName(test.getAbsolutePath())));
		}
		return result;*/
	}
	
	
	@Deprecated
	public void test(List<File> projectFiles) throws IOException, InterruptedException{
		File command=new File("testCommand.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("java -classpath jars/*");
		for(File file:projectFiles)
			fw.write(":"+file);
		fw.flush();
		fw.close();
		
		//runProcess("bash "+command);
		//return FileUtils.findFiles(testFiles.get(0).getParentFile(), "RegressionTest.*.class");
		
	}
	
	
	
	
	public void runProcess(String command) throws IOException, InterruptedException {
		
		Process p=Runtime.getRuntime().exec(command);
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		while ((s=reader.readLine()) != null)
			System.out.println(s);
		p.waitFor();
		//return FileUtils.findFiles(testFiles.get(0).getParentFile(), "RegressionTest.*.class");
		
	}
}

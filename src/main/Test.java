package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import utils.FileUtils;
import utils.XMLUtils;

public class Test {
	
	private File testsFolder;
	private List<String> flakies;
	private File sourceProjectFolder;
	private List<File> compiledSourceProject;
	private File targetProjectFolder;
	private List<File> compiledTargetProject; 

	public Test(File sourceProjectFolder, File targetProjectFolder) throws Exception {
		this.sourceProjectFolder = sourceProjectFolder;
		this.targetProjectFolder = targetProjectFolder;
		this.compiledSourceProject = this.getCompiledFiles(sourceProjectFolder);
		this.compiledTargetProject = this.getCompiledFiles(targetProjectFolder);
		testsFolder = new File(sourceProjectFolder.getParentFile(),"TestsFolder");
		if(!testsFolder.exists())
			testsFolder.mkdirs();
	}
	
	public File getSourceProjectFolder() {
		return sourceProjectFolder;
	}

	public void setSourceProjectFolder(File sourceProjectFolder) {
		this.sourceProjectFolder = sourceProjectFolder;
	}

	public File getTargetProjectFolder() {
		return targetProjectFolder;
	}

	public void setTargetProjectFolder(File targetProjectFolder) {
		this.targetProjectFolder = targetProjectFolder;
	}

	public File getTestsFolder() {
		return testsFolder;
	}

	public List<String> getFlakies() {
		return flakies;
	}
	
	public void generate(int timeLimit) throws Exception {
		File commonMethods=getCommonMethods();
		generateTests(commonMethods, timeLimit);
		compileTests();
	}
	
	public boolean hasSameBehaviour() throws IOException, InterruptedException, ParserConfigurationException, SAXException {
		File sourceReport=runTests(compiledSourceProject);
		File targetReport=runTests(compiledTargetProject);
		return compare(sourceReport, targetReport);
	};
	
	private File getCommonMethods() throws Exception {
		File file=new File(testsFolder,"methodsList.txt");
		FileWriter fw= new FileWriter(file);
		
		Map<String,Class> sourceClasses=getClasses(sourceProjectFolder, compiledSourceProject);
		Map<String,Class> targetClasses=getClasses(targetProjectFolder, compiledTargetProject);
		
		for(Class sourceClass: sourceClasses.values()) {
			//skip if the target doesn't contains this class
			if(!targetClasses.containsKey(sourceClass.getName())) {
				System.out.println("Not common class: "+sourceClass                                                                                                                                 );
				continue;
			}
			
			System.out.println("Common Class: "+sourceClass);
			
			Class targetClass= targetClasses.get(sourceClass.getName());
		
			for (Constructor constructor : sourceClass.getConstructors()) {
				for (Constructor c : targetClass.getConstructors()) {
					if(constructor.toString().equals(c.toString())) {
						fw.write("cons : "+getSignature(constructor)+"\n");
						System.out.println("Common constructor: "+constructor);
						break;
					}
				}
			}
			
			for (Method method: sourceClass.getMethods()) {
				for(Method m: targetClass.getMethods()) {
					if(method.toString().equals(m.toString()) && method.getDeclaringClass().equals(sourceClass)) {
						fw.write("method : "+getSignature(method)+"\n");
						System.out.println("Common method: "+method.toGenericString());
						break;
					}
				}
			}
			fw.flush();
		}	
		fw.close();
		return file;
	}
	
	private void generateTests(File methodList, int timeLimit) throws IOException, InterruptedException {
		String command="java -ea";
		
		command+=" -classpath jars/randoop/*";
		for(File file:compiledSourceProject)
			command+=":"+file;
		command+=" randoop.main.Main gentests";
		command+=" --testclass=NoClassToTest";
		command+=" --methodlist="+methodList;
		command+=" --timelimit="+timeLimit;
		command+=" --ignore-flaky-tests=true";
		command+=" --junit-output-dir="+testsFolder;
		
		
		File commandFile=new File(testsFolder,"generateCommand.sh");
		FileWriter fw= new FileWriter(commandFile);
		fw.write(command);
		fw.flush();
		fw.close();
		
		FileUtils.runProcess("bash "+commandFile);
	}
	
	private void compileTests() throws IOException, InterruptedException{
		File command=new File(testsFolder,"compileCommand.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("javac -classpath jars/junit/*");
		for(File file:compiledSourceProject)
			fw.write(":"+file);
		fw.write(" $(find "+testsFolder+"/* | grep .java)");
		fw.flush();
		fw.close();
		
		FileUtils.runProcess("bash "+command);
	}
	
	private Map<String,Class> getClasses(File projectFolder, List<File> compiledFiles) throws Exception {	
		Map<String,Class> classes=new HashMap<String,Class>();
		List<String> cls=getClassesName(projectFolder);
		List<URL> urls = new ArrayList<URL>();
		
		for(File f: compiledFiles)
			urls.add(f.toURI().toURL());
		
		ClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]));
		
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
	
	private File runTests(List<File> projectFiles) throws IOException, InterruptedException {
		File XMLReport= new File(testsFolder,"junit_report.xml");
		for(int i=0;XMLReport.exists();i++){
			XMLReport= new File(testsFolder,"junit_report"+i+".xml");
		}
		File command=new File(testsFolder,"runCommand.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("java -classpath jars/junit/*:");
		fw.write(testsFolder+"/.");
		for(File file:projectFiles)
			fw.write(":"+file);
		fw.write(" -Dorg.schmant.task.junit4.target="+XMLReport);
		fw.write(" barrypitman.junitXmlFormatter.Runner RegressionTest");
		fw.flush();
		fw.close();
		
		FileUtils.runProcess("bash "+command);
		return XMLReport;
	}
	
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
	
	private List<File> getCompiledFiles(File projectFolder) throws Exception {
		List<File> modules=XMLUtils.getModules(new File(projectFolder,"pom.xml"));
		List<File> result=new ArrayList<File>();
		for(File module:modules) {
			File folder=new File(module,"target");
			if(folder.exists())
				result.add(FileUtils.findSingleFile(folder, ".*jar-with-dependencies.jar"));
		}
		return result;
	}
	

}

package main;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utils.FileUtils;
import utils.GithubDownloader;
import utils.ZipExtractor;
import utils.XMLUtils;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;



public class Purity {
	
	private String urlRepository;
	
	public Purity(String urlRepository){
		this.urlRepository=urlRepository;
	}
	
	public void setUrlRepository(String urlRepository){
		this.urlRepository=urlRepository;
	}
	
	public String getUrlRepository(){
		return this.urlRepository;
	}
	

	public int check(String commit, String parent) throws Exception{
		
		
		GithubDownloader git=new GithubDownloader(urlRepository);
		git.setLocation(git.getLocation()+"/"+commit);
		
		
		File sourceFile=git.downloadCommit(parent);
		File sourceFolder=ZipExtractor.extract(sourceFile, new File(git.getLocation(),parent));
		
		System.out.println(sourceFolder.getAbsolutePath());
		
		
		//compilar o projeto
		List<File> compiledProject=this.compileProject(sourceFolder);
		
		//Listar Classes
		File classesToTest=this.getClassesToTest(sourceFolder);
		
		
		
		List<File> tests=this.genarateTests(compiledProject, classesToTest, 10, sourceFolder);
		List<File> compiledTests=compileTests(compiledProject,tests);
		System.out.println(compiledTests);
		
		System.exit(0);
		
		List<Result> result=this.runTests(compiledTests);
		
		
		
		
		return 0;
	}
	
	private List<File> compileProject(File projectFolder) throws Exception{
		List<File> modules=XMLUtils.getModules(new File(projectFolder,"pom.xml"));
		XMLUtils.addPlugins(modules);
		
		Invoker invoker = new DefaultInvoker();
		if(System.getProperty("os.name").contains("Linux"))
			invoker.setMavenHome(new File("/usr/share/maven"));
		else
			invoker.setMavenHome(new File("C:\\Program Files\\apache-maven-3.5.0"));
		
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile( new File( projectFolder,"pom.xml" ) );
		request.setGoals( Arrays.asList( "install" , "-DskipTests") );
		invoker.execute( request );
		
		//encontrar o projeto compilado
		
		List<File> result=new ArrayList<File>();
		for(File module:modules) {
			File folder=new File(module,"target");
			if(folder.exists())
				result.add(FileUtils.findSingleFile(folder, ".*jar-with-dependencies.jar"));
		}
		return result;
	}
	
	
	private File getClassesToTest(File projectFolder) throws Exception {
		File file=new File(projectFolder,"classesList.txt");
		List<File> modules=XMLUtils.getModules(new File(projectFolder,"pom.xml"));
		FileWriter fw= new FileWriter(file);
		
		for(File module:modules) {
			List<String> classes=FileUtils.listClass(new File(module,"src/main/java"));
			for(String c: classes) {
				fw.write(c);
				fw.write("\n");
				fw.flush();
			}
		}
		fw.close();
		return file;
	}
	
	private List<File> genarateTests(List<File> projectFiles,File classesList, int timeLimit, File outputDir) throws IOException, InterruptedException {
		File command=new File(outputDir,"command.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("pwd");
		fw.write("\njava -ea");
		fw.write(" -classpath lib/randoop-all-3.1.5.jar");
		for(File file:projectFiles)
			fw.write(":"+file);
		fw.write(" randoop.main.Main gentests ");
		fw.write(" --classlist="+classesList);
		fw.write(" --timelimit="+timeLimit);
		fw.write(" --junit-output-dir="+outputDir);
		fw.flush();
		fw.close();
		
		
		Process p=Runtime.getRuntime().exec("bash "+command);
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		while ((s=reader.readLine()) != null)
			System.out.println(s);
		p.waitFor();
		return FileUtils.findFiles(outputDir, "RegressionTest.*.java");
	}
	
	
	private List<File> compileTests(List<File> projectFiles,List<File> testFiles) throws IOException, InterruptedException{
		String command="javac -classpath lib/junit-4.12.jar";
		
		for(File file:projectFiles)
			command+=":"+file;
		
		for(File file:testFiles)
			command+=" "+file;
		
		System.out.println(command);
		
		Process p=Runtime.getRuntime().exec(command);
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		while ((s=reader.readLine()) != null)
			System.out.println(s);
		p.waitFor();
		return FileUtils.findFiles(testFiles.get(0).getParentFile(), "RegressionTest.*.class");
		
	}
	
	private List<Result> runTests(List<File> tests) throws ClassNotFoundException {
		JUnitCore junit = new JUnitCore();
		List<Result> result=new ArrayList<Result>();
		for(File test:tests) {
			result.add(junit.run(Class.forName(test.getAbsolutePath())));
		}
		return result;
	}
	
	private void deleteDirectory(File dir){
		File[] contents=dir.listFiles();
		for(File f: contents){
			if(f.isDirectory())
				deleteDirectory(f);
			else
				f.delete();
		}
		dir.delete();
	}
	
}

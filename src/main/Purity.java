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
		Test test=new Test();
		git.setLocation(git.getLocation()+"/"+commit);
		
		
		File sourceFile=git.downloadCommit(parent);
		File sourceFolder=ZipExtractor.extract(sourceFile, new File(git.getLocation(),parent));
		
		File targetFile=git.downloadCommit(commit);
		File targetFolder=ZipExtractor.extract(targetFile, new File(git.getLocation(),commit));
		
		System.out.println(sourceFolder.getAbsolutePath());
		
		
		//compilar o projeto
		List<File> compiledSourceProject=this.compileProject(sourceFolder);
		List<File> compiledTargetProject=this.compileProject(targetFolder);
		
		
		
		//Listar Classes
		//File classesToTest=test.getClassesToTest(sourceFolder);
		
		
		//List Methods
		File commonMethods=test.getCommonMethods(sourceFolder, targetFolder);
		
		
		
		
		File testsFolder=test.genarateTests(compiledSourceProject, commonMethods, 30, git.getLocation());
		test.compileTests(compiledSourceProject,testsFolder);
		//List<File> compiledTests=compileTests(compiledProject,testsFolder);
		//System.out.println(compiledTests);
		
		
		
		File sourceReport=test.runTests(compiledSourceProject,testsFolder);
		File targetReport=test.runTests(compiledTargetProject,testsFolder);
		
		System.out.println(test.compare(sourceReport, targetReport));
		System.exit(0);
		
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

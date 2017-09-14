package main;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import randoop.main.Main;

import utils.FileUtils;
import utils.GithubDownloader;
import utils.ZipExtractor;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;



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
		File compiledProject=this.compileProject(sourceFolder);
		
		//Listar Classes
		File classesToTest=this.getClassesToTest(sourceFolder);
		
		List<File> tests=this.genarateTests(compiledProject, classesToTest, 10, sourceFolder);
		for(File f:tests)
			System.out.println(f);
		
		System.exit(0);
		
		
		
		
		/*Process p=Runtime.getRuntime().exec("mvn -f "+sourceFolder.getAbsolutePath()+" clean");
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s;
		while ((s=reader.readLine()) != null) {System.out.println(s);}
		p.waitFor();
		System.out.println("Estatus da compila��o: "+p.exitValue());*/
		
		
		
		File targetFile=git.downloadCommit(commit);
		File targetFolder=ZipExtractor.extract(targetFile, new File(git.getLocation(),commit));
		
		/*
		System.out.println("Copiling: "+targetFolder);
		p=Runtime.getRuntime().exec("mvn -f "+targetFolder.getAbsolutePath()+" compile");
		reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
		while ((reader.readLine()) != null) {}
		p.waitFor();
		System.out.println("Estatus da compila��o: "+p.exitValue());
		*/
		return 0;
	}
	
	private File compileProject(File projectFolder) throws MavenInvocationException{
		Invoker invoker = new DefaultInvoker();
		if(System.getProperty("os.name").contains("Linux"))
			invoker.setMavenHome(new File("/usr/share/maven"));
		else
			invoker.setMavenHome(new File("C:\\Program Files\\apache-maven-3.5.0"));
		
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile( new File( projectFolder,"pom.xml" ) );
		request.setMavenOpts("DskipTests");
		request.setGoals( Arrays.asList( "package" ) );
		invoker.execute( request );
		
		//encontrar o projeto compilado
		return FileUtils.findSingleFile(projectFolder, ".*[^(sources)].jar");
	}
	
	
	private File getClassesToTest(File projectFolder) throws IOException {
		File file=new File(projectFolder,"classesList.txt");
		FileWriter fw= new FileWriter(file);
		List<String> classes=FileUtils.listClass(new File(projectFolder,"src/main/java"));
		for(String c: classes) {
			fw.write(c);
			fw.write("\n");
			fw.flush();
		}
		fw.close();
		return file;
	}
	
	private List<File> genarateTests(File project,File classesList, int timeLimit, File outputDir) throws IOException, InterruptedException {
		File command=new File(System.getProperty("java.io.tmpdir")+"command.sh");
		FileWriter fw= new FileWriter(command);
		fw.write("\n java -ea");
		fw.write(" -classpath lib/randoop-all-3.1.5.jar:"+project);
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
		while ((s=reader.readLine()) != null) {
			System.out.println(s);
			}
		p.waitFor();
		return FileUtils.findFiles(outputDir, "RegressionTest.*.java");
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

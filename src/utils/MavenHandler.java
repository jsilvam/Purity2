package utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

public class MavenHandler {

	private String mavenHome;
	private Invoker invoker;
	
	public MavenHandler(String mavenHome) {
		this.mavenHome = mavenHome;
		invoker = new DefaultInvoker();
		invoker.setMavenHome(new File(mavenHome));
	}
	
	public MavenHandler() {
		invoker = new DefaultInvoker();
		if(System.getProperty("os.name").contains("Linux"))
			setMavenHome("/usr/share/maven");
		else
			setMavenHome("C:\\Program Files\\apache-maven-3.5.0");
	}
	
	public String getMavenHome() {
		return mavenHome;
	}

	public void setMavenHome(String mavenHome) {
		this.mavenHome = mavenHome;
		invoker.setMavenHome(new File(mavenHome));
	}

	public void copyDependencies(File projectFolder) throws MavenInvocationException {
		execute(new File( projectFolder,"pom.xml" ),
				Arrays.asList( "dependency:copy-dependencies"));
	}
	
	public void compileProject(File projectFolder) throws Exception{
		List<File> modules=XMLUtils.getModules(new File(projectFolder,"pom.xml"));
		XMLUtils.addPlugins(modules);
		
		execute(new File( projectFolder,"pom.xml" ), 
				Arrays.asList( "install" , "-DskipTests"));
	}
	
	public void execute(File pomFile, List<String> goals) throws MavenInvocationException {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile( pomFile );
		request.setGoals( goals );
		invoker.execute( request );
	}
	
}

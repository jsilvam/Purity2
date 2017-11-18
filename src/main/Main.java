 package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Main {
	
	
	
	private static void check(String repositoryUrl) throws IOException  {
		
		String dir;
		if(System.getProperty("os.name").contains("Linux"))
			dir  = "/home/jaziel/Dropbox/UFCG/Projeto/Dados/CSVs/Refatoramentos"; //Linux
		else
			dir  = "C:\\Users\\Jaziel Moreira\\Dropbox\\UFCG\\Projeto\\Dados\\CSVs\\Refatoramentos"; //Windows
		
		String aux=repositoryUrl.substring(repositoryUrl.lastIndexOf("/")+1);
		
		Purity p=new Purity(repositoryUrl);
		Scanner in = new Scanner(new FileReader(dir+"/Part 1/"+aux+".csv")).useDelimiter(";");
		FileWriter fw= new FileWriter(new File(dir+"/Part 2/"+aux+".csv"));

		fw.write("Commit;isRefactoring\n");
		fw.flush();
		String commit="";
		
		in.nextLine();
		while(in.hasNext()) {
			in.next();
			aux=in.next();
			if(!aux.equals(commit)) {
				commit=aux;
				String parent=in.next();
				fw.write(commit+";");
				try {
					boolean sameBehaviour=p.check(commit, parent);
					if(sameBehaviour)
						fw.write(1+"\n");
					else
						fw.write(2+"\n");
					System.out.println("Same Behaviour: "+sameBehaviour);
				} catch (Exception e) {
					fw.write((-1)+"\n");
					System.out.println("Same Behaviour: Error");
				}
				fw.flush();
				
			}
			in.nextLine();
		}
		in.close();
	}

	public static void main(String[] args) throws Exception {

		
		check("https://github.com/square/retrofit");

	}
}

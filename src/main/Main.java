 package main;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;

public class Main {
	
	
	
	private static void check(String repositoryUrl) throws Exception {
		
		String dir  = "C:\\Users\\Jaziel Moreira\\Dropbox\\UFCG\\Projeto\\Dados\\CSVs\\Refatoramentos"; //windows
		String aux=repositoryUrl.substring(repositoryUrl.lastIndexOf("/")+1);
		int refactoring;
		
		Purity p=new Purity(repositoryUrl);
		Scanner in = new Scanner(new FileReader(dir+"\\Part 1\\"+aux+".csv")).useDelimiter(";");
		FileWriter fw= new FileWriter(new File(dir+"\\Part 2\\"+aux+".csv"));
		
		FileWriter fw2= new FileWriter(new File(dir+"\\Part 2\\"+aux+" - log.txt"));
		
		fw.write("Commit;isRefactoring\n");
		fw.flush();
		String commit="";
		int cont=0;
		
		in.nextLine();
		while(in.hasNext()) {
			in.next();
			aux=in.next();
			if(!aux.equals(commit)) {
				commit=aux;
				String parent=in.next();
				refactoring=p.check(commit, parent);
				for(int i=1;refactoring==1 && i<3;i++)
					refactoring=p.check(commit, parent);
				System.out.println("Is refactoring? "+refactoring);
				fw.write(commit+";"+refactoring+"\n");
				fw.flush();
				cont++;
				fw2.write("\nCommit atual: "+cont);
				fw2.flush();
			}
			in.nextLine();
		}
		fw.close();									
		fw2.close();
		in.close();
	}

	public static void main(String[] args) throws Exception {

		
		check("https://github.com/clojure/clojure");
		check("https://github.com/alibaba/fastjson");
		check("https://github.com/jankotek/mapdb");
		check("https://github.com/alibaba/druid");
		check("https://github.com/mcMMO-Dev/mcMMO");

	}
}

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JOptionPane;

//java -Xmx4000m -jar Wagner_.jar -D:\Lewis\small_opera_test\ 4
public class Wagner_{
private static final String USAGE = "Wagner - constructs stacks from TIFFs with names matching r[0-9]{2}c[0-9]{2}f[0-9]{2}p[0-9]{2}-ch[0-9]sk[0-9]?[0-9]fk[0-9]fl[0-9].tiff\n"
		 							+"usage: Wagner directory_path(string, required) thread_count(int, optional)";

	/*	
* 	opera exported TIFF filenames
*	r[0-9]{2}c[0-9]{2}f[0-9]{2}p[0-9]{2}-ch[0-9]sk[0-9]?[0-9]fk[0-9]fl[0-9].tiff
*	r - row
*	c - column
*	f - field
*	p - z-slice
*	ch - channel
*	sk - t-frame
*	fk - ???
*	fl - ???
*/
	
	public static void main(String[] args){
		try{	
			if(args==null||args.length==0){
				System.out.print(USAGE);
				return;
			}
			String path = args[0];
			int nThreads = 1;
			if(args.length>1){
				try{
				nThreads = Integer.valueOf( args[1] );
				}catch(NumberFormatException nfe){
					System.out.println(args[1]+" is not an integer");
					System.out.print(USAGE);
					return;
				}
			}
			if(nThreads<1){
				System.out.print("Invalid thread count: "+nThreads);
				System.out.print(USAGE);
				return;
			}
			long available = Runtime.getRuntime().maxMemory();
			long req = 1024*1024*500;	//require 500Mb per thread //TODO: predict requirement for each location somehow?
			//System.out.println( available+" / "+req );
			if( (available/nThreads)<req ){
				System.out.print( (available/1024/1024)+" Mb is not enough memory for "+nThreads+" threads\n"+
								  " - set higher with the java -Xmx*m arg or use fewer threads" );
				return;
			}
			Charles charles = new Charles(path, nThreads);
			
			final DirectoryStream<Path> dirstream = Files.newDirectoryStream(Paths.get(path));
			for (final Path entry: dirstream){
				String name = entry.getFileName().toString();
				if(name.matches( "r[0-9]{2}c[0-9]{2}f[0-9]{2}p[0-9]{2}-ch[0-9]sk[0-9]?[0-9]fk[0-9]fl[0-9].tiff" )){
					charles.add( name );
				}
			}
			charles.execute();
		}catch(Exception e){ JOptionPane.showMessageDialog(null, e.toString(), "Error", JOptionPane.ERROR_MESSAGE); }
	}

}
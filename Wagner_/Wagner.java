import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

//java -Xmx4000m -jar Wagner_.jar -D:\Lewis\small_opera_test\ 4
//cluster - use 60 Gb, 24 cores

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

public class Wagner{
private static final String USAGE = "Wagner - constructs stacks from TIFFs with names matching r[0-9]{2}c[0-9]{2}f[0-9]{2}p[0-9]{2}-ch[0-9]sk[0-9]?[0-9]fk[0-9]fl[0-9].tiff\n"
		 							+"usage: Wagner directory_path(string, required) thread_count(int, optional)";

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

			LocationManager charles = new LocationManager(path, nThreads);
			
			final DirectoryStream<Path> dirstream = Files.newDirectoryStream(Paths.get(path));
			for (final Path entry: dirstream){
				String name = entry.getFileName().toString();
				if(name.matches( "r[0-9]{2}c[0-9]{2}f[0-9]{2}p[0-9]{2}-ch[0-9]sk[0-9]?[0-9]fk[0-9]fl[0-9].tiff" )){
					charles.add( name );
				}
			}
			charles.execute();
		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}

}
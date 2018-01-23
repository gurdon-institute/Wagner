package cam.gurdon.wagner;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

public class LocationManager {
private ExecutorService executor;
private List<PlateLocation> locations;
private String path;
private int nThreads;
	
	public LocationManager(String path, int n) {
		try{
			this.path = path;
			this.nThreads = n;
			locations = new ArrayList<PlateLocation>();
			final DirectoryStream<Path> dirstream = Files.newDirectoryStream(Paths.get(path));
			int count = 0;
			for (final Path entry: dirstream){
				String name = entry.getFileName().toString();
				if(name.matches( "r[0-9]{2}c[0-9]{2}f[0-9]{2}p[0-9]{2}-ch[0-9]sk[0-9]?[0-9]fk[0-9]fl[0-9].tiff" )){
					add( name );
					count++;
				}
			}
			dirstream.close();
			System.out.println("Constructing images for "+path);
			System.out.println(count+" files, "+locations.size()+" plate locations using "+nThreads+" threads.");
		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}
	
	public void add(String name){
		try{
			PlateLocation loc = getLocation( name );
			if( loc == null ){
				loc = new PlateLocation(path, name);
				loc.addImage( name );
				locations.add( loc );
			}
			else{
				loc.addImage( name );
			}
		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}
	
	public PlateLocation getLocation( String str ){
		for(PlateLocation loc : locations){
			if(loc.matches(str)){
				return loc;
			}
		}
		return null;
	}

	public void execute(){
		
		if(nThreads>locations.size()){	//don't allocate more threads than locations
			nThreads = locations.size();
		}
		long available = Runtime.getRuntime().maxMemory();
		long total = 0L;
		for(PlateLocation pl : locations){
			total += pl.getSize();
		}
		long req = (total/locations.size()) * nThreads;
		if( available<req ){
			System.out.print( "Insufficient memory for "+locations.size()+" locations running on "+nThreads+" threads\n"+
							   (available/1024/1024)+" Mb available, "+(req/1024/1024)+" Mb required for "+locations.size()+" locations\n"+
							  " - set higher with the java -Xmx*m argument or use fewer threads\n" );
			return;
		}
		
		executor = Executors.newFixedThreadPool( nThreads );
		for(PlateLocation pl : locations){
			executor.execute( pl );
		}
		executor.shutdown();
		try {
			executor.awaitTermination(7L, TimeUnit.DAYS);
			System.out.println( "\nWagner finished "+locations.size()+" locations in "+path+"\n" );
			
			if(JOptionPane.showConfirmDialog(null, "Stitch?", "Stitch stacks?", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
				new Stitcher(locations, path);
			}
			
		} catch (InterruptedException ie) {
			System.out.println(ie.toString());
		}
	}
	
}

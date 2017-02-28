import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

class PlateLocation implements Runnable{
	int row, column, field;
	String path;
	List<String> files;
	private static final String foo = System.getProperty("file.separator");
	
	
	public PlateLocation(){	}
	
	public PlateLocation(String path, String loc){
		this.path = path;
		int rowi = loc.indexOf("r")+1;
		int columni = loc.indexOf("c")+1;
		int fieldi = loc.indexOf("f")+1;
		this.row = Integer.valueOf(loc.substring(rowi, rowi+2));
		this.column = Integer.valueOf(loc.substring(columni, columni+2));
		this.field = Integer.valueOf(loc.substring(fieldi, fieldi+2));
		files = new ArrayList<String>();
	}
	
	public void addFile(String file) {
		if(files.indexOf(file)==-1){
			files.add(file);
		}
	}

	public boolean matches(String loc){
		int rowi = loc.indexOf("r")+1;
		int columni = loc.indexOf("c")+1;
		int fieldi = loc.indexOf("f")+1;
		int locRow = Integer.valueOf(loc.substring(rowi, rowi+2));
		int locColumn = Integer.valueOf(loc.substring(columni, columni+2));
		int locField = Integer.valueOf(loc.substring(fieldi, fieldi+2));
		if(locRow==row&&locColumn==column&&locField==field){
			return true;
		}
		else{
			return false;
		}
	}
	
	public String toString(){
		return "row-"+row+" column-"+column+" field-"+field;
	}
	
	@Override
	public void run() {
		
		System.out.println( "building location "+toString() );
		File saveDir = new File(path+foo+"stacks");
		if(!saveDir.isDirectory()){
			saveDir.mkdir();
		}
		ImageStack stack = null;
		if(files.size()==0){
			System.out.println("No files for "+toString());
			return;
		}
		for(String file : files){
			
			int channeli = file.indexOf("ch")+2;
			int slicei = file.indexOf("p")+1;
			int framei = file.indexOf("sk")+2;
			int frameiEnd = file.indexOf("fk");
			int channel = (channeli==1) ? 0 : Integer.valueOf(file.substring(channeli, channeli+1));
			int slice = (slicei==0) ? 0 : Integer.valueOf(file.substring(slicei, slicei+2));
			int frame = (framei==1) ? 0 : Integer.valueOf(file.substring(framei, frameiEnd));
			
			ImagePlus image = IJ.openImage( path+foo+file );

			if(stack==null){
				stack = new ImageStack(image.getWidth(), image.getHeight());
			}
			stack.addSlice( "c"+channel+" z"+slice+" t"+frame, image.getProcessor() );
			image.flush();
			image.close();
		}
		if(stack.getSize()==0){
			System.out.println("No stack for "+toString());
			return;
		}
		//TODO: arrange into hyperstack
		System.out.println("Got stack for "+toString());
		ImagePlus imp = new ImagePlus( toString(), stack );
		IJ.saveAs(imp, "TIFF", path+foo+"stacks"+foo+toString()+".tiff");
		imp.close();
	}
	
}
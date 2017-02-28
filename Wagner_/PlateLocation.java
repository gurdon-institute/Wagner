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
		this.row = OperaParser.getValue(OperaParser.Dimension.ROW, loc);
		this.column = OperaParser.getValue(OperaParser.Dimension.COLUMN, loc);
		this.field = OperaParser.getValue(OperaParser.Dimension.FIELD, loc);
		files = new ArrayList<String>();
	}
	
	public void addFile(String file) {
		if(files.indexOf(file)==-1){
			files.add(file);
		}
	}

	public boolean matches(String loc){
		int locRow = OperaParser.getValue(OperaParser.Dimension.ROW, loc);
		int locColumn = OperaParser.getValue(OperaParser.Dimension.COLUMN, loc);
		int locField = OperaParser.getValue(OperaParser.Dimension.FIELD, loc);
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
			int channel = OperaParser.getValue(OperaParser.Dimension.CHANNEL, file);
			int slice = OperaParser.getValue(OperaParser.Dimension.SLICE, file);
			int frame = OperaParser.getValue(OperaParser.Dimension.FRAME, file);
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
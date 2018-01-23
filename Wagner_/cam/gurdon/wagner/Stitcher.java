package cam.gurdon.wagner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.HyperStackConverter;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class Stitcher {
	private static final String foo = System.getProperty("file.separator");
	ArrayList<Well> wells;
	int[][] arrange;
	String stackPath;
	
	class Well{
		private int row, col, maxf;
		private ArrayList<Integer> fields;
		private Well(int r, int c){
			row = r;
			col = c;
			fields = new ArrayList<Integer>();
		}
		private void addField(int field){
			if(fields.contains(field)){
				System.out.println("Well row "+row+" column "+col+" already contains field "+field);
			}
			else{
				fields.add(field);
				maxf = Math.max(maxf, field);
			}
		}
		String getTitle(int field){
			return "row-"+row+" column-"+col+" field-"+field+".tiff";
		}
		private int getNFields(){
			return fields.size();
		}
	}

	public Stitcher(List<PlateLocation> locations, String path){
		try{
			stackPath = path+foo+"stacks"+foo;
			ArrayList<int[]> wellrcs = new ArrayList<int[]>();
			wells = new ArrayList<Well>();
			for(PlateLocation loc:locations){
				int[] rc = new int[]{loc.row,loc.column};
				boolean got = false;
				for(int[] pair:wellrcs){
					if(pair[0]==rc[0]&&pair[1]==rc[1]){
						got = true;
						break;
					}
				}
				if(!got){
					wellrcs.add(rc);
					wells.add(new Well(rc[0], rc[1]));
				}
			}
			for(Well well:wells){
				for(PlateLocation loc:locations){
					if(loc.row==well.row&&loc.column==well.col){
						well.addField(loc.field);
					}
				}
			}
			Arranger arranger = new Arranger(wells.get(0).maxf, this);
			
		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}
	
	//default constuctor for testing
	public Stitcher() {
		
	}

	public void stitch(int[][] arrange, double overlapPercent) {
		
		double olF = (100d-overlapPercent)/100d;
		
		for(int w=0;w<wells.size();w++){
			Well well = wells.get(w);
			if(well.getNFields()<2) continue;
			ImagePlus[][] tiles = new ImagePlus[arrange.length][arrange[0].length];
			int totalW = 0;
			int totalH = 0;
			int W = 0;
			int H = 0;
			int C = 0;
			int Z = 0;
			int T = 0;
			for (int y = 0; y < arrange.length; y++) {
				for (int x = 0; x < arrange[y].length; x++) {
					int f = arrange[y][x];
					if(f==0) continue;
					tiles[y][x] = IJ.openImage( stackPath+well.getTitle(f) );
					totalW = Math.max(totalW, tiles[y][x].getWidth()*(x+1));
					totalH = Math.max(totalH, tiles[y][x].getHeight()*(y+1));
					W = Math.max(W, tiles[y][x].getWidth());
					H = Math.max(H, tiles[y][x].getHeight());
					C = Math.max(C, tiles[y][x].getNChannels());
					Z = Math.max(Z, tiles[y][x].getNSlices());
					T = Math.max(T, tiles[y][x].getNFrames());
				}
			}

			ImageStack stack = new ImageStack((int)(totalW*olF), (int)(totalH*olF));
			
			for(int z=0;z<Z;z++){
				ImageProcessor ip = new ShortProcessor((int)(totalW*olF), (int)(totalH*olF));
				for (int y = 0; y < tiles.length; y++) {
					for (int x = 0; x < tiles[y].length; x++) {
						ImageProcessor tp = null;
						if(tiles[y][x]==null) continue;
						if(C>1||Z>1||T>1){
							tp = tiles[y][x].getImageStack().getProcessor(z+1);
						}
						else{
							tp = tiles[y][x].getProcessor();
						}
						int xp = (int)( x*tiles[y][x].getWidth()*olF );
						int yp = (int)( y*tiles[y][x].getHeight()*olF );
						//ip.copyBits(tp, xp, yp, Blitter.AVERAGE);
						ip.copyBits(tp, xp, yp, Blitter.MAX);
					}
				}
				stack.addSlice("slice "+(z+1), ip);
			}
			
			Z /= C;
			Z /= T;
			ImagePlus mosaic = new ImagePlus("mosaic", stack);
			//System.out.println(mosaic.getNSlices()+" == "+C+"*"+Z+"*"+T);
			if(C>1||Z>1||T>1){
				mosaic = HyperStackConverter.toHyperStack(mosaic, C, Z, T, "xyczt", "composite");
			}
			IJ.saveAs(mosaic, "TIFF", stackPath+"mosaic");
			System.out.println("Saved stitched mosaic in "+stackPath);
mosaic.show();	//TEST
			//mosaic.close();
		}
	}
	
/*	public void stitchA(int[][] arrange) {
		for (int y = 0; y < arrange.length; y++) {
			System.out.println(Arrays.toString(arrange[y]));
		}
		
//////////////////TEST
		
		if(true){
			File file = new File(stackPath+wells.get(0)+"row-1 column-2 field-1.tiff");
			System.out.println(file.getAbsolutePath()+" "+file.exists());
			return;
		}
		
		//ImagePlus imp1 = IJ.openImage( stackPath+wells.get(0).getTitle(0) ); //FIXME: images not found
		//ImagePlus imp2 = IJ.openImage( stackPath+wells.get(0).getTitle(1) );
		
		ImagePlus imp1 = IJ.openImage( stackPath+"row-1 column-2 field-1.tiff" );
		ImagePlus imp2 = IJ.openImage( stackPath+"row-1 column-2 field-2.tiff" );
		
		StitchingParameters params = new StitchingParameters();
		params.cpuMemChoice = 1;
		params.dimensionality = 3;
		params.xOffset = imp1.getWidth(); //TODO: offsets
		params.yOffset = 0;
		
		Stitching_Pairwise.performPairWiseStitching(imp1, imp2, params);
///////////////////////
		
		
		StitchingParameters params = new StitchingParameters();
		params.cpuMemChoice = 1;
		params.dimensionality = 3;
		params.fusionMethod = 0;	//0=blending pixel fusion, 1=average pixel fusion, 2=median pixel fusion, 3=max pixel fusion, 4=min pixel fusion, 5=overlap fusion
		params.computeOverlap = false;
		params.displayFusion = false;
		
		int W=0, H=0;
		for(int w=0;w<wells.size();w++){
			Well well = wells.get(w);
			ImagePlus[] rows = new ImagePlus[arrange.length];
			for (int y = 0; y < arrange.length; y++) {
				int f = arrange[y][0];
				if(f==0) continue;
				rows[y] = IJ.openImage( stackPath+well.getTitle(f) );
				W = rows[y].getWidth();
				H = 0;
				for (int x = 1; x < arrange[y].length; x++) {
					String name = x+","+y+"_"+f;
					int addF = arrange[y][x];
					if(addF==0) continue;
					ImagePlus addImp = IJ.openImage( stackPath+well.getTitle(addF) );
					params.xOffset = W*x;
					params.yOffset = 0;
					params.fusedName = name;
					Stitching_Pairwise.performPairWiseStitching(rows[y], addImp, params);
					rows[y] = WindowManager.getImage(name);
				}
			}
			ImagePlus full = rows[0];
			for (int y = 1; y < arrange.length; y++) {
				params.xOffset = 0;
				params.yOffset = H*y;
				params.fusedName = "full";
				Stitching_Pairwise.performPairWiseStitching(full, rows[y], params);
				full = WindowManager.getImage("full");
			}
			full.show();
		}
		
	}*/
	
}

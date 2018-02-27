package cam.gurdon.wagner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.HyperStackConverter;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class Stitcher {
	private static final String foo = System.getProperty("file.separator");
	
	static final float[] V_KERNEL = {
			0f, 0f, 0f, 0f, 0f,
			0f, 0f, 0f, 0f, 0f,
			0.2f, 0.2f, 0.2f, 0.2f, 0.2f,
			0f, 0f, 0f, 0f, 0f,
			0f, 0f, 0f, 0f, 0f
	};
	
	static final float[] H_KERNEL = {
			0f, 0f, 0.2f, 0f, 0f,
			0f, 0f, 0.2f, 0f, 0f,
			0f, 0f, 0.2f, 0f, 0f,
			0f, 0f, 0.2f, 0f, 0f,
			0f, 0f, 0.2f, 0f, 0f
	};
	
	
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
		String getRowColumn(){
			return "row-"+row+" column-"+col;
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
			if(wells.size()==0){
				JOptionPane.showMessageDialog(null, "No stacks found in "+stackPath, "No stacks found", JOptionPane.ERROR_MESSAGE);
				System.out.println(locations);
				return;
			}
			for(Well well:wells){
				for(PlateLocation loc:locations){
					if(loc.row==well.row&&loc.column==well.col){
						well.addField(loc.field);
					}
				}
			}
			System.out.println("Got "+wells.size()+" wells with "+locations.size()+" locations for stitching");
			
			new Arranger(wells.get(0).maxf, this);	//calls back to stitch(int[][] arrange, double overlapPercent) when arrangement is oked

		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}

	public void stitch(int[][] arrange, double overlapPercent) {
		
		//remove empty rows and columns
		int y0 = Integer.MAX_VALUE;
		int y1 = Integer.MIN_VALUE;
		int x0 = Integer.MAX_VALUE;
		int x1 = Integer.MIN_VALUE;
		for (int y = 0; y < arrange.length; y++) {
			for (int x = 0; x < arrange[y].length; x++) {
				if(arrange[y][x]>0){
					y0 = Math.min(y0, y);
					y1 = Math.max(y1, y);
					x0 = Math.min(x0, x);
					x1 = Math.max(x1, x);
				}
			}
		}
		int yn = y1-y0+1;
		int xn = x1-x0+1;
		int[][] keep = new int[yn][xn];
		for(int y=0;y<yn;y++){
			for(int x=0;x<xn;x++){
				keep[y][x] = arrange[y0+y][x0+x];
			}
		}
		arrange = keep;
		keep = null;
	
		int skip = 0;
		
		double olF = (100d-overlapPercent)/100d;
		for(int w=0;w<wells.size();w++){
			Runtime rt = Runtime.getRuntime();
			System.out.println( "Memory use : "+ (rt.freeMemory()/ 1000000) + " / " + (rt.totalMemory()/ 1000000) + " MB");
			Well well = wells.get(w);
			String mosaicName = well.getRowColumn()+" mosaic";
			File mosFile = new File(stackPath+mosaicName+".tif");
			if(mosFile.exists()){
				if(skip==1){
					continue;
				}
				else if(skip==0){ //unset
					if(JOptionPane.showConfirmDialog(null, mosFile.getAbsolutePath()+" already exists.\nSkip stitching for wells with existing mosaics?", "Skip?", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
						skip = 1;
						continue;
					}
					else{
						skip = -1;
					}
				}
			}
			if(well.getNFields()<2) continue;
			ImagePlus[][] tiles = new ImagePlus[arrange.length][arrange[0].length];
			int totalW = 0;
			int totalH = 0;
			int W = 0;
			int H = 0;
			int C = 0;
			int Z = 0;
			int T = 0;
			int prog = 0; 
			int total = arrange.length*arrange[0].length;
			for (int y = 0; y < arrange.length; y++) {
				for (int x = 0; x < arrange[y].length; x++) {
					int f = arrange[y][x];
					if(f==0) continue;
					tiles[y][x] = IJ.openImage( stackPath+well.getTitle(f) );
					totalW = (int) Math.max(totalW, tiles[y][x].getWidth()*(x+1)*olF);
					totalH = (int) Math.max(totalH, tiles[y][x].getHeight()*(y+1)*olF);
					W = Math.max(W, tiles[y][x].getWidth());
					H = Math.max(H, tiles[y][x].getHeight());
					C = Math.max(C, tiles[y][x].getNChannels());
					Z = Math.max(Z, tiles[y][x].getNSlices());
					//System.out.println("C = "+C+" Z = "+Z);
					T = Math.max(T, tiles[y][x].getNFrames());
					prog++;
					System.out.println( "Well "+w+" tiles : "+prog+"/"+total );
				}
			}
			System.out.println( "Well "+w+" : stitching mosaic..." );
			
			//sort out overlap lengths for 1 or >1 tiles
			if(arrange.length>1) totalH += (totalH-(totalH*olF))/2d;
			else totalH += (totalH-(totalH*olF));
			if(arrange[0].length>1) totalW += (totalW-(totalW*olF))/2d;
			else totalW += (totalW-(totalW*olF));
			
			ImageStack stack = new ImageStack((int)(totalW), (int)(totalH));
			for (int t = 0; t < T; t++) {
				for (int z = 0; z < Z; z++) {
					for (int c = 0; c < C; c++) {
						ImageProcessor ip = new ShortProcessor((int) (totalW), (int) (totalH));
						for (int y = 0; y < tiles.length; y++) {
							for (int x = 0; x < tiles[y].length; x++) {

								ImageProcessor tp = null;
								if (tiles[y][x] == null)
									continue;

								tiles[y][x].setPosition(c+1, z+1, t+1);
								tp = tiles[y][x].getProcessor();

								int xp = (int) (x * tiles[y][x].getWidth() * olF);
								int yp = (int) (y * tiles[y][x].getHeight() * olF);
								// ip.copyBits(tp, xp, yp, Blitter.AVERAGE);
								ip.copyBits(tp, xp, yp, Blitter.MAX);
							}
						}
						stack.addSlice("slice " + (z + 1), ip);
					}
				}
			}

			
			ImagePlus mosaic = new ImagePlus(mosaicName, stack);
			if(C>1||Z>1||T>1){
				mosaic = HyperStackConverter.toHyperStack(mosaic, C, Z, T, "xyczt", "composite");
			}
			
			if(mosFile.exists()){	//the well wasn't skipped, so delete the existing mosiac
				mosFile.delete();
			}
			IJ.saveAs(mosaic, "TIFF", stackPath+mosaicName);
			System.out.println("Saved stitched mosaic: "+stackPath+mosaicName+".tif\n");
			mosaic.close();
			
/////////////////////// memory "optimisation"
			well = null;
			mosFile = null;
			mosaic.flush();
			mosaic = null;
			for (int y = 0; y < tiles.length; y++) {
				for (int x = 0; x < tiles[y].length; x++) {
					tiles[y][x].close();
					tiles[y][x].flush();
					tiles[y][x] = null;
				}
			}
			tiles = null;
			stack = null;
//////////////////////
		
		}
		System.out.println("Stitching complete for "+wells.size()+" wells.\n");
	}
	
}

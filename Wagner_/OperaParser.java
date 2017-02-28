
public class OperaParser {

	public static enum Dimension{
		ROW, COLUMN, FIELD, CHANNEL, SLICE, FRAME;
	}
	
	public OperaParser() {
		throw new UnsupportedOperationException("OperaParser has only static methods");
	}

	public static int getValue(Dimension dim, String name) throws NumberFormatException{	//TODO: report instead of just throwing up
		int i = -1;
		switch(dim){
			case ROW:
				int rowi = name.indexOf("r")+1;
				i = Integer.valueOf(name.substring(rowi, rowi+2));
				break;
			case COLUMN:
				int columni = name.indexOf("c")+1;
				i = Integer.valueOf(name.substring(columni, columni+2));
				break;
			case FIELD:
				int fieldi = name.indexOf("f")+1;
				i = Integer.valueOf(name.substring(fieldi, fieldi+2));
				break;
			case CHANNEL:	
				int channeli = name.indexOf("ch")+2;
				i = (channeli==1) ? 0 : Integer.valueOf(name.substring(channeli, channeli+1));
				break;
			case SLICE:
				int slicei = name.indexOf("p")+1;
				i = (slicei==0) ? 0 : Integer.valueOf(name.substring(slicei, slicei+2));
				break;
			case FRAME:
				int framei = name.indexOf("sk")+2;
				int frameiEnd = name.indexOf("fk");
				i = (framei==1) ? 0 : Integer.valueOf(name.substring(framei, frameiEnd));
				break;
			default:
				break;
		}
		return i;
	}
	
	public static int getRow(String name){
		int rowi = name.indexOf("r")+1;
		int row = Integer.valueOf(name.substring(rowi, rowi+2));
		return row;
	}
	public static int getColumn(String name){
		int columni = name.indexOf("c")+1;
		int column = Integer.valueOf(name.substring(columni, columni+2));
		return column;
	}
	public static int getField(String name){
		int fieldi = name.indexOf("f")+1;
		int field = Integer.valueOf(name.substring(fieldi, fieldi+2));
		return field;
	}
	public static int getChannel(String name){
		int channeli = name.indexOf("ch")+2;
		int channel = (channeli==1) ? 0 : Integer.valueOf(name.substring(channeli, channeli+1));
		return channel;
	}
	public static int getSlice(String name){
		int slicei = name.indexOf("p")+1;
		int slice = (slicei==0) ? 0 : Integer.valueOf(name.substring(slicei, slicei+2));
		return slice;
	}
	public static int getFrame(String name){
		int framei = name.indexOf("sk")+2;
		int frameiEnd = name.indexOf("fk");
		int frame = (framei==1) ? 0 : Integer.valueOf(name.substring(framei, frameiEnd));
		return frame;
	}
}

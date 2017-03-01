
public class OperaTiff {
public int row, column, field, channel, slice, frame;
public String name;

	public OperaTiff(String str) {
		this.name = str;
		this.row = OperaParser.getValue(OperaParser.Dimension.ROW, str);
		this.column = OperaParser.getValue(OperaParser.Dimension.COLUMN, str);
		this.field = OperaParser.getValue(OperaParser.Dimension.FIELD, str);
		this.channel = OperaParser.getValue(OperaParser.Dimension.CHANNEL, str);
		this.slice = OperaParser.getValue(OperaParser.Dimension.SLICE, str);
		this.frame = OperaParser.getValue(OperaParser.Dimension.FRAME, str);
	}

}

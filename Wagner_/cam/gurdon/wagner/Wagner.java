package cam.gurdon.wagner;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;


/*	submitWagner.sh
#!/bin/bash
#SBATCH --job-name=Wagner
#SBATCH --partition=1604
#SBATCH --distribution=cyclic:block
#SBATCH --ntasks=12
#SBATCH --mail-type=END
#SBATCH --mail-user=rsb48@cam.ac.uk

echo -e "JobID: $SLURM_JOB_ID\n====="
echo "Time: `date`"
echo "Running on master node: `hostname`"
echo "Current directory: `pwd`"
echo -e "\nExecuting command: $WAGNER_CMD\n====="

eval $WAGNER_CMD
*/
//export WAGNER_CMD="java -Xmx30000m -jar Wagner.jar -/mnt/DATA/home1/imaging/rsb48/data/ 12"
//sbatch ./submitWagner.sh
//cat slurm-jobno.out

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
private static final String USAGE = "Wagner - by Richard Butler, Gurdon Institute Imaging Facility\n"
									+"constructs stacks from TIFFs with names matching r[0-9]{2}c[0-9]{2}f[0-9]{2}p[0-9]{2}-ch[0-9]sk[0-9]?[0-9]fk[0-9]fl[0-9].tiff\n"
		 							+"usage: Wagner directory_path(string, required for headless mode) thread_count(int, optional)\n"
		 							+"run with no arguments for GUI mode.";
//~~~~~GUI mode things~~~~~
private JFrame gui;
private JLabel pathLabel;
private JSpinner threadSpin;
private static final String prefPath = System.getProperty("user.home")+File.separator+"Wagner.cfg";
private String path = System.getProperty("user.home");
private int threads = 2;
//~~~~~~~~~~

	public static void main(String[] args){
		try{
			String path = "";
			int nThreads = 0;
			if(args.length == 0){
				new Wagner().showGui();
				return;
			}
			else{
				path = args[0];
				nThreads = 1;
			}
			if(args.length > 1){
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
	
			LocationManager manager = new LocationManager(path, nThreads);
			manager.execute();
		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}
	
	private class Log extends OutputStream{
		private JTextArea text;
		private StringBuilder sb;
		
		public Log(){
			text = new JTextArea(){
				private static final long serialVersionUID = 8332767163052368928L;
				public Dimension getPreferredSize(){
					return new Dimension(700,400);
				}
			};
			text.setEditable(false);
			text.setText(USAGE);
			sb = new StringBuilder();
		}
		
		public JTextArea getTextArea(){
			return text;
		}
		
		@Override
		public synchronized void write(int b) throws IOException {
			if (b == '\n') {
	            final String str = sb.toString() + "\n";
	            SwingUtilities.invokeLater(new Runnable() {
	                @Override
	                public void run() {
	                    text.append(str);
	                }
	            });
	            sb.setLength(0);
	            return;
	        }

	        sb.append((char) b);
		}
		
	}
	
	private void savePrefs(){
		try{
			PrintWriter writer = new PrintWriter(prefPath);
			writer.println(path);
			writer.println(threads);
			writer.close();
		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}
	
	private void loadPrefs(){
		try{
			File cfg = new File(prefPath);
			if(cfg.exists()){
				List<String> lines = Files.readAllLines(Paths.get(prefPath));
				path = lines.get(0);
				threads = Integer.valueOf(lines.get(1));
			}
			else{
				savePrefs();	//save defaults
			}
		}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
	}
	
	private void showGui(){
		
		loadPrefs();
		
		ActionListener listen = new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				try{
					String event = ae.getActionCommand();
					if(event.equals("Run")){
						threads = (int)threadSpin.getValue();
						SwingWorker<Void, String> worker = new SwingWorker<Void, String>(){
							public Void doInBackground(){
								LocationManager manager = new LocationManager(path, threads);
								manager.execute();
								savePrefs();
								return null;
							}
						};
						worker.execute();
					}
					else if(event.equals("Cancel")){
						gui.dispose();
					}
					else if(event.equals("Path...")){
						JFileChooser chooser = new JFileChooser();
						chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					    FileNameExtensionFilter filter = new FileNameExtensionFilter("Images", "tif", "tiff");
					    chooser.setFileFilter(filter);
					    chooser.setSelectedFile(new File(path));
					    if(chooser.showOpenDialog(gui) == JFileChooser.APPROVE_OPTION) {
					    	try{
						    	path = chooser.getSelectedFile().getAbsolutePath();
						    	pathLabel.setText(path);
					    	}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
					    }
					}
				}catch(Exception e){ System.out.print( e.toString()+"\n"+Arrays.toString(e.getStackTrace()).replace(",","\n"));}
			}
		};
		
		Log log = new Log();
		PrintStream outStream = new PrintStream(log);
		System.setOut(outStream);
		System.setErr(outStream);
		
		gui = new JFrame("Wagner");
		gui.setLayout(new BoxLayout(gui.getContentPane(), BoxLayout.Y_AXIS));
		gui.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("logo_icon.gif")));
		gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		gui.add(new JScrollPane(log.getTextArea()));
		
		JPanel controlPan = new JPanel();
		pathLabel = new JLabel(path);
		controlPan.add(pathLabel);
		JButton pathButton = new JButton("Path...");
		pathButton.addActionListener(listen);
		controlPan.add(pathButton);
		controlPan.add(Box.createHorizontalStrut(20));
		controlPan.add(new JLabel("Threads:"));
		SpinnerModel model = new SpinnerNumberModel(threads, 1, 32, 1);
		threadSpin = new JSpinner(model);
		controlPan.add(threadSpin);
		gui.add(controlPan);
		
		JPanel buttonPan = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
		JButton run = new JButton("Run");
		run.addActionListener(listen);
		buttonPan.add(run);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(listen);
		buttonPan.add(cancel);
		gui.add(buttonPan);
		
		gui.pack();
		gui.setLocationRelativeTo(null);
		gui.setVisible(true);
	}
	
}
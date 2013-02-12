import java.util.Collections;

import javax.sound.midi.*;
import javax.sound.sampled.*;

public class soundconverter {
	/**
	 * @param args
	 */
	private static java.util.ArrayList<MidiEvent> _combinedTrack;
	private static java.util.ArrayList<Integer> _tracks;
	
	public static void main(String[] args) {
		String sFile = "";
		_combinedTrack = new java.util.ArrayList<MidiEvent>();
		_tracks = new java.util.ArrayList<Integer>();
		boolean bCombine = false;
		boolean bJustPlay = false;
		//Parse command line arguments
		try{
			for(int a=0; a<args.length; a++){
				if(args[a].indexOf('-') != -1){
					if(args[a].equalsIgnoreCase("-f")) sFile = args[a+1];
					if(args[a].equalsIgnoreCase("-t")) {
						String[] tracks = args[a+1].split(",");
						for(int t=0; t<tracks.length; t++){
							_tracks.add(Integer.parseInt(tracks[t]));
						}
					}
					if(args[a].equalsIgnoreCase("-p")) bJustPlay = true;
					if(args[a].equalsIgnoreCase("-h")) {ShowUsage(); return;}
					if(args[a].equalsIgnoreCase("-c")) bCombine = true; 
				}
			}
		}catch(Exception e){
			ShowUsage();
		}
		
		if(sFile.toLowerCase().indexOf(".wav")!=-1) ParseSampledData(sFile);
		if(sFile.toLowerCase().indexOf(".mid")!=-1) ParseMidiData(sFile,bCombine,bJustPlay);
	}
	
	private static void ShowUsage(){
		System.out.println("soundconverter is a program to convert wav and midi files into");
		System.out.println("usable c code for the Virtual Boy Game System. The timings and");
		System.out.println("calculations used in this program have been preset to the VB's specs.");
		System.out.println("This program writes it's text output to standard out so you'll need to");
		System.out.println("redirect the output to a file if you want it saved. The paranthesis are");
		System.out.println("needed in Linux since the java program will read > as a paramter otherwise.");
		System.out.println("Example: (java soundconverter -f mymusicfile.mid) > outputfile.txt");
		System.out.println("Feel free to modify this program to meet you're needs.");
		System.out.println("");
		System.out.println("Usage: java soundconverter -{param} {param value}");
		System.out.println("  Params: (x=alphanumeric, n=numeric");
		System.out.println("  -f {x..} =         File name and path to convert");
		System.out.println("  -p       =         Play file. Can be combined with -t to only play certain tracks");
		System.out.println("  -t {n..} =         Track number to play and/or convert");
		System.out.println("  -h       =         Show this help menu");
	}
	
	/**
	 * Plays a MIDI file potentially limiting the tracks if so requested
	 * @param s Sequence containing the song information
	 * @param t List of tracks wanting to play
	 */
	private static void PlayFile(Sequence s, java.util.ArrayList<Integer>t){
		try{
			Sequencer seq = MidiSystem.getSequencer();
			seq.open();
			seq.setSequence(s);
			boolean bPlay = false;
			for(int tr=0; tr<s.getTracks().length; tr++){
				if(t.size()==0) bPlay = true;
				else{
					for(int i=0; i<t.size(); i++){
						if(tr == t.get(i)) bPlay = true;
					}
				}
				if(!bPlay) seq.setTrackMute(tr, true);
				bPlay = false;
			}
			seq.start();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * This function will order all the events by tick position
	 * @param s The sequence containing all the tracks
	 */
	private static void CombineTracks(Sequence s, java.util.ArrayList<Integer>t){
		Track[] tracks = s.getTracks();
		boolean bValid = false;
		for(int i=0; i<tracks.length; i++){
			if(t.size()==0) bValid = true;
			else{
				for(int j=0; j<t.size(); j++){
					if(t.get(j) == i) bValid = true;
				}
			}
			if(bValid){
				int numEvents = tracks[i].size();
				for(int e=0; e<numEvents; e++){
					_combinedTrack.add(tracks[i].get(e));
				}
			}
			bValid = false;
		}
		//Sort List
		Collections.sort(_combinedTrack, new java.util.Comparator<MidiEvent>(){
			@Override
			public int compare(MidiEvent arg0, MidiEvent arg1) {
				return (int)(arg0.getTick() - arg1.getTick());
			}
			
		});
		
		System.out.println("Total Events: " + _combinedTrack.size());
	}
	/**
	 * This function does the MIDI audio parsing and information displays
	 * @param sFile The file name of a MIDI file
	 */
	private static void ParseMidiData(String sFile, boolean combine, boolean justPlay){
		try{
			java.io.File i = new java.io.File(sFile);
			Sequence s = MidiSystem.getSequence(i);
			if(justPlay){
				PlayFile(s,_tracks); 
				return;
			}
			ShowMidiInfo(s);
			if(combine){
				CombineTracks(s, _tracks);
				//Delete all current tracks
				while(s.getTracks().length > 0){
					s.deleteTrack(s.getTracks()[0]);
				}
				//Add in the new track
				Track newT = s.createTrack();
				for(int e=0; e<_combinedTrack.size(); e++){
					newT.add(_combinedTrack.get(e));
				}
			}
			ParseData(s);	
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * This function does the sampled audio parsing and information displays
	 * @param sFile The filename of a WAV file to be parsed
	 */
	private static void ParseSampledData(String sFile){
		try {
			java.io.File i = new java.io.File(sFile);
			AudioInputStream a = AudioSystem.getAudioInputStream(i);
			AudioFormat f = a.getFormat();
			
			ShowSampledInfo(f);
			ParseData(a);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Parses a sampled audio stream and writes the bytes out as an array to standard out
	 * @param i A Sampled AudioInputStream Object
	 */
	private static void ParseData(AudioInputStream i){
		byte[] buffer = new byte[1024*1024*5];
		int bytesRead = 0;
		int totalBytes = 0;
		StringBuffer sb = new StringBuffer();
		try{
			while((bytesRead = i.read(buffer))>0){
				for(int b=0; b<bytesRead; b++){
					sb.append(String.format((b==0)?(""):(",") + "0x%02x", buffer[b]).toUpperCase());
					if((b & 15)==15) sb.append("\n");
				}
				totalBytes += bytesRead;
			}
			sb.append("\n};");
			System.out.print("#define DATA_LEN " + totalBytes + "\n");
			System.out.print("const u8 data["+totalBytes+"] = {\n");
			System.out.print(sb.toString());
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Parse the MIDI data bytes and write them out to the stream
	 * @param s a MIDI Sequence object
	 */
	private static void ParseData(Sequence s){
		Track[] tracks = s.getTracks();
		StringBuffer sb = new StringBuffer();
		sb.append("typedef struct {\n");
		sb.append("\tu8  onoff;\n");
		sb.append("\tu32 tickPosition;\n");
		sb.append("\tu8  note;\n");
		sb.append("\tu8  volume;\n");
		sb.append("} midiNote;\n");
		boolean bValidTrack = false;
		for(int t=0; t<tracks.length; t++){
			if(_tracks.size()==0) bValidTrack = true;
			else{
				for(int tr=0; tr<_tracks.size(); tr++){
					if (t == _tracks.get(tr)) bValidTrack = true;
				}
			}
			if(!bValidTrack)continue;
			bValidTrack = false;
			sb.append("/*New Track******\n");
			sb.append("Number of ticks: " + tracks[t].ticks() + "\n");
			sb.append("*/\n");
			sb.append("const midiNote track_" + t + "[] = {\n");
			int bytecount = 0;
			for(int e=0; e<tracks[t].size(); e++){
				MidiEvent me = tracks[t].get(e);
				MidiMessage mess = me.getMessage();
				if(validMessage(mess)){
					if(((mess.getStatus()>>4) == MidiStatus.NOTE_ON) ||
						((mess.getStatus()>>4) == MidiStatus.NOTE_OFF)){
						sb.append(String.format(((bytecount==0)?(""):(",")) + "{0x%02x,0x%08x", (mess.getStatus()>>4), me.getTick()));
						bytecount++;
						for(int b=1;b<mess.getMessage().length; b++){
							sb.append(String.format(",0x%02x", mess.getMessage()[b]));
							bytecount++;
						}
						sb.append("}");
						
					}
					if((mess.getStatus()>>4) == MidiStatus.PROGRAM_CHANGE){
						try{
							sb.append("//Instrument=" + MidiSystem.getSynthesizer().getAvailableInstruments()[mess.getMessage()[1]].getName() + "\n");
						}catch(Exception ex){
							ex.printStackTrace();
						}
					}else{
						sb.append(" //Tick=" + me.getTick() + "\n");
					}
				}
			}
			sb.append(",{0x09,0xFFFFFFFF,0x00,0x00} //Ending byte\n");
			sb.append("};\n\n");
		}
		
		System.out.print(sb.toString());
	}
	
	/**
	 * This function will determine which messages we actually care about
	 * @param m A MidiMessage object
	 * @return boolean
	 */
	private static boolean validMessage(MidiMessage m){
		boolean r = false;
		if((m.getStatus()>>4) == MidiStatus.NOTE_ON) r = true;
		if((m.getStatus()>>4) == MidiStatus.NOTE_OFF) r = true;
		if((m.getStatus()>>4) == MidiStatus.PROGRAM_CHANGE) r = true;
		return r;
	}
	
	/**
	 * Show all of the details of a MIDI file that are available
	 * @param s MIDI Sequence object
	 */
	private static void ShowMidiInfo(Sequence s){
		try{
			Sequencer seq = MidiSystem.getSequencer();
			seq.setSequence(s);
			
			System.out.println("/*");
			System.out.println("File Information:");
			System.out.println("Division type:                 " + 
			((s.getDivisionType()==Sequence.PPQ)?("PPQ"):
				((s.getDivisionType()==Sequence.SMPTE_24)?("SMPTE_24"):
					((s.getDivisionType()==Sequence.SMPTE_25)?("SMPTE_25"):
						((s.getDivisionType()==Sequence.SMPTE_30)?("SMPTE_30"):
							((s.getDivisionType()==Sequence.SMPTE_30DROP)?("SMPTE_30DROP"):("UNKNOWN")
									)
								)
							)
						)
					)
				);
			System.out.println("Number of tracks:              " + s.getTracks().length);
			System.out.println("Number of patches:             " + s.getPatchList().length);
			System.out.println("Total length in Ticks:         " + seq.getTickLength());
			System.out.println("Tempo factor:                  " + seq.getTempoFactor());
			System.out.println("Tempo beats per minute:        " + seq.getTempoInBPM());
			double ticksPerSecond = (s.getResolution() * (seq.getTempoInBPM()/60));
			System.out.println("Ticks per second:              " + ticksPerSecond);
			System.out.println("20us clock interrupt:          " + 50000/ticksPerSecond);
			System.out.println("100us clock interrupt:         " + 10000/ticksPerSecond);
			System.out.println("*/");
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Show all of the details of a WAV file that are available
	 * @param f Sampled AudioFormat object
	 */
	private static void ShowSampledInfo(AudioFormat f){
		System.out.println("/*");
		System.out.println("File Information:");
		System.out.println("Number of Channels:  " + f.getChannels());
		System.out.println("Frame rate:          " + f.getFrameRate());
		System.out.println("Frame size:          " + f.getFrameSize());
		System.out.println("Sample rate:         " + f.getSampleRate());
		System.out.println("Sample size in bits: " + f.getSampleSizeInBits());
		System.out.println("Is big endian:       " + f.isBigEndian());
		System.out.println("Encoding is:         " + f.getEncoding().toString());
		System.out.println("*/\n");
	}

}

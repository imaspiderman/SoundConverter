import javax.sound.midi.*;
import javax.sound.sampled.*;

public class soundconverter {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 1){
			System.out.println("usage soundconverter <filename>");
			return;
		}
		
		if(args[0].toLowerCase().indexOf(".wav")!=-1) ParseSampledData(args[0]);
		if(args[0].toLowerCase().indexOf(".mid")!=-1) ParseMidiData(args[0]);
	}
	
	/**
	 * This function does the MIDI audio parsing and information displays
	 * @param sFile The file name of a MIDI file
	 */
	private static void ParseMidiData(String sFile){
		try{
			java.io.File i = new java.io.File(sFile);
			Sequence s = MidiSystem.getSequence(i);
			ShowMidiInfo(s);
			ParseData(s);
			/*
			Sequencer seq = MidiSystem.getSequencer();
			seq.open();
			seq.setSequence(s);
			seq.start();
			*/
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
		for(int t=0; t<tracks.length; t++){
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
			sb.append(",{0xFFFFFFFF,0x00,0x00} //Ending byte\n");
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
			//System.out.println("Resolution:                    " + s.getResolution());
			System.out.println("Number of tracks:              " + s.getTracks().length);
			System.out.println("Number of patches:             " + s.getPatchList().length);
			//System.out.println("Total length of sequence:      " + seq.getMicrosecondLength());
			System.out.println("Total length in Ticks:         " + seq.getTickLength());
			System.out.println("Tempo factor:                  " + seq.getTempoFactor());
			System.out.println("Tempo beats per minute:        " + seq.getTempoInBPM());
			//System.out.println("Tempo micro secs per qtr Note: " + seq.getTempoInMPQ());
			double ticksPerSecond = (s.getResolution() * (seq.getTempoInBPM()/60));
			//double tickSize = 1/ticksPerSecond;
			System.out.println("Ticks per second:              " + ticksPerSecond);
			System.out.println("20us clock interrupt:          " + 50000/ticksPerSecond);
			System.out.println("100us clock interrupt:         " + 10000/ticksPerSecond);
			//System.out.println("Tick size:                     " + tickSize);
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

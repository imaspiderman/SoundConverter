
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
	}
	
	private static void ParseSampledData(String sFile){
		try {
			java.io.File i = new java.io.File(sFile);
			javax.sound.sampled.AudioInputStream a =
				javax.sound.sampled.AudioSystem.getAudioInputStream(i);
			javax.sound.sampled.AudioFormat f = a.getFormat();
			
			ShowInfo(f);
			ParseData(a);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void ParseData(javax.sound.sampled.AudioInputStream i){
		byte[] buffer = new byte[1024*1024*5];
		int bytesRead = 0;
		int totalBytes = 0;
		java.lang.StringBuffer sb = new java.lang.StringBuffer();
		try{
			while((bytesRead = i.read(buffer))>0){
				for(int b=0; b<bytesRead; b++){
					sb.append(String.format((b==0)?(""):(",") + "%02x", buffer[b]).toUpperCase());
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
	
	private static void ShowInfo(javax.sound.sampled.AudioFormat f){
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

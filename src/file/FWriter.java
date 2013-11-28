package file;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FWriter {
	private FileWriter fw;
	private BufferedWriter bw;

	public FWriter(String path) throws IOException {
		fw = new FileWriter(path);
		bw = new BufferedWriter(fw);
	}

	public void write(String data) throws IOException {
		bw.write(data);
		bw.newLine();	
	}

	public void writeAll(String[] data) throws IOException {
		for (String line: data) write(line);
	}
	
	public void close() throws IOException {
		bw.flush();
		bw.close();
		fw.close();
	}
}

package file;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileWriter {
	public void write() {
		try {
		byte bWrite[] = { 11, 21, 3, 40, 5 };
		OutputStream os = new FileOutputStream("test.txt");
		for (int x = 0; x < bWrite.length; x++) {
			os.write(bWrite[x]); // writes the bytes
		}
		os.close();
		} catch (IOException e) {
			System.out.print("Exception");
		}
	}
}

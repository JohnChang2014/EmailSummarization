package file;

import java.io.File;

public class Directory {

	public boolean exists(String dirname) {
		File d = new File(dirname);
		return d.exists();
	}

	public boolean isEmpty(String dirname) {
		File d = new File(dirname);
		if (d.exists()) {
		} else {
			System.out.println("No such directory or files");
			return false;
		}
		return false;
	}

	public void createDir(String dirname) {
		File d = new File(dirname);
		if (!d.exists()) d.mkdirs();
		else System.out.println("This directory exists already.");
	}

	public String[] getDirList(String dirname) {
		File d = new File(dirname);
		if (d.exists()) {
			if (d.isDirectory()) return d.list();
			else System.out.println(d + " is not a directory");
		} else {
			System.out.println("No such directory or files");
		}
		return null;
	}
}

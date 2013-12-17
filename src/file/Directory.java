package file;

import gate.util.Out;

import java.io.File;

public class Directory {

	public boolean exists(String dirname) {
		File d = new File(dirname);
		return d.exists();
	}

	public boolean isEmpty(String dirname) {
		File d = new File(dirname);
		if (d.exists()) {
			if (d.list().length == 0) return true;
			else return false;
		} else {
			Out.println("No such directory or files");
			return false;
		}
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
			else Out.println(d + " is not a directory");
		} else {
			Out.println("No such directory or files");
		}
		return null;
	}

	public void delete(String dirname) {
		File file = new File(dirname);
		if (file.isDirectory()) {

			// directory is empty, then delete it
			if (file.list().length == 0) {

				file.delete();
				System.out.println("Directory is deleted : " + file.getAbsolutePath());

			} else {

				// list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					// construct the file structure
					File fileDelete = new File(file, temp);

					// recursive delete
					this.delete(fileDelete.getAbsolutePath());
				}

				// check the directory again, if empty then delete it
				if (file.list().length == 0) {
					file.delete();
					System.out.println("Directory is deleted : " + file.getAbsolutePath());
				}
			}

		} else {
			// if file, then delete it
			file.delete();
			System.out.println("File is deleted : " + file.getAbsolutePath());
		}
	}
}

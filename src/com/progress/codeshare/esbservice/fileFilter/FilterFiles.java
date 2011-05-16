package com.progress.codeshare.esbservice.fileFilter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.sonicsw.xq.XQLog;
import com.sonicsw.xq.XQServiceException;

public class FilterFiles {

	// This is the XQLog (the container's logging mechanism).
	private XQLog m_xqLog = null;

	// This is the the log prefix that helps identify this service during
	// logging
	private static String m_logPrefix = "FileFilterService";

	public FilterFiles() {
	}

	public File[] getFilesAndFilterName(String directoryName,
			String fileNamePoint, String fileExtension) {

		File[] fileList;

		File dir = new File(directoryName);

		File[] filesFlag = dir.listFiles(getFilterFileName(fileNamePoint));

		File[] filesData = dir.listFiles(getFilterFile(fileExtension));

		fileList = new File[filesData.length];

		int iList = 0;

		if (filesFlag != null) {
			for (File data : filesData) {
				fileList[iList] = data;
				iList++;
			}
		}

		return fileList;

	}

	public File[] getFilesAndFilter(String directoryName,
			String fileExtensionPoint, String fileExtension,
			boolean moveExtensionPoint, String destinationExtensionPoint) {

		File[] fileList;

		File dir = new File(directoryName);

		File[] filesFlag = dir.listFiles(getFilterFile(fileExtensionPoint));

		File[] filesData = dir.listFiles(getFilterFile(fileExtension));

		if (filesFlag != null) {

			fileList = new File[filesFlag.length];

			int iList = 0;

			for (File file : filesFlag) {
				String fileNameFlag = file.getName().substring(0,
						file.getName().lastIndexOf('.'));
				for (File data : filesData) {
					String fileNameData = data.getName().substring(0,
							data.getName().lastIndexOf('.'));
					if (fileNameData.equalsIgnoreCase(fileNameFlag)) {
						fileList[iList] = data;
						iList++;

					}

				}

				if (moveExtensionPoint) {
					try {
						fileCopy(file.toString(), destinationExtensionPoint,
								file.getName().toString(), true, true, true);
					} catch (XQServiceException e) {
						m_xqLog.logDebug(m_logPrefix + e.getMessage());
					} catch (Exception e) {
						m_xqLog.logDebug(m_logPrefix + e.getMessage());
					}
				}
			}
			if (filesData == null) {
				fileList = null;
			}
		} else {
			fileList = null;
		}
		return fileList;
	}

	private FileFilter getFilterFile(final String fileExtension) {

		FileFilter filter = new FileFilter() {

			public boolean accept(File file) {
				return file.getName().toLowerCase().endsWith(fileExtension);
			}

		};

		return filter;
	}

	private FileFilter getFilterFileName(final String fileName) {

		FileFilter filter = new FileFilter() {

			public boolean accept(File file) {
				return file.getName().toLowerCase().equalsIgnoreCase(
						fileName.toLowerCase());
			}

		};

		return filter;
	}

	private void fileCopy(String sourceFile, String destinationDirectory,
			String destinationFile, boolean replaceExisting,
			boolean createDirectory, boolean removeSource)
			throws XQServiceException, Exception {
		File srcFileObject = new File(sourceFile);

		if (!destinationDirectory.endsWith("/")) {
			destinationDirectory = destinationDirectory + "/";
		}

		if (destinationFile.equals("") || destinationFile.equals("\"\"")) {
			destinationDirectory = destinationDirectory
					+ sourceFile.substring(sourceFile.lastIndexOf("/") + 1);
		} else {
			destinationFile = destinationDirectory + destinationFile;
		}

		File destFileObject = new File(destinationFile);

		// prevent overwrite of file
		if (destFileObject.exists() && !replaceExisting) {
			throw new XQServiceException(m_logPrefix
					+ "Runtime Error - Prevented Overwrite of file '"
					+ destinationDirectory + "'.");
		} else {
			BufferedInputStream inBuffer = new BufferedInputStream(
					new FileInputStream(srcFileObject));

			File destDirObject = new File(destinationDirectory);

			if (!destDirObject.exists()) {
				if (createDirectory) {
					(new File(destinationDirectory)).mkdirs();
				} else {
					throw new XQServiceException(
							m_logPrefix
									+ " Runtime Error - "
									+ "Destination Directory does not exist and the Create Directory "
									+ "flag for service set to FALSE.");
				}
			}

			BufferedOutputStream outBuffer = new BufferedOutputStream(
					new FileOutputStream(destFileObject));

			byte[] buf_copy = new byte[1024];

			int readLength;

			if (!srcFileObject.equals(destFileObject)) {
				while ((readLength = inBuffer.read(buf_copy, 0, 1024)) != -1)
					outBuffer.write(buf_copy, 0, readLength);
			}

			if (inBuffer != null) {
				inBuffer.close();
			}

			if (outBuffer != null) {
				outBuffer.close();
			}

			if (removeSource && !srcFileObject.equals(destFileObject)) {
				srcFileObject.delete();
			}
		}
	}
}
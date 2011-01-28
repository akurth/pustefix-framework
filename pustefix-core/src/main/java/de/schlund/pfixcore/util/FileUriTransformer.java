package de.schlund.pfixcore.util;

import java.io.File;

public class FileUriTransformer {

	/**
	 * Build URI without the windows root (e.g get rid of C:),
	 * because javax.xml.transform.Transformer can't handle it
	 * and add the "file://" prefix.
	 * 
	 * e.g.: 
	 * C:/workspace/pustefix-core/test/resources/file.xml and
	 * /workspace/pustefix-core/test/resources/file.xml will return
	 * the same result: file:///workspace/pustefix-core/test/resources/file.xml
	 *
	 * @param file
	 * @return
	 */
	public static String getFileUriWithoutWindowsDriveSpecifier(File file) {
		String absolutePath = file.getAbsolutePath();
		if (absolutePath.charAt(1) == ':') {
			absolutePath = absolutePath.substring(2).replace(
					File.separatorChar, '/');
		}
		return "file://" + absolutePath;
	}
}

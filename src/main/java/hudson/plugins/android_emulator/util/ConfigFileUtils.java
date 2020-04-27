package hudson.plugins.android_emulator.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.plugins.android_emulator.Messages;

public class ConfigFileUtils {

    private static String CONFIG_FILE_EXT_INI = "ini";
    private static String CONFIG_FILE_EXT_PROPS = "properties";

    /**
     * Parses the contents of a .properties or .ini file into a map.
     *
     * @param configFile The file to read.
     * @return The key-value pairs contained in the file, ignoring any comments or blank lines.
     * @throws IOException If the file could not be read or an unsupported file extension is used.
     */
    public static Map<String, String> parseConfigFile(File configFile) throws IOException {
        final String fileExtension = getLowerCaseFileExtension(configFile);

        if (fileExtension.equals(CONFIG_FILE_EXT_PROPS)) {
            return parsePropertiesFile(configFile);
        } else if (fileExtension.equals(CONFIG_FILE_EXT_INI)) {
            return parseSimpleINIFormatFile(configFile);
        } else {
            throw new IOException(Messages.CONFIG_FILE_UNSUPPORTED_EXTENSION(fileExtension));
        }
    }

    /**
     * Parses the contents of a properties file into a map.
     *
     * @param configFile The file to read.
     * @return The key-value pairs contained in the file, ignoring any comments or blank lines.
     * @throws IOException If the file could not be read.
     */
    private static Map<String, String> parsePropertiesFile(File configFile) throws IOException {
        final Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        }

        final Map<String, String> values = new HashMap<>();
        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            values.put((String) entry.getKey(), (String) entry.getValue());
        }

        return values;
    }

    /**
     * Parses the contents of a .ini file into a map.
     *
     * @param configFile The file to read.
     * @return The key-value pairs contained in the file, ignoring any comments or blank lines.
     * @throws IOException If the file could not be read.
     */
    private static Map<String, String> parseSimpleINIFormatFile(File configFile) throws IOException {
        final Map<String, String> values = new HashMap<>();

        for (String line : FileUtils.readLines(configFile)) {
            // remove trailing space
            line = line.replaceFirst("^\\s++", "");

            // ignore comments and empty lines
            if (line.startsWith("#") || line.startsWith(";") || line.isEmpty()) {
                continue;
            }

            final String[] keyVal = line.split("=", 2);

            final String key = keyVal[0].trim(); //split is used with limit, there will be always one element
            final String val = (keyVal.length > 1) ? keyVal[1].replaceFirst("^\\s++", "") : "";

            if (!key.isEmpty()) {
                values.put(key, val);
            }
        }

        return values;
    }

    /**
     * Write the configuration values dependent of the file extension.
     * Currently supported are .properties and .ini files.
     *
     * @param configFile the destination config file.
     * @param values configuration key-value-pairs to write.
     * @throws IOException If the file could not be written or an unsupported file extension is used.
     */
    public static void writeConfigFile(final File configFile, final Map<String,String> values) throws IOException {
        final String fileExtension = getLowerCaseFileExtension(configFile);

        if (fileExtension.equals(CONFIG_FILE_EXT_PROPS)) {
            writeConfigFilePropertiesFormat(configFile, values);
        } else if (fileExtension.equals(CONFIG_FILE_EXT_INI)) {
            writeConfigFileSimpleINIFormat(configFile, values);
        } else {
            throw new IOException(Messages.CONFIG_FILE_UNSUPPORTED_EXTENSION(fileExtension));
        }
    }

    /**
     * Write the configuration values as Java-{@code Properties}
     *
     * @param configFile the destination config file.
     * @param values configuration key-value-pairs to write.
     * @throws IOException If the file could not be written.
     */
    private static void writeConfigFilePropertiesFormat(final File configFile, final Map<String,String> values) throws IOException {
        final Properties props = new Properties();
        props.putAll(values);

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, null);
        }
    }

    /**
     * Write the configuration values in a simple INI Format ('key=value', single line format)
     *
     * @param configFile the destination config file
     * @param values configuration key-value-pairs to write
     * @throws IOException If the file could not be written.
     */
    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    private static void writeConfigFileSimpleINIFormat(final File configFile, final Map<String,String> values) throws IOException {
        PrintWriter out = new PrintWriter(configFile);
        for (final Entry<String, String> entry : values.entrySet()) {
            out.print(entry.getKey() + "=" + entry.getValue() + "\r\n");
        }
        out.flush();
        out.close();
    }

    /**
     * Returns the lower case file extension (part after the last dot) of the given file.
     *
     * @param file to extract extension from
     * @return the lower case extension or "" if no extension could be extracted
     */
    private static String getLowerCaseFileExtension(final File file) {
        return FilenameUtils.getExtension(file.getAbsolutePath()).toLowerCase();
    }
}

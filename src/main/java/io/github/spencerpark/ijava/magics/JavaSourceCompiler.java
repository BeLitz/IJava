package io.github.spencerpark.ijava.magics;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Class used to compile Java files into class files.
 */
public class JavaSourceCompiler {


    /**
     * No construction please.
     */
    private JavaSourceCompiler() {
    }

    private static String getClasspathAsString(String fileName){
        if (fileName == null){
            return null;
        }
        StringBuilder sb = new StringBuilder();
        File file = new File(fileName);
        String absolutePath = file.getAbsolutePath() + File.separator;
        String[] filenames = new File(fileName).list();
        for(int i = 0; i < filenames.length; i++) {
            if(i > 0) { 
                // separate with ':' or ';' on Win
                sb.append(File.pathSeparatorChar); 
            }       
            sb.append(absolutePath + filenames[i]);
        }
        return sb.toString();

    }
    
    private static String getAllClasspathsAsString(List<String> fileNames){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < fileNames.size(); i++) {
            if(i > 0) { 
                // separate with ':' or ';' on Win
                sb.append(File.pathSeparatorChar); 
            }       
            sb.append(getClasspathAsString(fileNames.get(i)));
        }
        return sb.toString();
    }
    
    
    /**
     * Compile a directory of Java classes into the corresponding class files.
     * 
     * @param javaDir
     *            the directory with the Java source files
     * @param classesDir
     *            the directory to write the class files into
     * @param jarFile
     *            if not <code>null</code>, package the compiled classes into a JAR file
     */
    public static void compile(File javaDir, File classesDir, File jarFile, List<String> fileNames) {
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager sjfm = jc.getStandardFileManager(null, null, Charset.forName("UTF-8"));
         if (!javaDir.isDirectory()) {
            throw new IllegalArgumentException(javaDir.getAbsolutePath() + " is not a directory");
        }
        Collection<File> files = FileUtils.listFiles(javaDir, new String[]{ "java" }, true);
        System.out.println("Compiling " + files.size() + " Java file(s)");
        Iterable<? extends JavaFileObject> fileObjects = sjfm.getJavaFileObjectsFromFiles(files);
        
        String classpathAsString = getAllClasspathsAsString(fileNames);
        System.out.println("Including the following libraries in classpath: " + classpathAsString);
        String[] options = null;
        if(classpathAsString == null){
            options = new String[]{"-d", classesDir.getAbsolutePath()};
        } else {
            options = new String[]{"-d", classesDir.getAbsolutePath(),
                    "-classpath", classpathAsString};
        }

        if (!jc.getTask(null, sjfm, null, Arrays.asList(options), null, fileObjects).call()) {
            throw new RuntimeException("There was some error compiling the Java code");
        }
        try {
            sjfm.close();
        } catch (IOException e) {
            throw new RuntimeException("Error compiling Java code: " + e.getMessage());
        }
        if (jarFile != null) {
            createJarFile(jarFile, classesDir);
        } else {
            throw new RuntimeException("Jar file is null.");
        }
    }

    /**
     * Package a directory into a JAR file.
     * 
     * @param jarFile
     *            the JAR file to create
     * @param directoryToJar
     *            the directory with the JAR contents
     */
    public static void createJarFile(File jarFile, File directoryToJar) {
        try {
            System.out.println("Creating JAR file " + jarFile);
            Collection<File> classFiles = FileUtils.listFiles(directoryToJar, null, true);
            if (classFiles.isEmpty()) {
                throw new IllegalArgumentException(
                    "Failed to create JAR file. No files were found in directory "
                        + directoryToJar.getAbsolutePath());
            }

            JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(jarFile), new Manifest());

            for (File fileToJar : classFiles) {

                if (fileToJar.isDirectory()) {
                    // ignore
                    continue;
                }

                String strJarFile =
                    correctPath(fileToJar.getAbsolutePath().substring(
                        directoryToJar.getAbsolutePath().length() + 1));

                System.out.println("Adding file " + strJarFile + " to JAR file " + jarFile);
                JarEntry jarAdd = new JarEntry(strJarFile);

                jarOut.putNextEntry(jarAdd);
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(fileToJar);
                    IOUtils.copy(inputStream, jarOut);
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            }

            jarOut.close();
        } catch (IOException e) {
            throw new RuntimeException("Error creating JAR file " + e.getMessage(), e);
        }
    }

    /**
     * @param inPath
     *            path to modify.
     * @return the slash corrected path.
     */
    private static String correctPath(String inPath) {
        return inPath.replaceAll("\\\\", "/");
    }

}
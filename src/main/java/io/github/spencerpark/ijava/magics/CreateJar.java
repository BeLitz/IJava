package io.github.spencerpark.ijava.magics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import io.github.spencerpark.jupyter.kernel.magic.registry.LineMagic;

public class CreateJar {

    private static final String JAR = ".jar";
    private static final String OUTPUT = "output";
    private static final String SOURCE = "-source";
    private static final String CLASSPATH = "-classpath";

    public CreateJar() {
    }

    @LineMagic
    public void createJar(List<String> args) {

        int pos = 0;
        List<String> classpathList = new ArrayList<>();
        List<String> javaFileList = new ArrayList<>();
        String javaFileName = null;
        // get all locations for classpath and source code
        try {
            if (CLASSPATH.equals(args.get(pos))) {
                pos++;
                while (!SOURCE.equals(args.get(pos))) {
                    classpathList.add(args.get(pos));
                    pos++;

                }
            }
            if (SOURCE.equals(args.get(pos))) {
                pos++;
                javaFileName = args.get(pos);
                while (pos < args.size()) {
                    javaFileList.add(args.get(pos));
                    pos++;
                    }
            }

            File tmpDir = new File(OUTPUT);
            File javaFile = new File(javaFileName);
            File srcDir = null;
            if (javaFile.isDirectory()) {
                srcDir = javaFile;
            } else {
                srcDir = javaFile.getParentFile();
            }
            String javaBaseFileName = javaFile.getName();
            if (tmpDir.exists()) {
                FileUtils.cleanDirectory(tmpDir);
            } else {
                tmpDir.mkdirs();
            }

            File jarFile = new File(tmpDir, FilenameUtils.removeExtension(javaBaseFileName) + JAR);
            JavaSourceCompiler.compile(srcDir, tmpDir, jarFile, classpathList);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}

package com.ceabie.dexknife;

import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.RelativePath;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Mock the file tree element of Filter.
 *
 * @author ceabie
 */
public class ClassFileTreeElement implements FileTreeElement {
    private RelativePath mRelativePath;
    private File mFile;

    public void setClassPath(String name) {
        mFile = new File(name);
        mRelativePath = RelativePath.parse(!isDirectory(), name);
    }

    @Override
    public File getFile() {
        return mFile;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public InputStream open() {
//        try {
//            return mZipFile.getInputStream(mZipEntry);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return null;
    }

    @Override
    public void copyTo(OutputStream outputStream) {
    }

    @Override
    public boolean copyTo(File file) {
        return true;
    }

    @Override
    public String getName() {
        return mFile.getName();
    }

    @Override
    public String getPath() {
        return mFile.getPath();
    }

    @Override
    public RelativePath getRelativePath() {
        return mRelativePath;
    }

    @Override
    public int getMode() {
        return isDirectory()
                ? FileSystem.DEFAULT_DIR_MODE
                : FileSystem.DEFAULT_FILE_MODE;
    }
}

package playground.sebhoerl.mexec.ssh.utils;

import com.jcraft.jsch.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class SSHUtils {
    final private ChannelSftp sftp;
    final private Session session;

    public SSHUtils(Session session) {
        try {
            this.sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
        } catch (JSchException e) {
            throw new RuntimeException("Error while starting SFTP");
        }

        this.session = session;
    }

    public boolean mkdirs(SSHFile path) {
        String[] parts = path.toString().split("/");
        String partial = "";

        for (int i = 0; i < parts.length - 1; i++) {
            partial += parts[i] + "/";

            try {
                sftp.cd(partial);
                sftp.mkdir(parts[i + 1]);
            } catch (SftpException e) {}
        }

        return exists(path);
    }

    public boolean exists(SSHFile path) {
        try {
            sftp.lstat(path.toString());
            return true;
        } catch (SftpException e) {
            return false;
        }
    }

    public SSHFile getAbsoluteFile(SSHFile file) {
        if (file.toString().startsWith("/")) {
            return file;
        }

        try {
            return new SSHFile(sftp.realpath(file.toString()));
        } catch (SftpException e) {
            throw new RuntimeException("Cannot find absolute path for " + file);
        }
    }

    public boolean isDirectory(SSHFile path) {
        try {
            return sftp.lstat(path.toString()).isDir();
        } catch (SftpException e) {
            return false;
        }
    }

    public InputStream read(SSHFile path) throws IOException {
        try {
            return sftp.get(path.toString());
        } catch (SftpException e) {
            throw new IOException("File " + path + " could not be read.");
        }
    }

    public void write(SSHFile path, InputStream stream) throws IOException {
        deleteQuietly(path);

        try {
            sftp.put(stream, path.toString());
        } catch (SftpException e) {
            throw new IOException("Error while writing to " + path);
        }
    }

    public void copyFile(File localPath, SSHFile remotePath) throws IOException {
        try {
            sftp.put(localPath.toString(), remotePath.toString());
        } catch (SftpException e) {
            throw new IOException("Error while put'ing file " + localPath + " to " + remotePath);
        }
    }

    public void copyDirectory(File localPath, SSHFile remotePath) throws IOException {
        String absolute = localPath.getAbsolutePath();

        try {
            Collection<File> hits = FileUtils.listFilesAndDirs(localPath, new RegexFileFilter("^(.*?)"), DirectoryFileFilter.DIRECTORY);

            List<String> directories = new LinkedList<>();
            List<String> files = new LinkedList<>();

            for (File file : hits) {
                if (file.getAbsolutePath().contains(".git")) {
                    continue;
                }

                if (file.isDirectory()) {
                    directories.add(file.getAbsolutePath());
                } else if (file.isFile()) {
                    files.add(file.getAbsolutePath());
                }
            }

            for (String directory : directories) {
                String target = directory.replace(absolute, "");

                if (target.length() == 0) {
                    mkdirs(remotePath);
                } else {
                    try {
                        sftp.mkdir(new SSHFile(remotePath, target.substring(1)).toString());
                    } catch (SftpException e) {}
                }
            }

            for (String file : files) {
                String target = file.replace(absolute, "").substring(1);
                sftp.put(file, new SSHFile(remotePath, target).toString());
            }
        } catch (SftpException e) {
            throw new IOException("Error while put'ing directory " + localPath + " to " + remotePath);
        }
    }

    public void deleteQuietly(SSHFile path) {
        try {
            execute("rm -r " + path.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void copyRemote(SSHFile source, SSHFile target) throws IOException {
        RunResult result = execute(String.format("cp %s %s", source.toString(), target.toString()));

        if (result.exitStatus != 0) {
            throw new IOException("Error while copying.");
        }
    }

    final int BUFFER_SIZE = 1024;

    public class RunResult {
        final public int exitStatus;
        final public byte[] output;
        final public byte[] error;

        public RunResult(int exitStatus, byte[] output, byte[] error) {
            this.exitStatus = exitStatus;
            this.output = output;
            this.error = error;
        }
    }

    public RunResult execute(String command) throws IOException {
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;

            InputStream errorStream = channel.getErrStream();
            InputStream outputStream = channel.getInputStream();

            channel.connect();

            while (!channel.isClosed() || errorStream.available() > 0 || outputStream.available() > 0) {
                while (errorStream.available() > 0) {
                    length = errorStream.read(buffer);
                    errorOutput.write(buffer, 0, length);
                }

                while (outputStream.available() > 0) {
                    length = outputStream.read(buffer);
                    output.write(buffer, 0, length);
                }
            }

            channel.disconnect();

            return new RunResult(channel.getExitStatus(), output.toByteArray(), errorOutput.toByteArray());
        } catch (JSchException e) {
            throw new RuntimeException("SSH exception");
        }
    }
}

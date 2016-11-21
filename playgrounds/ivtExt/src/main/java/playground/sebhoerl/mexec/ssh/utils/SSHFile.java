package playground.sebhoerl.mexec.ssh.utils;

public class SSHFile {
    final private String path;

    public SSHFile(String path) {
        this.path = path;
    }

    public SSHFile(SSHFile parent, String path) {
        String temporary = parent.toString();

        while (temporary.endsWith("/")) {
            temporary = temporary.substring(0, temporary.length() - 1);
        }

        this.path = temporary + "/" + path;
    }

    @Override
    public String toString() {
        return this.path;
    }
}

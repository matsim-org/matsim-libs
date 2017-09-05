package playground.sebhoerl.mexec.examples;

import java.io.File;
import java.io.IOException;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import playground.sebhoerl.mexec.ssh.utils.SSHFile;
import playground.sebhoerl.mexec.ssh.utils.SSHUtils;

public class SftpTest {
    static public void main(String[] args) throws JSchException, SftpException, IOException {
        JSch jsch = new JSch();
        jsch.addIdentity("~/.ssh/eth");

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");


        Session session = jsch.getSession("shoerl", "ifalik.ivt.ethz.ch", 22);
        session.setConfig(config);
        session.connect();

        SSHUtils ssh = new SSHUtils(session);
        ssh.copyDirectory(new File("/home/sebastian/Downloads/matsim/examples/equil"), new SSHFile("/nas/shoerl/equi"));

        session.disconnect();
    }
}

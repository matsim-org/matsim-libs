package playground.sebhoerl.mexec.examples;

import com.jcraft.jsch.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import playground.sebhoerl.mexec.ssh.utils.SSHFile;
import playground.sebhoerl.mexec.ssh.utils.SSHUtils;
import playground.sebhoerl.remote_exec.euler.EulerConfiguration;
import playground.sebhoerl.remote_exec.euler.EulerEnvironment;
import playground.sebhoerl.remote_exec.euler.EulerInterface;
import playground.sebhoerl.remote_exec.euler.EulerSimulation;

import java.io.*;

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

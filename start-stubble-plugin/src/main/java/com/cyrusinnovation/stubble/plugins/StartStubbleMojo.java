package com.cyrusinnovation.stubble.plugins;

import com.cyrusinnovation.stubble.server.StubServer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Starts the Stubble StubServer on the specified port.
 *
 * @goal start
 * @phase pre-integration-test
 */
public class StartStubbleMojo extends AbstractMojo {
    /**
     * Port on which to start the server.
     *
     * @parameter default-value="8082"
     * @required
     */
    private int port;

    public void execute() throws MojoExecutionException {
        StubServer server = new StubServer(port);
        server.start();
    }
}

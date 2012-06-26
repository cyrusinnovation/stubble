package com.cyrusinnovation.stubble.plugins;

import com.cyrusinnovation.stubble.server.StubServer;
import com.cyrusinnovation.stubble.server.StubbleClient;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Stops the Stubble StubServer running on the specified port.
 *
 * @goal stop
 * @phase post-integration-test
 */
public class StopStubbleMojo extends AbstractMojo {
    /**
     * Port on which the server is running.
     *
     * @parameter default-value="8082"
     * @required
     */
    private int port;

    public void execute() throws MojoExecutionException {
        StubbleClient client = new StubbleClient(port);
        client.stopServer();
    }
}

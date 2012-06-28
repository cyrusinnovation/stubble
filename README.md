stubble
======

Stub server for testing HTTP interactions

## Why Stubble?

Stubble was designed to support testing of HTTP interactions between applications.  Typically, this would be a web application consuming web services from a backend server, but it could be used for any sort of application that needs to talk to another via HTTP.  Stubble allows you to setup interactions as you would with a [mock or stub](http://martinfowler.com/articles/mocksArentStubs.html) [library](http://code.google.com/p/mockito/) for unit testing.  Rather than specifying method calls and return values, Stubble allows you to specify request conditions to match that, when matched, will cause the ```StubServer``` to return a specified response.

Testing is supported either within the same process, controlling the StubServer programmatically, or remotely,
such as from an integration test running in a build, controlling the StubServer with a remote client over HTTP.

## Usage

### Starting the Server

Start the server by creating an instance with the desired port on which to serve interactions, then calling the ```start``` method:

```scala
val server = new StubServer(8082)
server.start()
```

### Direct Control

Control of the ```StubServer``` is done via the ```StubServerControl``` interface/trait:

```scala
trait StubServerControl {
  def addInteraction(interaction: Interaction)
  def popInteractions()
  def pushInteractions()
}
```

The trait is mixed into both the ```StubServer``` class and the ```StubbleClient``` class, the latter being the mechanism for controlling interactions from a remote process.  Interactions are setup by passing in an instance of the ```Interaction``` class:

```scala
server.addInteraction(Interaction(List(PathCondition("/")), Response(HttpResponseStatus.OK, Some(body))))
```

See also the ```StubbleClientTest``` and ```StubServerTest``` for more examples of setting up interactions.


### Remote Control

Adding interactions remotely is much the same as on the server directly, but you must first create a client configured with the same port as the server:

```scala
val client = new StubbleClient(8082)
```

### The Interaction Stack

Often during integration testing, you may want to start Stubble, start your application, then run a bunch of tests against the application.  You probably want each test to be as self-contained as possible, so Stubble allows you to setup an interaction "stack frame" for each test.  By calling ```pushInteractions()``` before each test and ```popInteractions()``` after each test, you can isolate tests from each other, throwing away any state you've setup after each test.  A complete typical test might look like this (assuming the StubServer is started by the build, and here the ExampleApplication is small enough to be started for each test):

```scala
import com.cyrusinnovation.stubble.server._
import org.junit._
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.junit.Assert._

class ExampleTest {
  var client: StubbleClient = new StubbleClient(8082)
  val app = new ExampleApplication

  @Before
  def setUp() {
    client.pushInteractions()
  }

  @After
  def tearDown() {
    client.popInteractions()
  }

  @Test
  def addsInteractionsToServer() {
    val interactions = List(Interaction(List(CookieCondition("type" -> "chocolate chip")), Response(HttpResponseStatus.OK, Some("gimme cookie!"))),
                            Interaction(List(PathCondition("/")), Response(HttpResponseStatus.OK, Some("Hello!"))))
    interactions.foreach(client.addInteraction(_))

    val appResponse = app.somethingThatNeedsABackendServer
    assertEquals("Something wonderful happened", appResponse)
  }
}
```

The interaction stack is sort of analogous to what JUnit does by initializing your test class before each test and throwing it away after, except that often you can't throw away your back end each time, because your application may be making requests of it outside your test flow.  Stubble supports setting up the base, background interactions that might be happening outside the test flow by adding them to the first stack frame, which it will never pop off the stack.  In other words, if you have interactions that need to be around for the duration of your server's lifetime, set those up before pushing interactions before the first time, and they'll stay around until the ```StubServer``` is shut down.

## Maven Integration

Stubble is designed to fit easily into your Maven build.  You can include the stub server and client code by adding a dependency on the ```stubble-core``` module:

```xml
    <dependency>
      <groupId>com.cyrusinnovation</groupId>
      <artifactId>stubble-core</artifactId>
      <version>0.0.1</version>
    </dependency>
```

A common way of running integration tests with maven is to create a submodule to contain integration tests, that depends on your main project module(s) and starts/stops your server instance.  Typically, you may want the stub server to start before your server starts and stop after your server shuts down so that, for example, any interactions initiated at startup will succeed.
In order to start your server for integration testing, you might bind [Cargo](http://cargo.codehaus.org/Maven2+plugin)'s ```start``` goal to the ```pre-integration-test``` phase and its ```stop``` goal to the ```post-integration-test``` phase, using the Surefire plugin to run tests in the ```integration-test``` phase.  When binding a second plugin to the same phase (```pre-integration-test```, for example), however, Maven executes plugins in declaration order in the pom.  In order to start before Cargo and stop after Cargo, then, Stubble provides two plugins, one each for start and stop actions.  Therefore, use the ```start-stubble-plugin``` before Cargo in the ```pre-integration-test``` phase and the ```stop-stubble-plugin``` after Cargo in the ```post-integration-test``` phase.
If you don't understand any of this and haven't tried to sequence actions within a single phase in Maven, be thankful and just copy and paste the below example into your integration-test pom, modifying Cargo for your own application:

```xml
  <properties>
    <serverPort>8081</serverPort>
    <stubblePort>8082</stubblePort>
  </properties>

  <build>
    <plugins>
      <plugin>
        <groupId>com.cyrusinnovation</groupId>
        <artifactId>start-stubble-plugin</artifactId>
        <version>0.0.1</version>
        <configuration>
          <port>${stubblePort}</port>
        </configuration>
        <executions>
          <execution>
            <phase>pre-integration-test</phase>
            <goals>
              <goal>start</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <skip>true</skip>
          <systemProperties>
            <property>
              <name>serverPort</name>
              <value>${serverPort}</value>
            </property>
            <property>
              <name>stubblePort</name>
              <value>${stubblePort}</value>
            </property>
          </systemProperties>
        </configuration>
        <executions>
          <execution>
            <id>integration-tests</id>
            <phase>integration-test</phase>
            <goals>
              <goal>test</goal>
            </goals>
            <configuration>
              <skip>false</skip>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.codehaus.cargo</groupId>
        <artifactId>cargo-maven2-plugin</artifactId>
        <version>1.2.0</version>
        <executions>
          <execution>
            <id>start-container</id>
            <phase>pre-integration-test</phase>
            <goals>
              <goal>start</goal>
            </goals>
          </execution>
          <execution>
            <id>stop-container</id>
            <phase>post-integration-test</phase>
            <goals>
              <goal>stop</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <wait>false</wait>
          <container>
            <containerId>tomcat7x</containerId>
            <zipUrlInstaller>
              <url>
                http://mirror.cc.columbia.edu/pub/software/apache/tomcat/tomcat-7/v7.0.27/bin/apache-tomcat-7.0.27.zip
              </url>
              <downloadDir>${java.io.tmpdir}/cargoinstalls.${user.name}</downloadDir>
            </zipUrlInstaller>
            <systemProperties>
              <stubblePort>${stubblePort}</stubblePort>
            </systemProperties>
          </container>
          <configuration>
            <home>${project.build.directory}/tomcat7x</home>
            <properties>
              <cargo.servlet.port>${serverPort}</cargo.servlet.port>
              <cargo.tomcat.ajp.port>8010</cargo.tomcat.ajp.port>
            </properties>
            <deployables>
              <deployable>
                <groupId>com.cyrusinnovation</groupId>
                <artifactId>test-server</artifactId>
                <type>war</type>
                <properties>
                  <context>/</context>
                </properties>
              </deployable>
            </deployables>
          </configuration>
        </configuration>
      </plugin>
      <plugin>
        <groupId>com.cyrusinnovation</groupId>
        <artifactId>stop-stubble-plugin</artifactId>
        <version>0.0.1</version>
        <configuration>
          <port>${stubblePort}</port>
        </configuration>
        <executions>
          <execution>
            <phase>post-integration-test</phase>
            <goals>
              <goal>stop</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```
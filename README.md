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
import org.junit.{After, Before, Test}
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.junit.Assert._
import scala.Some

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
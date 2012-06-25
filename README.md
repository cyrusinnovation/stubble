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
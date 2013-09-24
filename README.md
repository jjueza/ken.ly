ken.ly
======

a very simple URL shortening service written in Scala

### What is it?

ken.ly consists of 3 services to help with your URL-shortening needs.

| Service  | Endpoint                         | Sample Output                                        |
|:---------|:---------------------------------|:-----------------------------------------------------|
| Hasher   | /action/hash?url=http://your.url | {"originalURL":"http://your.url", "hash":"yOuRhAsH"} |
| Stats    | /action/stats?hash=yOuRhAsH      | {"hash":"yOuRhAsH", "clickCount":"0"}                |
| Redirect | /yOuRhAsH                        | .. redirects to http://your.url                      |

Check out a working version [here](http://powerful-brook-3153.herokuapp.com/actions/hash?url=https://github.com/kbritton/ken.ly).  The ken.ly domain name has not
yet been registered, so please use your imagination.

### Required tools

1. [SBT](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html) build utility
2. [Heroku Toolbelt](https://toolbelt.heroku.com/)
3. [JDK 1.6](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
4. [MongoDB](http://docs.mongodb.org/manual/installation/) \(production use\)

### Usage

Clone the ken.ly repository
```
git clone git@github.com:kbritton/ken.ly.git
```

Run the tests
```
sbt clean compile test
```

Start it locally (default location: [http://localhost:5000](http://localhost:5000))
```
sbt clean compile stage
foreman start
```

Generate some hashes!
```
curl http://localhost:5000/actions/hash?url=http://www.google.com
```

NOTE If you experience an `java.lang.OutOfMemoryError: PermGen space` error while trying to run SBT, set the following
environment variable
```
export SBT_OPTS=-XX:MaxPermSize=512m
```

### MongoDB

The default configuration of ken.ly uses an in-memory data store that is destroyed when the JVM exits.  If you'd
like durable data and the ability to horizontally scale, install [MongoDB](http://docs.mongodb.org/manual/installation/).
On startup ken.ly looks for an environment variable called `MONGOLAB_URI` and tries to connect to the URI.
```
export MONGOLAB_URI="mongodb://localhost:27017/links"
```

On startup, the console log will indicate the type of data store in use
```
11:10:50 web.1  | 0 [on-spray-can-akka.actor.default-dispatcher-3] INFO com.britton.MongoDataStore - Using mongo instance: mongodb://localhost:27017/links
```

If you have the MongoLab add-on configured for your Heroku instance, the `MONGOLAB_URI` environment variable
will automatically be set for your application.

### Heroku

Heroku is a cloud platform as a service (PaaS) that supports Scala.  In order to deploy ken.ly to heroku, sign up [here](https://api.heroku.com/signup/devcenter) 
then follow these steps.
```
heroku login
sbt clean compile stage
heroku create
git push heroku master
heroku open
```

### Known Issues

InMemoryDataStore can only support single-threaded, single-actor usage such as simple unit testing.  
For all other usage scenarios, MongoDataStore is required.

### Credits

Thanks to [peet](https://github.com/peet) for the sweet [hashids](https://github.com/peet/hashids.java) implementation.

### Contact

Follow me [@k_britton](http://twitter.com/k_britton)

### License

MIT License. See the `LICENSE` file.

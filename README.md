ken.ly
======

a very simple link shortening service written in Scala

### What is it?

ken.ly consists of 3 services to help with your link-shortening needs.

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

### MongoDB

The default configuration of ken.ly uses an in-memory data store that is destroyed when the JVM exits.  If you'd
like the data to stick around longer, you can install [MongoDB](http://docs.mongodb.org/manual/installation/) and use it as the principal data store.  On startup
ken.ly looks for an environment variable called `MONGOLAB_URI` and tries to connect to the URI.
```
export MONGOLAB_URI="mongodb://localhost:27017/links"
```

If you have the MongoLab add-on configured for your Heroku instance, the `MONGOLAB_URI` environment variable
will automatically be set for your application.

### Credits

Thanks to [peet](https://github.com/peet) for the sweet [hashids](https://github.com/peet/hashids.java) implementation.

### Contact

Follow me [@k_britton](http://twitter.com/k_britton)

### License

MIT License. See the `LICENSE` file.

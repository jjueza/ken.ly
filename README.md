ken.ly
======

a very simple link shortening service written in Scala

## What is it?

ken.ly consists of 3 services to help with your link-shortening needs.

| Service  | Endpoint                         | Sample Output                                                          |
|:---------|:---------------------------------|:-----------------------------------------------------------------------|
| Hasher   | /action/hash?url=http://your.url | {"originalURL":"https://github.com/kbritton/ken.ly","hash":"x5EBR5pK"} |
| Stats    | /action/stats?hash=yOuRhAsH      | {"hash":"x5EBR5pK","clickCount":"0"}                                   |
| Redirect | /yOuRhAsH                        | .. redirects to https://github.com/kbritton/ken.ly                                        |

Check out a working version [here](http://powerful-brook-3153.herokuapp.com/actions/hash?url=https://github.com/kbritton/ken.ly).  The ken.ly domain name has not
yet been registered, so please use your imagination.

## Requirements

1. [SBT](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html) command line utility
2. [MongoDB](http://docs.mongodb.org/manual/installation/)
3. [Heroku Toolbelt](https://toolbelt.heroku.com/)

## Optional

1. A [Heroku account](https://api.heroku.com/signup/devcenter)

## Usage

Clone the ken.ly repository
```
git clone git@github.com:kbritton/ken.ly.git
```

Run the tests
```
sbt clean compile test
```

Run it locally (check out [http://localhost:5000](http://localhost:5000))
```
sbt clean compile stage
foreman start
```

Deploy to heroku

In order to deploy to heroku, you will need to add the MongoLab Add-on to your Application.  ken.ly will automatically
connect to the MongoLab instance via the `MONGOLAB_URI` environment variable.

```
heroku login
sbt clean compile stage
heroku create
git push heroku master
heroku open
```

## Credits

Thanks to [peet](https://github.com/peet) for the sweet [hashids](https://github.com/peet/hashids.java) implementation.

## Contact

Follow me [@k_britton](http://twitter.com/k_britton)

## License

MIT License. See the `LICENSE` file.
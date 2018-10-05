# Colliding Events

![now](https://media0.giphy.com/media/tn8zWeNYA73G0/giphy.gif)

## Assumptions

Since the problem definition has been left very open, let's lay out our assumptions:

 1. It's not clear how this should be run, so let's use a webserver: this server can accept requests to read from local, or accept a provided sequence.
 2. Instead of making up an arbitrary data-model for a calendar-sequence, let's accept json for an http request and edn for a local calendar.
 3. 'Events' are pretty vague, so let's define a spec for an accepted event - data is code in clojure.

To start a web server for the application, run:

    lein ring server-headless

## License

Copyright © 2018 FIXME

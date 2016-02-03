# Hipmunk Coding Challenge

Here is my solution to the Hipmunk Coding Challenge.
There is a running version of this code deployed to AWS [here](http://54.208.248.245:8000/). It may be a bit slow since it is a micro instance.

## Requirement

- java 8
- leiningen 2.0
- python2.7

## Installation

Clone this repo. Make sure you have installed the requirements.
Start the python scraper API (Running on port 9000)

```
cd hipproblems
python setup.py develop
python -m searchrunner.scraperapi
```

Start the Clojure API (Running on port 8000)

```
lein repl
(start-server)
```

## Frontend

[Click here to see a demo](http://54.208.248.245:8000/)
The small frontend is built with ClojureScript (Clojure syntax that compiles down to javascript), Bootstrap, and React.
Click on the Search button and that should make an API call to the backend and display it in a table.

## Tests

Run the tests
> python -m searchrunner.scraperapi_test

You should get a message saying:

```
Took 2.30 seconds. Looks good!
```

You can also look at the tests written in the test/template/util_test.cljs file.s

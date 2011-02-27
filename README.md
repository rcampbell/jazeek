# jazeek

# TODO:

- loginza: callbacks can throw exceptions. Right now nothing happens
and there is no stacktrace, just an empty response.
- add/remove identities from account
- merge accounts (logged in with google, logged in with twitter, now
have two accounts for each identity)
- jquery + jquery-ui 
- remember-me (login seems stable, time to skip it)
- move sessions to DB



FIXME: write description

## Usage

    lein deps
    lein swank

    emacs
    M-x slime-connect
    
    C-x C-f src/jazeek/init.clj 
    C-c C-k   (to recreate database schema)
    C-x C-f src/jazeek/core.clj
    C-c C-k   (to start the server)

## License

Copyright (C) 2010 FIXME

Distributed under the Eclipse Public License, the same as Clojure.

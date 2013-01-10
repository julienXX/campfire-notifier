Campfire Notifier
=================

A Campfire Notifier for OSX which display messages & mentions with Growl.

To install:
```shell
git clone git@github.com:julienXX/campfire-notifier.git
cd campfire-notifier
lein deps
cp config.clj{.template,}
edit config.clj to match your token and your rooms
lein run
```
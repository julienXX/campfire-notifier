Campfire Notifier
=================

A Campfire Notifier for OSX which displays messages & mentions with Growl or Notification Center.

To install:
```shell
git clone git@github.com:julienXX/campfire-notifier.git
cd campfire-notifier
lein deps
cp config.clj{.template,}
edit config.clj to match your token, rooms and other options
lein run OR lein uberjar && java -jar target/foo.jar
```

TODO:
- [ ] Add tests
- [ ] Display Rooms/Users names in notification

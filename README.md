# TeamCity Commit Status Publisher

This is a Cockroach Labs fork of https://github.com/JetBrains/commit-status-publisher which tweaks the commit-status
behavior slightly to suppress spurious failures of project builds if a sub-project encounters a retriable error. This
will happen, for example, when using preemptible builder agents. 

This a hack-and-slash approach to fixing a problem.

## Building

You'll need a JDK and a local copy of TeamCity in order to build against various APIs.

Create a `gradle.properties` file in the top-level directory with the following:

```properties
TeamCityLibs=/Users/bob/Downloads/TeamCity/webapps/ROOT/WEB-INF/lib/
TeamCityTestLibs=/Users/bob/Downloads/TeamCity/webapps/ROOT/WEB-INF/lib/
PluginVersion=99999
TeamCityVersion=2019.2
``` 

Update the library location to wherever you have have unpacked TC.

Run `./gradlew zip` to build a new version of the plugin and upload it via the TC web interface.  The `PluginVersion`
needs to be higher than the value of the version that ships with TC in order to override it.

## Logging

It's useful to bump the log level of this package. On the TC server, edit
`/home/teamcity/TeamCity/conf/teamcity-server-log4j.xml`. You do not need to restart the server for this change to
take effect. The relevant output is in the `TeamCity/logs/teamcity-server.log` file.

```xml
<category name="jetbrains.buildServer.commitPublisher.github">
  <priority value="DEBUG"/>
</category>
```

# ericsson-jenkins-flowdock-notifier

A simple Groovy script which enables Flowdock notifications to be sent from a Jenkins Pipeline.  The current Flowdock notification plugin does not have any Pipeline integration--here is a stopgap.

Inspired by and lifted from [this thread and this comment specifically](https://github.com/jenkinsci/flowdock-plugin/issues/24#issuecomment-271784565).
and
https://github.com/c3tp/jenkins-flowdock-notifier

## Prerequisites

The following must be available in your Jenkins installation to use this library:

* Pipeline plugins, of course
* [Pipeline: Shared Groovy Libraries](https://wiki.jenkins-ci.org/display/JENKINS/Pipeline+Shared+Groovy+Libraries+Plugin) plugin
* [HTTP Reuqest Plugin](https://wiki.jenkins.io/display/JENKINS/HTTP+Request+Plugin ) plugin
* An appropriate source code management plugin supported by the above, probably [the GitHub Branch Source](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Branch+Source+Plugin) plugin

## Installation

A Jenkins administrator must install the library as follows:

1. In _Manage Jenkins_ => _Configure System_, find the _Global Pipeline Libraries_ section.
2. Click on _Add_.
3. Specify:
  * Name: `enhanced-flowdock-notifier`
  * Default version: `master`
4. For retrieval method, select _Modern SCM_ and then _GitLab_:
  * Owner: `eeddir`
  * Repository: ` enhanced-flowdock-pipeline`
5. Click _Save_ at the bottom.

## Use

You must declare use of the library somewhere before use:

```library 'enhanced-flowdock-notifier'```

Then, call `enhancedFlowdockNotify` in an appropriate place in your pipeline.  It needs to be a _step_-class block.  There are four arguments:

1. `this` - passes the script object to the notifier function to provide necessary context
2. API token for Flowdock.  This can be retrieved from your Flowdock profile page
3. A string containing list of tags (optional).
4. An parameter indicating whether a chat notification shall be sent for a successful build (SEND_CHAT_FOR_SUCCESS, default), or not (NO_CHAT_FOR_SUCCESS)

For example:

```
pipeline {
   //...
   stage('Flowdock Notifier') {
      library 'enhanced-flowdock-notifier'
      enhancedFlowdockNotify this,'token', '#test', 'NO_CHAT_FOR_SUCCESS'
   }
}
```



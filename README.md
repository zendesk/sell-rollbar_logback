Rollbar Logback
=============

This is the Logback appender for use with [Rollbar](https://rollbar.com/), the error aggregation service. You will need a Rollbar account: sign up for an account [here](https://rollbar.com/signup/).


Setup via SBT
-------------

Add a library dependency to your build.sbt file
	
	libraryDependencies += "com.github.ahaid" % "rollbar-logback_2.10" % "0.1-SNAPSHOT"
     

The easy way to use the rollbar notifier is configuring a Logback appender. Otherwise if you don't use Logback you can use the rollbar notifier directly with a very simple API.

Logback
-----

	<appender name="ROLLBAR" class="com.ahaid.rollbar.logback.RollbarAppender">
        <apiKey>[YOUR APIKEY HERE]</apiKey>
        <environment>local</environment>
        <enabled>true</enabled>
        <onlyThrowable>false</onlyThrowable>
        <logs>true</logs>
        <limit>1000</limit>
    </appender>
	<root level="debug">
		<appender-ref ref="ROLLBAR"/>
	</root>
	
Appender parameters:

* api_key: The rollbar API key. Mandatory.
* environment: Environment. i.e. production, test, development. Mandatory.
* enabled: Enable the notifications. Default: true
* onlyThrowable: Only notify throwables skipping messages. Default: true
* logs: Send the last log lines attached to the notification. The log lines would be formatted with the configured layout. Default: true
* limit: The number of log lines to send attached to the notification. Default: 1000
* url: The Rollbar API url. Default: https://api.rollbar.com/api/1/item/


Directly
------------------------------

Example:

	// init Rollbar notifier
	RollbarNotifier.init(url, apiKey, env);

	try {
  		doSomethingThatThrowAnException();
	} catch(Throwable throwable) {
  		RollbarNotifier.notify(throwable);
	}

The RollbarNotifier object has several static methods that can be used to notify:
* RollbarNotifier.notify(message)
* RollbarNotifier.notify(message, context)
* RollbarNotifier.notify(level, message)
* RollbarNotifier.notify(level, message, context)
* RollbarNotifier.notify(throwable)
* RollbarNotifier.notify(throwable, context)
* RollbarNotifier.notify(message, throwable)
* RollbarNotifier.notify(message, throwable, context)
* RollbarNotifier.notify(level, throwable)
* RollbarNotifier.notify(level, throwable, context)
* RollbarNotifier.notify(level, message, throwable, context)


The parameters are:
* Message: String to notify 
* Throwable: Throwable to notify
* Context: Notification context. See Context section.
* Level: Notification level (don't confuse with the Log4j level). By default a throwable notification will be notified with a "error" level and a message notification as a "info" level.

Context
------------------------------

The rollbar notifier use a context to add additional information to the notification and help to solve any detected problem. The notifier try to be smart with the context values: 

* The rollbar notifier would add any known context value in the correct place in the notification message (To understand the notification message and the possible values see the [rollbar API item] (https://rollbar.com/docs/api_items/).)
* All the String context values, known and unknown, would be also add as custom parameters
* If any value is a HTTPSession, the session attributes would be extracted and added as custom parameters with the prefix "session.".
* If any value is a HttpServletRequest, the request attributes would be extracted and added as custom parameters with the prefix "attributes.".


The rollbar notifier library can recognize the values with the following keys:

* platform: String.
* framework: String.
* user: String. User ID.
* username: String. User name.
* email: String. User email.
* url: String. URL request
* method: String. HTTP method
* headers: Map<String, String>. HTTP headers.
* params: Map<String, String>. HTTP parameters.
* query. String. HTTP query string.
* user-ip: String. Request IP origin.
* session: String. HTTP session ID.
* protocol: String. HTTP protocal (http/https)
* requestId: String. Request ID.
* user-agent: String. User-agent of the browser that make the request.
* request: HttpServletRequest. It would be used to calculate the following values if they don't exist already in the context: url, method, headers, params, query, user-ip, session, user-agent.

Most of these values only make sense for J2EE applications.

Logback Context
------------------------------

The logback appender would use the MDC logback as the notification context. 

A very useful pattern is to use a J2EE filter to add helpful parameters to the MDC logback context. See for instance the [filter example] (https://github.com/ahaid/rollbar-logback/blob/master/src/main/java/com/ahaid/rollbar/logback/RollbarFilter.java)

Acknowledges
--------------

This library has been inspired by [rollbar-java] (https://github.com/rafael-munoz/rollbar-java)

License
-------

<pre>
This software is licensed under the Apache 2 license, quoted below.

Copyright 2014 Adam Haid

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
</pre>


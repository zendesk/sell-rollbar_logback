THIS REPOSITORY IS UNDER CONSTRUCTIONTION
Do not attempt to use the code here until this message is removed

Rollbar Java
=============

This is the notifier library for integrating Java apps with [Rollbar](https://rollbar.com/), the error aggregation service. You will need a Rollbar account: sign up for an account [here](https://rollbar.com/signup/).


Setup
-------------

You need to add these libraries from the [release] (https://github.com/rafael-munoz/rollbar-java/releases/download/v0.1/rollbar-java-v0.1.zip) to your classpath:
 * rollbar-java-0.1
 * log4j-1.2.14
 * json-20120521


The easy way to use the rollbar notifier is configuring a Log4j appender. Otherwise if you don't use log4j you can use the rollbar notifier directly with a very simple API.

Log4j
-----

Example:

	log4j.rootLogger=DEBUG, stdout, rollbar

	log4j.appender.stdout=org.apache.log4j.ConsoleAppender
	log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
	log4j.appender.stdout.layout.ConversionPattern=[%d,%p] [%c{1}.%M:%L] %m%n

	log4j.appender.rollbar=airbrake.AirbrakeAppender	
	log4j.appender.rollbar.api_key=YOUR_ROLLBAR_API_KEY
	#log4j.appender.rollbar.env=development
	#log4j.appender.rollbar.env=production
	log4j.appender.rollbar.env=test
	log4j.appender.rollbar.enabled=true
	log4j.appender.rollbar.onlyThrowable=true
	log4j.appender.rollbar.notifyLevel=error
	log4j.appender.rollbar.logs=true
	log4j.appender.rollbar.limit=1000
	#log4j.appender.rollbar.url=https://api.rollbar.com/api/1/item/

or in XML format:

	<appender name="ROLLBAR" class="com.muantech.rollbar.java.RollbarAppender">
	    <param name="enabled" value="false"/>
	    <param name="api_key" value="YOUR_ROLLBAR_API_KEY"/> 
	    <param name="env" value="test"/>
		<param name="onlyThrowable" value="true" />
		<param name="notifyLevel" value="error" />
		<param name="logs" value="true" />
		<param name="limit" value="1000" />
		<param name="url" value="https://api.rollbar.com/api/1/item/" />
	 	<layout class="org.apache.log4j.PatternLayout">
			<param name="ConversionPattern" value="%d{yyyy-MM-dd HH:mm:ss,SSS}-[%X{user}:%X{requestId}] %-5p %c{1} - %m%n" />
		</layout>
	</appender>

	<root>
		<appender-ref ref="ROLLBAR"/>
	</root>
	
Appender parameters:
* api_key: The rollbar API key. Mandatory.
* env: Environment. i.e. production, test, development. Mandatory.
* enabled: Enable the notifications. Default: true
* onlyThrowable: Only notify throwables skipping messages. Default: true
* notifyLevel: Only notify if the log4j level is equal or greater of this value. Default: error
* logs: Send the last log lines attached to the notification. The log lines would be formatted with the configured layout. Default: true
* limit: The number of log lines to send attached to the notification. Default: 1000
* url: The Rollbar API url. Default: https://api.rollbar.com/api/1/item/

It's important to distinguish between:
- The usual Log4j level: Log lines with level equal or greater than the Log4j level will be added to the logs buffer to be attached to the notifications and only notified if fulfill additional criteria (onlyThrowable and notifyLevel).
- The notifyLevel setting: Only log lines with level equal or greater than notifyLevel will be notify.

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

Log4j Context
------------------------------

The log4j appender would use the MDC log4j as the notification context. 

A very useful pattern is to use a J2EE filter to add helpful parameters to the MDC log4j context. See for instance the [filter example] (https://github.com/rafael-munoz/rollbar-java/blob/master/src/com/muantech/rollbar/java/RollbarFilter.java)

Acknowledges
--------------

This library has been inspired by [airbrake-java] (https://github.com/airbrake/airbrake-java)

License
-------

<pre>
This software is licensed under the Apache 2 license, quoted below.

Copyright 2014 Rafael Mu�oz Vega

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


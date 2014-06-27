Titanium Facebook Module
================================

Major Highlights
------------
*	Share Dialog functionality! Should take care of the vast majority of your publishing needs, with no permissions issues.
*	[Automatic error handling](https://developers.facebook.com/docs/ios/automatic-error-handling/) should make handling errors much easier. Please read the Facebook docs - I implemented all the new options.

Build notes
------------
* In module.xcconfig modify the -F option to the path on your machine
* Keep an eye on [TIMOB-15357](https://jira.appcelerator.org/browse/TIMOB-15357) and use the pull request referenced there, until integrated. The module will not work without this change.
* Note that the FacebookSDK.framework directory is the prebuilt Facebook SDK directly downloaded from Facebook, zero modifications. 

Module Goals
------------

* No hacking of Facebook SDK in order to enable easy upgrades. In fact, this module uses the pre-built SDK dropped in. Don't mess with that.
* Reliable Facebook authorization
* Proper error recovery mechanisms and user messaging - per Facebook's specs
* Use recent, preferably current, Facebook SDKs otherwise the above is unlikely....
* Feature parity with Titanium's Android Facebook module
* Future: include additional Facebook SDK functionality, such as friend and place pickers

Module Versioning
-----------------

x.y.zt, where x.y.z is the Facebook iOS SDK version, t denotes the Titanium module version for this SDK.
This module version is 3.8.01 - i.e. uses Facebook iOS SDK 3.8.0

Module API
----------

The module tries to stick to the original Titanium Facebook iOS module API (distributed with Ti SDK 3.1.0).
However, there are some differences, so read carefully.

*	`appid` - parameter unused. However, per the SDK docs, the app ID needs to be added in an additional key in plist.info (or tiapp.xml).
	In addition to the required `<property name="ti.facebook.appid">FACEBOOK_APP_ID</property>`, we also need to add the following in the ios plist dictionary in tiapp.xml:
*	`<key>FacebookAppID</key> <string>1234567891011</string>`
*	`<key>FacebookDisplayName</key> <string>SomeName</string>` where SomeName is exactly as appears in the Facebook developer settings page
*	Also make sure you have a URL Scheme in tiapp.xmp that looks like fb1234567891010. See [Facebook docs](https://developers.facebook.com/docs/ios/getting-started/) for details on this.
*	`forceDialogAuth` - parameter unused.
*	The login button functionality is for now removed. It makes no sense to use a button besides the Facebook branded buttons in the SDK, and that is left for the future. 
*	Instead of "reauthorize" there is now requestNewReadPermissions and a separate requestNewPublishPermissions, as per the Facebook SDK. This provides much more flexibility and less nuisance to the users.

Events and error handling
-------------------------

The `login` and `logout` events work as in the original module. 
However, the error handling is now adhering to the new Facebook guidelines. Here is how to handle `login` events:
```javascript
fb.addEventListener('login', function(e) {
	if(e.success) {
		// do your thang.... 
	} else if (e.cancelled) {
		// login was cancelled, just show your login UI again
	} else if (e.error) {
		if (Ti.Platform.name === 'iPhone OS') {
			// For all of these errors - assume the user is logged out
			// so show your login UI
			if (e.error.indexOf('Go to Settings') === 0){
				// alert: Go to Settings > Facebook and turn ON My Cool App 
				alert(e.error + 'My Cool App')
			} else if (e.error.indexOf('Session Login Error') === 0){
				// Session was invalid - e.g. the user deauthorized your app, etc
				alert('Please login again.');
			} else if (e.error.indexOf('OTHER:') !== 0){
				// another error that may require user attention
				alert (e.error);
			} else {
				// This must be an error message that the user was already notified about
				// Due to the automatic error handling in the graph call
				// Don't surface it again
			}
		} else {
			// not iOS............
		}
	} else {
		// if not success, nor cancelled, nor error message then pop a generic message
		// e.g. "Check your network, etc" . This is per Facebook's instructions
```

Share Dialog
-------------

See the [Facebook docs](https://developers.facebook.com/docs/ios/share-dialog/)
Use it! You don't need permissions, you don't even need the user to be logged into your app with Facebook!
*	First check if you can use it - call `fb.getCanPresentShareDialog()` which returns a boolean.
*	Currently the callback in the module just prints success or error to the debug log
*	To share a user's status just call `fb.share({});`
*	To share a link call `fb.share({url: 'http://example.com' });`
*	To post a graph action call:

```javascript
fb.share({url: someUrl, namespaceObject: 'myAppnameSpace:graphObject', objectName: 'graphObject', imageUrl: someImageUrl, 
		title: aTitle, description: blahBlah, namespaceAction: 'myAppnameSpace:actionType', placeId: facebookPlaceId}`
```
For the graph action apparently only placeId is optional.

requestNewReadPermissions
-------------------------

```
var fb = require('facebook');
fb.requestNewReadPermissions(['read_stream','user_hometown', etc...], function(e){
	if(e.success){
		fb.requestWithGraphPath(...);
 	} else if (e.cancelled){
 		....
 	} else {
 		Ti.API.debug('Failed authorization due to: ' + e.error);
 	}
});
```
 
requestNewPublishPermissions
----------------------------
 
 You must use the audience constants from the module, either `audienceNone`, `audienceOnlyMe`, `audienceFriends`, or `audienceEveryone`.
 Note that it is not an error for the user to 'Skip' your requested permissions, so you should check the module's permissions property following the call.

```
var fb = require('facebook');
fb.requestNewPublishPermissions(['read_stream','user_hometown', etc...], fb.audienceFriends, function(e){
	if(e.success){
		fb.requestWithGraphPath(...);
 	} else if (e.cancelled){
	....
	} else {
		Ti.API.debug('Failed authorization due to: ' + e.error);
	}
});
```

requestWithGraphPath
--------------------

Same as the original Titanium Facebook module. However, there is automatic error handling.
So in case of error only alert the user if `error == 'An unexpected error'`, everything else was already handled for you.

To do
-------
*	Facebook branded buttons - use the SDK implementation or don't do it.
*	[Share sheet](https://developers.facebook.com/docs/ios/ios-6/#nativepostcontroller) - it's more lightweight than the Share Dialog, but also with many less features. Some apps use Share Dialog (e.g. Pintrest), some the Share Sheet (e.g. Foodspotting).
*	Additional dialogs. But why?!?!??!? They are web based, require permissions, few good apps use them today. Just use the Share Dialog, or Share Sheet, or don't bother, in my opinion.
	
Feel free to comment and help out! :)
-------------------------------------

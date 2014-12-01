/**
 * Facebook Module
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

#import "FacebookModule.h"
#import "TiBase.h"
#import "TiHost.h"
#import "TiBlob.h"
#import "TiUtils.h"
#import "TiApp.h"


BOOL temporarilySuspended = NO;
BOOL skipMeCall = NO;

@implementation FacebookModule
#pragma mark Internal

// this is generated for your module, please do not change it
-(id)moduleGUID
{
	return @"da8acc57-8673-4692-9282-e3c1a21f5d83";
}

// this is generated for your module, please do not change it
-(NSString*)moduleId
{
	return @"facebook";
}

#pragma mark Lifecycle

-(void)dealloc
{
	RELEASE_TO_NIL(stateListeners);
	RELEASE_TO_NIL(permissions);
    RELEASE_TO_NIL(appid);
    RELEASE_TO_NIL(urlSchemeSuffix);
	RELEASE_TO_NIL(uid);
	[super dealloc];
}

-(BOOL)handleRelaunch
{
	NSDictionary *launchOptions = [[TiApp app] launchOptions];
	if (launchOptions!=nil)
	{
		NSString *urlString = [launchOptions objectForKey:@"url"];
        NSString *sourceApplication = [launchOptions objectForKey:@"source"];
        if (urlString != nil) {
            return [FBAppCall handleOpenURL:[NSURL URLWithString:urlString] sourceApplication:sourceApplication];
        } else {
            return NO;
        }
	}
	return NO;
}

-(void)resumed:(id)note
{
	NSLog(@"[DEBUG] facebook resumed");
	[FBAppEvents activateApp];
	if (!temporarilySuspended) {
        [self handleRelaunch];
    }
}

-(void)activateApp:(NSNotification *)notification
{
    [FBAppCall handleDidBecomeActive];
}

-(void)startup
{
	NSLog(@"[DEBUG] facebook startup");
	[super startup];
}

-(void)shutdown:(id)sender
{
	NSLog(@"[DEBUG] facebook shutdown");

    TiThreadPerformOnMainThread(^{
        [FBSession.activeSession close];
    }, NO);

	[[NSNotificationCenter defaultCenter] removeObserver:self];
	[super shutdown:sender];
}

-(void)suspend:(id)sender
{
	NSLog(@"[DEBUG] facebook suspend");
    temporarilySuspended = YES; // to avoid crazy logic if user rejects a call or SMS
}

-(void)paused:(id)sender
{
	NSLog(@"[DEBUG] facebook paused");
    temporarilySuspended = NO; // Since we are guaranteed full resume logic following this
}

-(BOOL)isLoggedIn
{
    return loggedIn;
}

-(BOOL)passedShareDialogCheck
{
    return canShare;
}

#pragma mark Auth Internals

- (void)populateUserDetails {
    TiThreadPerformOnMainThread(^{
        if (FBSession.activeSession.isOpen) {
            FBRequestConnection *connection = [[FBRequestConnection alloc] init];
            connection.errorBehavior = FBRequestConnectionErrorBehaviorReconnectSession
                | FBRequestConnectionErrorBehaviorAlertUser
                | FBRequestConnectionErrorBehaviorRetry;

            [connection addRequest:[FBRequest requestForMe]
                completionHandler:^(FBRequestConnection *connection, NSDictionary<FBGraphUser> *user, NSError *error) {
                    RELEASE_TO_NIL(uid);
                    if (!error) {
                        uid = [[user objectForKey:@"id"] copy];
                        loggedIn = YES;
                        [self fireLogin:user cancelled:NO withError:nil];
                    } else {
                        // Error on /me call
                        // In a future rev perhaps use stored user info
                        // But for now bail out
                        NSLog(@"/me graph call error");
                        TiThreadPerformOnMainThread(^{
                            [FBSession.activeSession closeAndClearTokenInformation];
                        }, YES);
                        loggedIn = NO;
                        // We set error to nil since any useful message was already surfaced
                        [self fireLogin:nil cancelled:NO withError:nil];

                    }
            }];
            [connection start];
        }
    }, NO);
}

- (void)sessionStateChanged:(FBSession *)session
                      state:(FBSessionState) state
                      error:(NSError *)error
{
    RELEASE_TO_NIL(uid);
    if (error) {
        NSLog(@"sessionStateChanged error");
        loggedIn = NO;
        BOOL userCancelled = error.fberrorCategory == FBErrorCategoryUserCancelled;
        [self fireLogin:nil cancelled:userCancelled withError:error];
    } else {
        switch (state) {
            case FBSessionStateOpen:
                NSLog(@"[DEBUG] FBSessionStateOpen");
                [self populateUserDetails];
                break;
            case FBSessionStateClosed: break;
            case FBSessionStateClosedLoginFailed:
                NSLog(@"[DEBUG] facebook session closed");
                TiThreadPerformOnMainThread(^{
                    [FBSession.activeSession closeAndClearTokenInformation];
                }, YES);

                loggedIn = NO;
                [self fireEvent:@"logout"];
                break;
            default:
                break;
        }
    }
}

#pragma mark Public APIs

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * alert(facebook.uid);
 *
 */
-(id)uid
{
	return uid;
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * if (facebook.loggedIn) {
 * }
 *
 */
-(id)loggedIn
{
	return NUMBOOL([self isLoggedIn]);
}

-(id)canPresentShareDialog
{
	return NUMBOOL([self passedShareDialogCheck]);
}


/**
 * JS example:
 *
 * var facebook = require('facebook');
 * facebook.permissions = ['read_stream'];
 * alert(facebook.permissions);
 *
 */
-(id)permissions
{
    __block NSArray *perms;
    TiThreadPerformOnMainThread(^{
        perms = FBSession.activeSession.permissions;
    }, YES);

    return perms;
}

-(id)appid
{
    __block NSString *tempid;
    TiThreadPerformOnMainThread(^{
        tempid = FBSettings.defaultAppID;
    }, YES);

    return tempid;
}
-(id)urlSchemeSuffix
{
    __block NSString *tempid;
    TiThreadPerformOnMainThread(^{
        tempid = FBSettings.defaultUrlSchemeSuffix;
    }, YES);

    return tempid;
}
/**
 * JS example:
 *
 * var facebook = require('facebook');
 * alert(facebook.accessToken);
 *
 */

-(id)getAccessToken:(id)args
{
    __block NSString * token;
    TiThreadPerformOnMainThread(^{
        token = FBSession.activeSession.accessTokenData.accessToken;
    }, YES);

    return token;
}

-(id)audienceNone
{
    return [NSNumber numberWithInt:FBSessionDefaultAudienceNone];
}

-(id)audienceOnlyMe
{
    return [NSNumber numberWithInt:FBSessionDefaultAudienceOnlyMe];
}

-(id)audienceFriends
{
    return [NSNumber numberWithInt:FBSessionDefaultAudienceFriends];
}

-(id)audienceEveryone
{
    return [NSNumber numberWithInt:FBSessionDefaultAudienceEveryone];
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * alert(facebook.expirationDate);
 *
 */

-(id)getExpirationDate:(id)args
{
    __block NSDate *expirationDate;
    TiThreadPerformOnMainThread(^{
        expirationDate = FBSession.activeSession.accessTokenData.expirationDate;
    }, YES);

    return expirationDate;
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * facebook.permissions = ['publish_stream'];
 * alert(facebook.permissions);
 *
 */
-(void)setPermissions:(id)arg
{
	RELEASE_TO_NIL(permissions);
	permissions = [arg retain];
}

-(void)setAppid:(id)arg
{
    RELEASE_TO_NIL(appid);
    appid = [arg retain];
}

-(void)setUrlSchemeSuffix:(id)arg
{
    RELEASE_TO_NIL(urlSchemeSuffix);
    urlSchemeSuffix = [arg retain];
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 *
 * facebook.addEventListener('login',function(e) {
 *    // You *will* get this event if loggedIn == false below
 *    // Make sure to handle all possible cases of this event
 *    if (e.success) {
 *		alert('login from uid: '+e.uid+', name: '+e.data.name);
 *    }
 *    else if (e.cancelled) {
 *      // user cancelled logout
 *    }
 *    else {
 *      alert(e.error);
 *    }
 * });
 *
 * facebook.addEventListener('logout',function(e) {
 *    alert('logged out');
 * });
 *
 * facebook.permissions = ['email'];
 * facebook.initialize(); // after you set up login/logout listeners and permissions
 * if (!fb.getLoggedIn()) {
 * // then you want to show a login UI
 * // where you should have a button that when clicked calls
 * // facebook.authorize();
 *
 */

-(void)authorize:(id)args
{
	NSLog(@"[DEBUG] facebook authorize");

	TiThreadPerformOnMainThread(^{
		NSArray *permissions_ = permissions == nil ? [NSArray array] : permissions;
        NSString *appid_ = appid == nil ? @"" : appid;
        NSString *urlSchemeSuffix_ = urlSchemeSuffix == nil ? @"" : urlSchemeSuffix;
        [FBSettings setDefaultAppID:appid_];
        [FBSettings setDefaultUrlSchemeSuffix:urlSchemeSuffix_];
        [FBSession openActiveSessionWithReadPermissions:permissions_
                                           allowLoginUI:YES
                                      completionHandler:
            ^(FBSession *session,
                FBSessionState state, NSError *error) {
                [self sessionStateChanged:session state:state error:error];
         }];
	}, NO);
}

// We have this function so that you can set up your listeners and permissions whenever you want
// Call initialize when ready, you will get a login event if there was a cached token
// else loggedIn will be false
-(void)initialize:(id)args
{
	TiThreadPerformOnMainThread(^{
		NSNotificationCenter * nc = [NSNotificationCenter defaultCenter];
        //NSString * savedToken = [TiUtils stringValue:args];

        [nc addObserver:self selector:@selector(activateApp:) name:UIApplicationDidBecomeActiveNotification object:nil];

        FBShareDialogParams *params = [[FBShareDialogParams alloc] init];
        params.link = [NSURL URLWithString:@"http://developers.facebook.com/ios"];
        canShare = [FBDialogs canPresentShareDialogWithParams:params];

        if (FBSession.activeSession.state == FBSessionStateCreatedTokenLoaded) {
            // Start with logged-in state, guaranteed no login UX is fired since logged-in
            loggedIn = YES;
            //skipMeCall = [FBSession.activeSession.accessTokenData.accessToken isEqualToString:savedToken];
            [self authorize:nil];
        } else {
            loggedIn = NO;
        }
	}, YES);
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * facebook.logout();
 *
 */
-(void)logout:(id)args
{
	NSLog(@"[DEBUG] facebook logout");
	if ([self isLoggedIn])
	{
        RELEASE_TO_NIL(uid);
        TiThreadPerformOnMainThread(^{
            [FBSession.activeSession closeAndClearTokenInformation];
        }, NO);
        loggedIn = NO;
        [self fireEvent:@"logout"];
	}
}

-(void)share:(id)args
{
	NSLog(@"[DEBUG] facebook share");
	if (canShare){

        NSDictionary* params = [args objectAtIndex:0];
        NSString* urlStr = [params objectForKey:@"url"];
        NSURL* linkUrl = [NSURL URLWithString:urlStr];
        NSString* namespaceObject = [params objectForKey:@"namespaceObject"];
        NSString* namespaceAction = [params objectForKey:@"namespaceAction"];
        NSString* objectName = [params objectForKey:@"objectName"];
        NSString* placeId = [params objectForKey:@"placeId"];
        NSString* imageUrl = [params objectForKey:@"imageUrl"];
        NSString* openGraphTitle = [params objectForKey:@"title"];
        NSString* openGraphDescription = [params objectForKey:@"description"];

        TiThreadPerformOnMainThread(^{
            if (objectName == nil || namespaceObject == nil || namespaceAction == nil){
                [FBDialogs presentShareDialogWithLink:linkUrl
                    handler:^(FBAppCall *call, NSDictionary *results, NSError *error) {
                    if(error) {
                        NSLog(@"[DEBUG] Facebook share error %@", error.description);
                    } else {
                        NSLog(@"Facebook share success!");
                    }
                }];
            } else {
                id<FBGraphObject> openGraphObject =
                [FBGraphObject openGraphObjectForPostWithType:namespaceObject
                        title:openGraphTitle
                        image:imageUrl
                        url:urlStr
                        description:openGraphDescription];

                id<FBOpenGraphAction> openGraphAction = (id<FBOpenGraphAction>)[FBGraphObject graphObject];
                [openGraphAction setObject:openGraphObject forKey:objectName];

                if (placeId != nil){
                    id<FBGraphPlace> place = (id<FBGraphPlace>)[FBGraphObject graphObject];
                    [place setId:placeId];

                    [openGraphAction setPlace:place];
                }

                [FBDialogs presentShareDialogWithOpenGraphAction:openGraphAction
                        actionType:namespaceAction
                        previewPropertyName:objectName
                        handler:^(FBAppCall *call, NSDictionary *results, NSError *error) {
                            if(error) {
                                NSLog(@"Error: %@", error.description);
                            } else {
                                NSLog(@"Success!");
                            }
                            }];
            }
        }, NO);
    }
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * ...
 * facebook.requestNewReadPermissions(['read_stream','user_hometown', etc...], function(e){
 *     if(e.success){
 *         facebook.requestWithGraphPath(...);
 *     } else if (e.cancelled){
 *         .....
 *     } else {
 *         Ti.API.debug('Failed authorization due to: ' + e.error);
 *     }
 * });
 */
-(void)requestNewReadPermissions:(id)args
{
	ENSURE_ARG_COUNT(args, 2);

	NSArray * readPermissions = [args objectAtIndex:0];
	KrollCallback * callback = [args objectAtIndex:1];

	TiThreadPerformOnMainThread(^{
        [FBSession.activeSession requestNewReadPermissions:readPermissions
                completionHandler:^(FBSession *session, NSError *error) {
                    bool success = (error == nil);
                    bool cancelled = NO;
                    NSString * errorString = nil;
                    int code = 0;
                    if(!success)
                    {
                        code = [error code];
                        if (code == 0)
                        {
                            code = -1;
                        }
                        if (error.fberrorCategory == FBErrorCategoryUserCancelled) {
                            cancelled = YES;
                        } else if (error.fberrorShouldNotifyUser) {
                            errorString = error.fberrorUserMessage;
                        } else {
                            errorString = @"An unexpected error";
                        }
                    }

                    NSNumber * errorCode = [NSNumber numberWithInteger:code];
                    NSDictionary * propertiesDict = [[NSDictionary alloc] initWithObjectsAndKeys:
                                                     [NSNumber numberWithBool:success],@"success",
                                                     [NSNumber numberWithBool:cancelled],@"cancelled",
                                                     errorCode,@"code", errorString,@"error", nil];

                    KrollEvent * invocationEvent = [[KrollEvent alloc] initWithCallback:callback eventObject:propertiesDict thisObject:self];
                    [[callback context] enqueue:invocationEvent];
                    [invocationEvent release];
                    [propertiesDict release];
        }];
	}, NO);
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 * ...
 * facebook.requestNewPublishPermissions(['read_stream','user_hometown', etc...], fb.audienceFriends, function(e){
 *     if(e.success){
 *         facebook.requestWithGraphPath(...);
 *     } else if (e.cancelled){
 *         .....
 *     } else {
 *         Ti.API.debug('Failed authorization due to: ' + e.error);
 *     }
 * });
 */
-(void)requestNewPublishPermissions:(id)args
{
	ENSURE_ARG_COUNT(args, 3);

	NSArray * writePermissions = [args objectAtIndex:0];
    FBSessionDefaultAudience defaultAudience = [TiUtils intValue:[args objectAtIndex:1]];
	KrollCallback * callback = [args objectAtIndex:2];

	TiThreadPerformOnMainThread(^{
        [FBSession.activeSession requestNewPublishPermissions:writePermissions
            defaultAudience:defaultAudience
            completionHandler:^(FBSession *session, NSError *error) {
                bool success = (error == nil);
                bool cancelled = NO;
                NSString * errorString = nil;
                int code = 0;
                if(!success)
                {
                    code = [error code];
                    if (code == 0)
                    {
                        code = -1;
                    }
                    if (error.fberrorCategory == FBErrorCategoryUserCancelled) {
                        cancelled = YES;
                    } else if (error.fberrorShouldNotifyUser) {
                        errorString = error.fberrorUserMessage;
                    } else {
                        errorString = @"An unexpected error";
                    }
                }

                NSNumber * errorCode = [NSNumber numberWithInteger:code];
                NSDictionary * propertiesDict = [[NSDictionary alloc] initWithObjectsAndKeys:
                                                 [NSNumber numberWithBool:success],@"success",
                                                 [NSNumber numberWithBool:cancelled],@"cancelled",
                                                 errorCode,@"code", errorString,@"error", nil];

                KrollEvent * invocationEvent = [[KrollEvent alloc] initWithCallback:callback eventObject:propertiesDict thisObject:self];
                [[callback context] enqueue:invocationEvent];
                [invocationEvent release];
                [propertiesDict release];
            }];
	}, NO);
}

/**
 * JS example:
 *
 * var facebook = require('facebook');
 *
 * facebook.requestWithGraphPath('me',{}, 'post', function(e) {
 *    if (e.success) {
 *      // e.path contains original path (e.g. 'me'), e.graphData contains the result
 *    }
 *    else {
 *      // note that we use new Facebook error handling
 *      // thus if there was any user action to take - he was already notified
 *      // see https://developers.facebook.com/docs/ios/automatic-error-handling/
 *      alert(e.error);
 *    }
 * });
 *
 */
-(void)requestWithGraphPath:(id)args
{
	VerboseLog(@"[DEBUG] facebook requestWithGraphPath");

	ENSURE_ARG_COUNT(args,4);

	NSString* path = [args objectAtIndex:0];
	NSMutableDictionary* params = [args objectAtIndex:1];
	NSString* httpMethod = [args objectAtIndex:2];
	KrollCallback* callback = [args objectAtIndex:3];

    TiThreadPerformOnMainThread(^{
        FBRequestConnection *connection = [[FBRequestConnection alloc] init];
        connection.errorBehavior = FBRequestConnectionErrorBehaviorReconnectSession
        | FBRequestConnectionErrorBehaviorAlertUser
        | FBRequestConnectionErrorBehaviorRetry;

        FBRequest *request = [FBRequest requestWithGraphPath:path
                                                  parameters:params
                                                  HTTPMethod:httpMethod];

        [connection addRequest:request
             completionHandler:^(FBRequestConnection *connection, id result, NSError *error) {
                 NSDictionary * returnedObject;
                 BOOL success;
                 if (!error) {
                     success = YES;
                     returnedObject = [[NSDictionary alloc] initWithObjectsAndKeys:
                                       result,@"graphData", NUMBOOL(success), @"success",
                                       path, @"path",nil];
                 } else {
                     NSLog(@"/me graph call error");
                     success = NO;
                     NSString * errorString;
                     if (error.fberrorShouldNotifyUser) {
                         errorString = error.fberrorUserMessage;
                     } else {
                         errorString = @"An unexpected error";
                     }
                     returnedObject = [[NSDictionary alloc] initWithObjectsAndKeys:
                                       NUMBOOL(success), @"success",
                                       path, @"path", errorString, @"error", nil];

                 }
                 KrollEvent * invocationEvent = [[KrollEvent alloc] initWithCallback:callback eventObject:returnedObject thisObject:self];
                 [[callback context] enqueue:invocationEvent];
                 [invocationEvent release];
                 [returnedObject release];

             }];
        [connection start];
    }, NO);
}

-(void)publishPermissionResult:(KrollCallback*)callback withProperties:(NSDictionary*)propertiesDict {
    KrollEvent * invocationEvent = [[KrollEvent alloc] initWithCallback:callback eventObject:propertiesDict thisObject:self];
    [[callback context] enqueue:invocationEvent];
    [invocationEvent release];
    [propertiesDict release];
}

-(id)getPublishPermission:(id)args {
    
    if (FBSession.activeSession.isOpen) {
        // Refreshes the current permissions for the session, to make sure the local permissions are up to date
        [FBSession.activeSession refreshPermissionsWithCompletionHandler:^(FBSession *session, NSError *error) {}];
        if ([FBSession.activeSession.permissions
             indexOfObject:@"publish_actions"] == NSNotFound) {
            // No permissions found in session, ask for it
            publishPermission =  NO;
        } else {
            // If publish permission present
            publishPermission =  YES;
        }
    } else {
        // Session is not open
        publishPermission = NO;
    }
        
    return NUMBOOL(publishPermission);
}

-(void)checkPublishPermission:(id)args {
    ENSURE_ARG_COUNT(args,1);
    NSDictionary * callback = [args objectAtIndex:0];
    KrollCallback * successCallback = [callback objectForKey: @"success"];
    KrollCallback * cancelCallback =  [callback objectForKey: @"cancel"];
    KrollCallback * errorCallback =   [callback objectForKey: @"error"];
    TiThreadPerformOnMainThread(^{
        if (FBSession.activeSession.isOpen) {
            // Refreshes the current permissions for the session, to make sure the local permissions are up to date
            [FBSession.activeSession refreshPermissionsWithCompletionHandler:^(FBSession *session, NSError *error) {}];
            if ([FBSession.activeSession.permissions
                 indexOfObject:@"publish_actions"] == NSNotFound) {
                // No permissions found in session, ask for it
                [FBSession.activeSession
                 requestNewPublishPermissions:
                 [NSArray arrayWithObject:@"publish_actions"]
                 defaultAudience:FBSessionDefaultAudienceFriends
                 completionHandler:^(FBSession *session, NSError *error) {
                    bool success = (error == nil);
                    bool cancelled = NO;
                    NSString *errorString = nil;
                    int *code = 0;
                    if (!error){
                        if ([session.permissions indexOfObject:@"publish_actions"] == NSNotFound) {
                            // Publish permissions not found
                            success = NO;
                            cancelled = YES;
                        } else {
                            // Publish permissions found
                            success = YES;
                            // Refreshes the current permissions for the session.
                            [session refreshPermissionsWithCompletionHandler:^(FBSession *session, NSError *error) {}];
                        }
                      } else {
                          // There was an error, handle it
                          // See https://developers.facebook.com/docs/ios/errors/
                          success = NO;
                          code = [error code];
                          if (code == 0)
                          {
                              code = -1;
                          }
                          if (error.fberrorCategory == FBErrorCategoryUserCancelled) {
                              cancelled = YES;
                          } else if (error.fberrorShouldNotifyUser) {
                             errorString = error.fberrorUserMessage;
                          } else {
                             errorString = @"An unexpected error...";
                         }
                      }

                      NSNumber * errorCode = [NSNumber numberWithInteger:code];
                      NSDictionary * propertiesDict = [[NSDictionary alloc] initWithObjectsAndKeys:
                                                       [NSNumber numberWithBool:success],@"success",
                                                       [NSNumber numberWithBool:cancelled],@"cancel",
                                                       errorCode,@"code", errorString, @"error", nil];
                     
                     if (success) {
                         [self publishPermissionResult:successCallback withProperties:propertiesDict];
                     } else if (cancelled) {
                         [self publishPermissionResult:cancelCallback withProperties:propertiesDict];
                     } else {
                         [self publishPermissionResult:errorCallback withProperties:propertiesDict];
                     }
        
                  }];
            } else {
                // If publish permission present
                NSDictionary * propertiesDict = [[NSDictionary alloc] initWithObjectsAndKeys:
                                                 [NSNumber numberWithBool:YES],@"success",
                                                 [NSNumber numberWithBool:NO], @"cancel",
                                                 @"", @"code", @"", @"error", nil];
                [self publishPermissionResult:successCallback withProperties:propertiesDict];
            }
        } else {
            // Session is not open
            NSDictionary * propertiesDict = [[NSDictionary alloc] initWithObjectsAndKeys:
                                             [NSNumber numberWithBool:NO],@"success",
                                             [NSNumber numberWithBool:NO],@"cancel",
                                             @"",@"code", @"Session is not open",@"error", nil];
           [self publishPermissionResult:errorCallback withProperties:propertiesDict];
        }
    }, NO);
}

- (NSDictionary*)parseURLParams:(NSString *)query {
    NSArray *pairs = [query componentsSeparatedByString:@"&"];
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
    for (NSString *pair in pairs) {
        NSArray *kv = [pair componentsSeparatedByString:@"="];
        NSString *val =
        [kv[1] stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        params[kv[0]] = val;
    }
    return params;
}

-(void)shareDialog:(id)args {
    ENSURE_ARG_COUNT(args,1);
    NSDictionary * dict = [args objectAtIndex:0];
    NSString * stringUrl            = [NSString stringWithFormat:@"%@",[dict objectForKey:@"url"]];
    NSString * stringTitle          = [NSString stringWithFormat:@"%@",[dict objectForKey:@"title"]];
    NSString * stringMessage        = [NSString stringWithFormat:@"%@",[dict objectForKey:@"message"]];
    NSString * stringUrlImage       = [NSString stringWithFormat:@"%@",[dict objectForKey:@"url_image"]];
    KrollCallback * successCallback = [dict objectForKey: @"success"];
    KrollCallback * cancelCallback  = [dict objectForKey: @"cancel"];
    KrollCallback * errorCallback   = [dict objectForKey: @"error"];
    TiThreadPerformOnMainThread(^{

        NSURL *urlToShare = [NSURL URLWithString:stringUrl];
        FBLinkShareParams *params = [[FBLinkShareParams alloc] initWithLink:urlToShare
        name:stringTitle
        caption:nil
        description:stringMessage
        picture:[NSURL URLWithString:stringUrlImage]];
        
        if ([FBDialogs canPresentShareDialogWithParams:params]) {
            FBAppCall *appCall = [FBDialogs presentShareDialogWithParams:params
            clientState:nil
            handler:^(FBAppCall *call, NSDictionary *results, NSError *error) {
                bool success;
                bool cancelled = NO;
                if (!error) {
                    if ([[results objectForKey: @"completionGesture"] isEqualToString:@"cancel"]) {
                        success = NO;
                        cancelled = YES;
                        NSLog(@"Facebook share Cancel");
                    } else {
                        success = YES;
                        NSLog(@"Facebook share Success!");
                    }
                } else {
                    success = NO;
                    NSLog(@"Facebook share Error: %@", error.description);
                }
             
                NSDictionary * propertiesDict = [[NSDictionary alloc] initWithObjectsAndKeys:
                                                 [NSNumber numberWithBool:success],@"success",
                                                 [NSNumber numberWithBool:cancelled],@"cancel",
                                                  @"An unexpected error...", @"error", nil];
                if (success) {
                    [self publishPermissionResult:successCallback withProperties:propertiesDict];
                } else if (cancelled) {
                    [self publishPermissionResult:cancelCallback withProperties:propertiesDict];
                } else {
                    [self publishPermissionResult:errorCallback withProperties:propertiesDict];
                }

            }];
        } else {
            //Feed dialog
            // Put together the dialog parameters
            NSMutableDictionary *params =
            [NSMutableDictionary dictionaryWithObjectsAndKeys:
             stringTitle, @"name",
             stringMessage, @"description",
             stringUrl, @"link",
             stringUrlImage, @"picture",
             nil];
            
            // Invoke the dialog
            [FBWebDialogs presentFeedDialogModallyWithSession:nil
                                                   parameters:params
                                                      handler:
             ^(FBWebDialogResult result, NSURL *resultURL, NSError *error) {
                 bool success;
                 bool cancelled = NO;
                 if (error) {
                     // Error launching the dialog or publishing a story.
                     NSLog(@"Error publishing story.");
                     success = NO;
                 } else {
                     if (result == FBWebDialogResultDialogNotCompleted) {
                         // User clicked the "x" icon
                         NSLog(@"User canceled story publishing.");
                         success = NO;
                         cancelled = YES;
                     } else {
                         // Handle the publish feed callback
                         NSDictionary *urlParams = [self parseURLParams:[resultURL query]];
                         if (![urlParams valueForKey:@"post_id"]) {
                             // User clicked the Cancel button
                             NSLog(@"User canceled story publishing.");
                             success = NO;
                             cancelled = YES;
                         } else {
                             // User clicked the Share button
                             NSString *msg = [NSString stringWithFormat:
                                              @"Posted story, id: %@",
                                              [urlParams valueForKey:@"post_id"]];
                             NSLog(@"%@", msg);
                             NSLog(@"Facebook share (feed) Success!");
                             success = YES;
                        
                         }
                     }
                 }
                 NSDictionary * propertiesDict = [[NSDictionary alloc] initWithObjectsAndKeys:
                                                  [NSNumber numberWithBool:success],@"success",
                                                  [NSNumber numberWithBool:cancelled],@"cancel",
                                                  @"An unexpected error...", @"error", nil];
                 if (success) {
                     [self publishPermissionResult:successCallback withProperties:propertiesDict];
                 } else if (cancelled) {
                     [self publishPermissionResult:cancelCallback withProperties:propertiesDict];
                 } else {
                     [self publishPermissionResult:errorCallback withProperties:propertiesDict];
                 }
            }];
        }

    }, NO);
}

-(void)eventCoupon:(id)args {
    ENSURE_ARG_COUNT(args,1);
    NSDictionary * dict = [args objectAtIndex:0];
    NSString * stringNumItems    = [NSString stringWithFormat:@"%@",[dict objectForKey:@"numItems"]];
    NSString * stringContentType = [NSString stringWithFormat:@"%@",[dict objectForKey:@"contentType"]];
    NSString * stringContentID   = [NSString stringWithFormat:@"%@",[dict objectForKey:@"contentID"]];
    NSString * stringCurrency    = [NSString stringWithFormat:@"%@",[dict objectForKey:@"currency"]];
    TiThreadPerformOnMainThread(^{
        [FBAppEvents logEvent:FBAppEventNamePurchased
         valueToSum:[[dict objectForKey:@"valueToSum"] doubleValue]
         parameters:@{ FBAppEventParameterNameNumItems    : stringNumItems,
           FBAppEventParameterNameContentType : stringContentType,
           FBAppEventParameterNameContentID   : stringContentID,
           FBAppEventParameterNameCurrency    : stringCurrency } ];
    }, NO);
}

#pragma mark Listener work

-(void)fireLogin:(id)result cancelled:(BOOL)cancelled withError:(NSError *)error
{
	BOOL success = (result != nil);
	int code = [error code];
	if ((code == 0) && !success)
	{
		code = -1;
	}
	NSMutableDictionary *event = [NSMutableDictionary dictionaryWithObjectsAndKeys:
								  NUMBOOL(cancelled),@"cancelled",
								  NUMBOOL(success),@"success",
								  NUMINT(code),@"code",nil];
	if(error != nil){
        NSString * errorMessage = @"OTHER: ";
        if (error.fberrorShouldNotifyUser) {
            if ([[error userInfo][FBErrorLoginFailedReason]
                 isEqualToString:FBErrorLoginFailedReasonSystemDisallowedWithoutErrorValue]) {
                // Show a different error message
                errorMessage = @"Go to Settings > Facebook and turn ON ";
            } else {
                // If the SDK has a message for the user, surface it.
                errorMessage = error.fberrorUserMessage;
            }
        } else if (error.fberrorCategory == FBErrorCategoryAuthenticationReopenSession) {
            // It is important to handle session closures as mentioned. You can inspect
            // the error for more context but this sample generically notifies the user.
            errorMessage = @"Session Login Error";
        } else if (error.fberrorCategory == FBErrorCategoryUserCancelled) {
            // The user has cancelled a login. You can inspect the error
            // for more context. For this sample, we will simply ignore it.
            errorMessage = @"User cancelled the login process.";
        } else {
            // For simplicity, this sample treats other errors blindly, but you should
            // refer to https://developers.facebook.com/docs/technical-guides/iossdk/errors/ for more information.
            errorMessage = [errorMessage stringByAppendingFormat:@" %@", (NSString *) error];
        }
        [event setObject:errorMessage forKey:@"error"];
	}

	if(result != nil)
	{
		[event setObject:result forKey:@"data"];
		if (uid != nil)
		{
			[event setObject:uid forKey:@"uid"];
		}
	}
	[self fireEvent:@"login" withObject:event];
}


#pragma mark Listeners

-(void)addListener:(id<TiFacebookStateListener>)listener
{
	if (stateListeners==nil)
	{
		stateListeners = [[NSMutableArray alloc]init];
	}
	[stateListeners addObject:listener];
}

-(void)removeListener:(id<TiFacebookStateListener>)listener
{
	if (stateListeners!=nil)
	{
		[stateListeners removeObject:listener];
		if ([stateListeners count]==0)
		{
			RELEASE_TO_NIL(stateListeners);
		}
	}
}

@end

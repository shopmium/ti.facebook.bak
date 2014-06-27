/**
 * Facebook Module
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiModule.h"
#import "FBConnect/FacebookSDK.h"

@protocol TiFacebookStateListener
@required
-(void)login;
-(void)logout;
@end


@interface FacebookModule : TiModule
{
	BOOL loggedIn;
    BOOL canShare;
	NSString *uid;
	//NSString *url;
	NSArray *permissions;
	NSMutableArray *stateListeners;
}

-(BOOL)isLoggedIn;
-(BOOL)passedShareDialogCheck;
-(void)addListener:(id<TiFacebookStateListener>)listener;
-(void)removeListener:(id<TiFacebookStateListener>)listener;

-(void)authorize:(id)args;
-(void)logout:(id)args;


@end

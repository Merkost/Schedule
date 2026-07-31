import UIKit
import FirebaseCore
import FirebaseMessaging
import GoogleSignIn
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler:
            @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound, .badge])
    }

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
#if DEBUG
        let isDebug = true
#else
        let isDebug = false
#endif
        MainViewControllerKt.doInitKoinIos(isDebug: isDebug)
        KMPNotifier.shared.initialize(
            configuration: NotificationPlatformConfigurationIos(
                showPushNotification: true,
                askNotificationPermissionOnStart: false,
                notificationSoundName: nil
            ),
            extensions: [FirebasePush.shared]
        )
        GoogleSignInBridge.install()
        AppleSignInBridge.install()
        application.registerForRemoteNotifications()
        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        IosLogKt.iosLog(tag: "APNs", message: "registration ok, token bytes: \(deviceToken.count)")
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        IosLogKt.iosLogError(tag: "APNs", message: "registration failed: \(error.localizedDescription)")
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        KMPNotifier.shared.onApplicationDidReceiveRemoteNotification(userInfo: userInfo)
        completionHandler(.newData)
    }
}

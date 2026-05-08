import UIKit
import FirebaseCore
import GoogleSignIn
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate {
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
        NotifierManager.shared.initialize(
            configuration: NotificationPlatformConfigurationIos(
                showPushNotification: true,
                askNotificationPermissionOnStart: false,
                notificationSoundName: nil
            )
        )
        GoogleSignInBridge.install()
        return true
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
        NotifierManager.shared.onApplicationDidReceiveRemoteNotification(userInfo: userInfo)
        completionHandler(.newData)
    }
}

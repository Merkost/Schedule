import UIKit
import GoogleSignIn
import ComposeApp

enum GoogleSignInBridge {

    static func install() {
        configureIfNeeded()
        IosGoogleAuthBridgeKt.installSignInBridge(
            signIn: { onResult in
                DispatchQueue.main.async {
                    guard let presenter = topViewController() else {
                        onResult(nil, "no presenter")
                        return
                    }
                    GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
                        if let error = error {
                            onResult(nil, error.localizedDescription)
                            return
                        }
                        guard let idToken = result?.user.idToken?.tokenString else {
                            onResult(nil, "no idToken")
                            return
                        }
                        onResult(idToken, nil)
                    }
                }
            },
            signOut: {
                GIDSignIn.sharedInstance.signOut()
            }
        )
    }

    private static func configureIfNeeded() {
        guard
            let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
            let plist = NSDictionary(contentsOfFile: path),
            let clientId = plist["CLIENT_ID"] as? String
        else {
            return
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientId)
    }

    private static func topViewController(
        base: UIViewController? = UIApplication.shared
            .connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first(where: { $0.isKeyWindow })?.rootViewController
    ) -> UIViewController? {
        if let nav = base as? UINavigationController {
            return topViewController(base: nav.visibleViewController)
        }
        if let tab = base as? UITabBarController, let selected = tab.selectedViewController {
            return topViewController(base: selected)
        }
        if let presented = base?.presentedViewController {
            return topViewController(base: presented)
        }
        return base
    }
}

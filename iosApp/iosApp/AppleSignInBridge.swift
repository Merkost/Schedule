import AuthenticationServices
import CryptoKit
import FirebaseAuth
import UIKit
import ComposeApp

enum AppleSignInBridge {

    static func install() {
        IosAppleAuthBridgeKt.installAppleSignInBridge(
            signIn: { onResult in
                DispatchQueue.main.async {
                    Coordinator.shared.start(onResult: onResult)
                }
            }
        )
    }

    private final class Coordinator: NSObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
        static let shared = Coordinator()

        private var currentNonce: String?
        private var onResult: ((String?) -> Void)?

        func start(onResult: @escaping (String?) -> Void) {
            self.onResult = onResult

            let nonce = Self.randomNonceString()
            currentNonce = nonce

            let request = ASAuthorizationAppleIDProvider().createRequest()
            request.requestedScopes = [.fullName, .email]
            request.nonce = Self.sha256(nonce)

            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            controller.performRequests()
        }

        func authorizationController(
            controller: ASAuthorizationController,
            didCompleteWithAuthorization authorization: ASAuthorization
        ) {
            guard
                let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                let nonce = currentNonce,
                let tokenData = credential.identityToken,
                let idToken = String(data: tokenData, encoding: .utf8)
            else {
                finish(error: "Apple ID credential malformed")
                return
            }

            let firebaseCredential = OAuthProvider.appleCredential(
                withIDToken: idToken,
                rawNonce: nonce,
                fullName: credential.fullName
            )

            Auth.auth().signIn(with: firebaseCredential) { [weak self] _, error in
                if let error = error {
                    self?.finish(error: error.localizedDescription)
                } else {
                    self?.finish(error: nil)
                }
            }
        }

        func authorizationController(
            controller: ASAuthorizationController,
            didCompleteWithError error: Error
        ) {
            let nsError = error as NSError
            if nsError.code == ASAuthorizationError.canceled.rawValue {
                finish(error: "canceled")
            } else {
                finish(error: error.localizedDescription)
            }
        }

        func presentationAnchor(
            for controller: ASAuthorizationController
        ) -> ASPresentationAnchor {
            UIApplication.shared
                .connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap { $0.windows }
                .first(where: { $0.isKeyWindow }) ?? ASPresentationAnchor()
        }

        private func finish(error: String?) {
            let callback = onResult
            onResult = nil
            currentNonce = nil
            callback?(error)
        }

        private static func sha256(_ input: String) -> String {
            let hashed = SHA256.hash(data: Data(input.utf8))
            return hashed.compactMap { String(format: "%02x", $0) }.joined()
        }

        private static func randomNonceString(length: Int = 32) -> String {
            precondition(length > 0)
            let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._")
            var result = ""
            var remaining = length
            while remaining > 0 {
                var random: UInt8 = 0
                let status = SecRandomCopyBytes(kSecRandomDefault, 1, &random)
                if status != errSecSuccess { continue }
                if random < charset.count {
                    result.append(charset[Int(random)])
                    remaining -= 1
                }
            }
            return result
        }
    }
}

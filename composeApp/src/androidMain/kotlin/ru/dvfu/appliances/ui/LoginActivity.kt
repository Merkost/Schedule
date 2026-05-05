package ru.dvfu.appliances.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.dvfu.appliances.Logger
import ru.dvfu.appliances.R
import ru.dvfu.appliances.compose.MainActivity
import ru.dvfu.appliances.compose.ui.theme.ScheduleTheme
import ru.dvfu.appliances.compose.viewmodels.LoginViewModel
import ru.dvfu.appliances.model.repository.entity.User

class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModel()
    private val logger: Logger by inject()
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val uiState = MutableStateFlow(LoginUiState())

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            handleError(e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()
        googleSignInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build(),
        )

        lifecycleScope.launch {
            viewModel.subscribe().collect { state ->
                when (state) {
                    is BaseViewState.Success<*> -> onLoginSuccess(state.data as User?)
                    is BaseViewState.Loading -> uiState.update { it.copy(loading = true) }
                    is BaseViewState.Error -> handleError(state.error)
                }
            }
        }

        setContent {
            ScheduleTheme {
                val state by uiState.asStateFlow().collectAsState()
                LoginScreen(
                    state = state,
                    onGoogleClick = ::startGoogleLogin,
                    onMicrosoftClick = ::startMicrosoftLogin,
                    onGuestClick = ::startGuestLogin,
                    onErrorShown = { uiState.update { it.copy(errorMessage = null) } },
                )
            }
        }
    }

    private fun onLoginSuccess(user: User?) {
        uiState.update { it.copy(loading = false) }
        if (user != null) {
            auth.currentUser?.let(::announceSignIn)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun announceSignIn(user: com.google.firebase.auth.FirebaseUser) {
        val label = user.email ?: user.displayName
        val text = if (user.isAnonymous || label.isNullOrEmpty()) "Успешный вход"
        else "Успешный вход в $label"
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    private fun handleError(error: Throwable?) {
        val msg = error?.message ?: getString(R.string.error_occured)
        uiState.update { it.copy(loading = false, errorMessage = msg) }
        logger.log(error?.message)
    }

    private fun startGoogleLogin() {
        uiState.update { it.copy(loading = true) }
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun startMicrosoftLogin() {
        uiState.update { it.copy(loading = true) }
        val provider = OAuthProvider.newBuilder("microsoft.com")
        provider.scopes = arrayListOf("calendars.read")

        val pending = auth.pendingAuthResult
        if (pending != null) {
            pending
                .addOnSuccessListener { result: AuthResult ->
                    Log.d("LoginActivity", "Microsoft pending success: ${result.user?.email}")
                }
                .addOnFailureListener(::handleError)
        } else {
            auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnFailureListener(::handleError)
        }
    }

    private fun startGuestLogin() {
        uiState.update { it.copy(loading = true) }
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) handleError(task.exception)
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) handleError(task.exception)
            }
    }
}

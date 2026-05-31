package com.example.globerouter.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.example.globerouter.data.CredentialStore
import com.example.globerouter.data.RouterApi
import com.example.globerouter.data.models.Credentials
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
  onLoginSuccess: () -> Unit,
  modifier: Modifier = Modifier,
  credentialStore: CredentialStore? = null,
) {
  val context = LocalContext.current
  val store = credentialStore ?: remember { CredentialStore(context.applicationContext) }
  var username by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var saveLogin by rememberSaveable { mutableStateOf(store.hasSaved()) }
  var error by remember { mutableStateOf<String?>(null) }
  var loading by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  // Auto-login on first composition if saved credentials exist
  LaunchedEffect(Unit) {
    val saved = store.load()
    if (saved != null) {
      username = saved.first
      password = saved.second
      saveLogin = true
      loading = true
      try {
        val api = RouterApi()
        val ok = api.login(username, password)
        api.close()
        if (ok) {
          Credentials.username = username
          Credentials.password = password
          onLoginSuccess()
        } else {
          error = "Login failed — credentials may have changed"
          store.clear()
          loading = false
        }
      } catch (e: Exception) {
        error = e.message ?: "Connection failed"
        loading = false
      }
    }
  }

  fun doLogin() {
    scope.launch {
      loading = true
      error = null
      try {
        val api = RouterApi()
        val ok = api.login(username, password)
        api.close()
        if (ok) {
          Credentials.username = username
          Credentials.password = password
          if (saveLogin) store.save(username, password) else store.clear()
          onLoginSuccess()
        } else {
          error = "Login failed — check credentials"
        }
      } catch (e: Exception) {
        error = e.message ?: "Connection failed"
      } finally {
        loading = false
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(64.dp))
    Spacer(Modifier.height(16.dp))
    Text("Globe Router", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("Connect to your router", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(32.dp))

    OutlinedTextField(
      value = username,
      onValueChange = { username = it; error = null },
      label = { Text("Username") },
      singleLine = true,
      enabled = !loading,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
      modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
      value = password,
      onValueChange = { password = it; error = null },
      label = { Text("Password") },
      singleLine = true,
      enabled = !loading,
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = {
        if (username.isNotBlank() && password.isNotBlank() && !loading) {
          doLogin()
        }
      }),
      modifier = Modifier.fillMaxWidth(),
    )
    if (error != null) {
      Spacer(Modifier.height(8.dp))
      Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(12.dp))
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Checkbox(
        checked = saveLogin,
        onCheckedChange = { saveLogin = it },
        enabled = !loading,
      )
      Spacer(Modifier.width(4.dp))
      Text("Save login", style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(16.dp))
    Button(
      onClick = dropUnlessResumed { doLogin() },
      modifier = Modifier.fillMaxWidth(),
      enabled = !loading && username.isNotBlank() && password.isNotBlank(),
    ) {
      if (loading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
      } else {
        Text("Connect")
      }
    }
  }
}

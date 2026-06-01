package com.example.globerouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.globerouter.data.CredentialStore
import com.example.globerouter.data.models.Credentials
import com.example.globerouter.ui.band.BandLockScreen
import com.example.globerouter.ui.dashboard.DashboardScreen
import com.example.globerouter.ui.login.LoginScreen

private object LoginRoute
private object DashboardRoute
private object BandLockRoute

@Composable
fun MainNavigation(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val store = remember { CredentialStore(context) }
  val backStack = remember { mutableStateListOf<Any>(LoginRoute) }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = { key ->
      when (key) {
        is LoginRoute -> NavEntry(key) {
          LoginScreen(
            onLoginSuccess = {
              backStack.clear()
              backStack.add(DashboardRoute)
            },
            credentialStore = store,
          )
        }
        is DashboardRoute -> NavEntry(key) {
          DashboardScreen(
            onLogout = {
              store.clear()
              Credentials.username = ""
              Credentials.password = ""
              backStack.clear()
              backStack.add(LoginRoute)
            },
            onBandLock = { backStack.add(BandLockRoute) },
          )
        }
        is BandLockRoute -> NavEntry(key) {
          BandLockScreen(onBack = { backStack.removeLastOrNull() })
        }
        else -> error("Unknown route: $key")
      }
    },
  )
}

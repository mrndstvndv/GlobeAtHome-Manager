package com.example.globerouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.globerouter.ui.dashboard.DashboardScreen
import com.example.globerouter.ui.login.LoginScreen

private object LoginRoute
private object DashboardRoute

@Composable
fun MainNavigation(modifier: Modifier = Modifier) {
  val backStack = remember { mutableStateListOf<Any>(LoginRoute) }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = { key ->
      when (key) {
        is LoginRoute -> NavEntry(key) {
          LoginScreen(
            onLoginSuccess = { backStack.add(DashboardRoute) },
          )
        }
        is DashboardRoute -> NavEntry(key) {
          DashboardScreen(
            onLogout = {
              backStack.clear()
              backStack.add(LoginRoute)
            },
          )
        }
        else -> error("Unknown route: $key")
      }
    },
  )
}

package com.example.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.DebtDirection
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.components.AccountEditorSheet
import com.example.ui.components.BiometricLockScreen
import com.example.ui.components.ContributeSavingsGoalSheet
import com.example.ui.components.CreateDebtSheet
import com.example.ui.components.CreateSavingsGoalSheet
import com.example.ui.components.DebtDetailAndPaymentSheet
import com.example.ui.components.TransactionEditorSheet
import com.example.ui.navigation.AccountsRoute
import com.example.ui.navigation.DashboardRoute
import com.example.ui.navigation.DebtsRoute
import com.example.ui.navigation.PlanningRoute
import com.example.ui.navigation.TransactionsRoute
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtsScreen
import com.example.ui.screens.PlanningAndSecurityScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.KmaAccountantTheme
import com.example.ui.viewmodel.AccountantViewModel
import com.example.ui.viewmodel.UiEvent

private data class TopLevelNavItem(
  val route: Any,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val TOP_LEVEL_ITEMS =
  listOf(
    TopLevelNavItem(
      route = DashboardRoute,
      label = "الرئيسية",
      selectedIcon = Icons.Filled.Dashboard,
      unselectedIcon = Icons.Outlined.Dashboard,
      testTag = "nav_dashboard",
    ),
    TopLevelNavItem(
      route = TransactionsRoute,
      label = "المعاملات",
      selectedIcon = Icons.Filled.ReceiptLong,
      unselectedIcon = Icons.Outlined.ReceiptLong,
      testTag = "nav_transactions",
    ),
    TopLevelNavItem(
      route = AccountsRoute,
      label = "الحسابات",
      selectedIcon = Icons.Filled.AccountBalanceWallet,
      unselectedIcon = Icons.Outlined.AccountBalanceWallet,
      testTag = "nav_accounts",
    ),
    TopLevelNavItem(
      route = DebtsRoute,
      label = "الديون",
      selectedIcon = Icons.Filled.Handshake,
      unselectedIcon = Icons.Outlined.Handshake,
      testTag = "nav_debts",
    ),
    TopLevelNavItem(
      route = PlanningRoute,
      label = "التخطيط",
      selectedIcon = Icons.Filled.Tune,
      unselectedIcon = Icons.Outlined.Tune,
      testTag = "nav_planning",
    ),
  )

@Composable
fun KmaAccountantApp(
  viewModel: AccountantViewModel,
  activity: FragmentActivity?,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
  val lockError by viewModel.lockError.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
        is UiEvent.ShareBackupJson -> {
          val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
              type = "application/json"
              putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - محاسبك الشخصي KMA")
              putExtra(Intent.EXTRA_TEXT, event.jsonPayload)
            }
          val chooser = Intent.createChooser(sendIntent, "حفظ أو مشاركة النسخة الاحتياطية")
          context.startActivity(chooser)
        }
      }
    }
  }

  LaunchedEffect(uiState.preferences.biometricLockEnabled, isUnlocked) {
    if (uiState.preferences.biometricLockEnabled && !isUnlocked && activity != null) {
      viewModel.requestBiometricUnlock(activity)
    }
  }

  KmaAccountantTheme(themeMode = uiState.preferences.themeMode) {
    if (uiState.preferences.biometricLockEnabled && !isUnlocked) {
      BiometricLockScreen(
        errorMessage = lockError,
        onUnlockClick = {
          if (activity != null) {
            viewModel.requestBiometricUnlock(activity)
          }
        },
      )
      return@KmaAccountantTheme
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Modal Sheet States
    var showTxSheet by remember { mutableStateOf(false) }
    var txInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }

    var showAccountSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }

    var showCreateDebtSheet by remember { mutableStateOf(false) }
    var debtInitialDirection by remember { mutableStateOf(DebtDirection.OWED_BY_ME) }
    var selectedDebtForDetail by remember { mutableStateOf<DebtEntity?>(null) }

    var showCreateGoalSheet by remember { mutableStateOf(false) }
    var selectedGoalForContribution by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    fun navigateToTopLevel(route: Any) {
      navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
      }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 600.dp

      Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (!useNavRail) {
            NavigationBar {
              TOP_LEVEL_ITEMS.forEach { item ->
                val selected = currentDestination?.hasRoute(item.route::class) == true
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateToTopLevel(item.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = item.label,
                    )
                  },
                  label = { Text(item.label, maxLines = 1) },
                  modifier = Modifier.testTag(item.testTag),
                )
              }
            }
          }
        },
        floatingActionButton = {
          val isOnDebts = currentDestination?.hasRoute(DebtsRoute::class) == true
          val isOnAccounts = currentDestination?.hasRoute(AccountsRoute::class) == true
          FloatingActionButton(
            onClick = {
              when {
                isOnDebts -> {
                  debtInitialDirection = DebtDirection.OWED_BY_ME
                  showCreateDebtSheet = true
                }
                isOnAccounts -> {
                  editingAccount = null
                  showAccountSheet = true
                }
                else -> {
                  editingTx = null
                  txInitialType = TransactionType.EXPENSE
                  showTxSheet = true
                }
              }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag("main_fab"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = "إضافة جديدة")
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (useNavRail) {
            NavigationRail(modifier = Modifier.fillMaxHeight()) {
              TOP_LEVEL_ITEMS.forEach { item ->
                val selected = currentDestination?.hasRoute(item.route::class) == true
                NavigationRailItem(
                  selected = selected,
                  onClick = { navigateToTopLevel(item.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = item.label,
                    )
                  },
                  label = { Text(item.label) },
                  modifier = Modifier.testTag(item.testTag),
                )
              }
            }
          }

          Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
          ) {
            NavHost(
              navController = navController,
              startDestination = DashboardRoute,
              modifier = Modifier.fillMaxSize().widthIn(max = 840.dp),
            ) {
              composable<DashboardRoute> {
                DashboardScreen(
                  state = uiState,
                  onToggleHideBalances = viewModel::toggleHideBalances,
                  onLockNow = viewModel::lockAppNow,
                  onQuickTransaction = { type ->
                    editingTx = null
                    txInitialType = type
                    showTxSheet = true
                  },
                  onCreateAccount = {
                    editingAccount = null
                    showAccountSheet = true
                  },
                  onCreateDebt = {
                    debtInitialDirection = DebtDirection.OWED_BY_ME
                    showCreateDebtSheet = true
                  },
                  onOpenTransaction = { tx ->
                    editingTx = tx
                    showTxSheet = true
                  },
                  onOpenDebt = { debt -> selectedDebtForDetail = debt },
                  onNavigateToTransactions = { navigateToTopLevel(TransactionsRoute) },
                  onNavigateToDebts = { navigateToTopLevel(DebtsRoute) },
                  onNavigateToPlanning = { navigateToTopLevel(PlanningRoute) },
                )
              }

              composable<TransactionsRoute> {
                TransactionsScreen(
                  state = uiState,
                  onAddTransaction = {
                    editingTx = null
                    txInitialType = TransactionType.EXPENSE
                    showTxSheet = true
                  },
                  onOpenTransaction = { tx ->
                    editingTx = tx
                    showTxSheet = true
                  },
                )
              }

              composable<AccountsRoute> {
                AccountsScreen(
                  state = uiState,
                  onAddAccount = {
                    editingAccount = null
                    showAccountSheet = true
                  },
                  onEditAccount = { acc ->
                    editingAccount = acc
                    showAccountSheet = true
                  },
                  onSetDefaultAccount = viewModel::setDefaultAccount,
                  onTransferBetweenAccounts = {
                    editingTx = null
                    txInitialType = TransactionType.TRANSFER
                    showTxSheet = true
                  },
                )
              }

              composable<DebtsRoute> {
                DebtsScreen(
                  state = uiState,
                  onCreateDebt = { dir ->
                    debtInitialDirection = dir
                    showCreateDebtSheet = true
                  },
                  onOpenDebt = { debt -> selectedDebtForDetail = debt },
                )
              }

              composable<PlanningRoute> {
                PlanningAndSecurityScreen(
                  state = uiState,
                  onCreateSavingsGoal = { showCreateGoalSheet = true },
                  onContributeGoal = { goal -> selectedGoalForContribution = goal },
                  onDeleteGoal = viewModel::deleteSavingsGoal,
                  onUpdateCategoryBudget = viewModel::updateCategoryBudget,
                  onToggleBiometricLock = { enable ->
                    if (activity != null) {
                      viewModel.toggleBiometricLock(activity, enable)
                    }
                  },
                  onSelectCurrency = viewModel::setCurrency,
                  onSelectThemeMode = viewModel::setThemeMode,
                  onExportBackup = viewModel::exportBackup,
                  onResetAllData = viewModel::resetAllData,
                )
              }
            }
          }
        }
      }
    }

    // --- Modal Bottom Sheets ---
    if (showTxSheet) {
      TransactionEditorSheet(
        accounts = uiState.accounts,
        categories = uiState.categories,
        currency = uiState.preferences.currency,
        initialType = txInitialType,
        existingTransaction = editingTx,
        onDismiss = {
          showTxSheet = false
          editingTx = null
        },
        onCreateAccountRequested = {
          editingAccount = null
          showAccountSheet = true
        },
        onSaveNew = viewModel::addTransaction,
        onUpdateExisting = viewModel::updateTransaction,
        onDeleteExisting = viewModel::deleteTransaction,
      )
    }

    if (showAccountSheet) {
      AccountEditorSheet(
        currency = uiState.preferences.currency,
        existingAccount = editingAccount,
        onDismiss = {
          showAccountSheet = false
          editingAccount = null
        },
        onCreate = viewModel::createAccount,
        onUpdate = viewModel::updateAccount,
        onDelete = viewModel::deleteAccount,
      )
    }

    if (showCreateDebtSheet) {
      CreateDebtSheet(
        accounts = uiState.accounts,
        currency = uiState.preferences.currency,
        initialDirection = debtInitialDirection,
        onDismiss = { showCreateDebtSheet = false },
        onCreateDebt = viewModel::createDebt,
      )
    }

    selectedDebtForDetail?.let { debt ->
      val freshDebt = uiState.debts.firstOrNull { it.id == debt.id } ?: debt
      val debtInstallments = uiState.installments.filter { it.debtId == freshDebt.id }
      DebtDetailAndPaymentSheet(
        debt = freshDebt,
        installments = debtInstallments,
        accounts = uiState.accounts,
        currency = uiState.preferences.currency,
        onDismiss = { selectedDebtForDetail = null },
        onRecordPayment = viewModel::recordDebtPayment,
        onDeleteDebt = viewModel::deleteDebt,
      )
    }

    if (showCreateGoalSheet) {
      CreateSavingsGoalSheet(
        currency = uiState.preferences.currency,
        onDismiss = { showCreateGoalSheet = false },
        onCreate = { name, targetMinor, colorHex ->
          viewModel.createSavingsGoal(name, targetMinor, null, colorHex)
        },
      )
    }

    selectedGoalForContribution?.let { goal ->
      val freshGoal = uiState.savingsGoals.firstOrNull { it.id == goal.id } ?: goal
      ContributeSavingsGoalSheet(
        goal = freshGoal,
        accounts = uiState.accounts,
        currency = uiState.preferences.currency,
        onDismiss = { selectedGoalForContribution = null },
        onSubmit = viewModel::contributeToSavingsGoal,
      )
    }
  }
}

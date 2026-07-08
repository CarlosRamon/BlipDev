package br.com.bliqbrasil.totem.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.com.bliqbrasil.totem.BliqTotemApp
import br.com.bliqbrasil.totem.data.model.WashOptionExtra
import br.com.bliqbrasil.totem.ui.screens.activation.ActivationScreen
import br.com.bliqbrasil.totem.ui.screens.support.SupportScreen
import br.com.bliqbrasil.totem.ui.screens.activation.ActivationViewModel
import br.com.bliqbrasil.totem.ui.screens.checkout.CheckoutScreen
import br.com.bliqbrasil.totem.ui.screens.checkout.CheckoutViewModel
import br.com.bliqbrasil.totem.ui.screens.cpfinput.CpfInputScreen
import br.com.bliqbrasil.totem.ui.screens.cpfinput.CpfInputViewModel
import br.com.bliqbrasil.totem.ui.screens.extras.ExtrasScreen
import br.com.bliqbrasil.totem.ui.screens.home.HomeScreen
import br.com.bliqbrasil.totem.ui.screens.home.HomeViewModel
import br.com.bliqbrasil.totem.ui.screens.minutespicker.MinutesPickerScreen
import br.com.bliqbrasil.totem.ui.screens.payment.PaymentScreen
import br.com.bliqbrasil.totem.ui.screens.payment.PaymentViewModel
import br.com.bliqbrasil.totem.ui.screens.session.SessionScreen
import br.com.bliqbrasil.totem.ui.screens.session.SessionViewModel
import br.com.bliqbrasil.totem.ui.screens.success.SuccessScreen
import br.com.bliqbrasil.totem.ui.screens.success.SuccessViewModel

@Composable
fun NavGraph(navController: NavHostController, app: BliqTotemApp) {


    val startDestination: Any = if (app.tokenStorage.getToken() != null)
        AppDestinations.Home else AppDestinations.Activation

    NavHost(navController = navController, startDestination = startDestination) {

        composable<AppDestinations.Activation> {
            val vm = viewModel<ActivationViewModel>(
                factory = ActivationViewModel.factory(app.repository, app.tokenStorage)
            )
            ActivationScreen(
                viewModel = vm,
                onActivated = {
                    navController.navigate(AppDestinations.Home) {
                        popUpTo(AppDestinations.Activation) { inclusive = true }
                    }
                }
            )
        }

        composable<AppDestinations.Home> {
            val vm = viewModel<HomeViewModel>(
                factory = HomeViewModel.factory(app.repository, app.bleManager, app.tokenStorage)
            )
            HomeScreen(
                viewModel = vm,
                onNavigateToExtras = { option, boxTipo ->
                    navController.navigate(AppDestinations.Extras(option.toJson(), boxTipo))
                },
                onNavigateToMinutesPicker = { option, boxTipo ->
                    navController.navigate(AppDestinations.MinutesPicker(option.toJson(), boxTipo))
                },
                onNavigateToCheckout = { option, extras, minutes, price, boxTipo ->
                    navController.navigate(
                        AppDestinations.CpfInput(
                            washOptionJson     = option.toJson(),
                            selectedExtrasJson = extras.toJson(),
                            totalMinutes       = minutes,
                            totalPrice         = price,
                            boxTipo            = boxTipo,
                        )
                    )
                },
                onNavigateToSession = { cicloId, totalMinutes, resumeFromSeconds, boxTipo ->
                    navController.navigate(
                        AppDestinations.Session(
                            cicloId            = cicloId,
                            totalMinutes       = totalMinutes,
                            boxTipo            = boxTipo,
                            resumeFromSeconds  = resumeFromSeconds,
                        )
                    ) { popUpTo(AppDestinations.Home) { inclusive = false } }
                },
                onNeedActivation = {
                    navController.navigate(AppDestinations.Activation) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSupport = {
                    navController.navigate(AppDestinations.Support)
                },
            )
        }

        composable<AppDestinations.MinutesPicker> { entry ->
            val route = entry.toRoute<AppDestinations.MinutesPicker>()
            val washOption = route.washOptionJson.toWashOption()
            MinutesPickerScreen(
                washOption = washOption,
                onBack = { navController.popBackStack() },
                onContinue = { minutes, totalPrice ->
                    navController.navigate(
                        AppDestinations.CpfInput(
                            washOptionJson     = washOption.toJson(),
                            selectedExtrasJson = emptyList<WashOptionExtra>().toJson(),
                            totalMinutes       = minutes,
                            totalPrice         = totalPrice,
                            boxTipo            = route.boxTipo,
                        )
                    )
                }
            )
        }

        composable<AppDestinations.Extras> { entry ->
            val route = entry.toRoute<AppDestinations.Extras>()
            val washOption = route.washOptionJson.toWashOption()
            ExtrasScreen(
                washOption = washOption,
                onBack = { navController.popBackStack() },
                onContinue = { extras, minutes, price ->
                    navController.navigate(
                        AppDestinations.CpfInput(
                            washOptionJson     = washOption.toJson(),
                            selectedExtrasJson = extras.toJson(),
                            totalMinutes       = minutes,
                            totalPrice         = price,
                            boxTipo            = route.boxTipo,
                        )
                    )
                }
            )
        }

        composable<AppDestinations.CpfInput> { entry ->
            val route = entry.toRoute<AppDestinations.CpfInput>()
            val vm = viewModel<CpfInputViewModel>(
                factory = CpfInputViewModel.factory(app.repository)
            )
            CpfInputScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onContinue = { clienteId ->
                    navController.navigate(
                        AppDestinations.Checkout(
                            washOptionJson     = route.washOptionJson,
                            selectedExtrasJson = route.selectedExtrasJson,
                            totalMinutes       = route.totalMinutes,
                            totalPrice         = route.totalPrice,
                            boxTipo            = route.boxTipo,
                            clienteId          = clienteId,
                        )
                    )
                }
            )
        }

        composable<AppDestinations.Checkout> { entry ->
            val route = entry.toRoute<AppDestinations.Checkout>()
            val washOption     = route.washOptionJson.toWashOption()
            val selectedExtras = route.selectedExtrasJson.toWashOptionExtras()
            val vm = viewModel<CheckoutViewModel>(
                factory = CheckoutViewModel.factory(
                    washOption     = washOption,
                    selectedExtras = selectedExtras,
                    totalMinutes   = route.totalMinutes,
                    totalPrice     = route.totalPrice,
                )
            )
            CheckoutScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onConfirm = { method ->
                    navController.navigate(
                        AppDestinations.Payment(
                            washOptionJson     = washOption.toJson(),
                            selectedExtrasJson = selectedExtras.toJson(),
                            totalMinutes       = route.totalMinutes,
                            totalPrice         = route.totalPrice,
                            paymentMethod      = method.toNavString(),
                            boxTipo            = route.boxTipo,
                            clienteId          = route.clienteId,
                        )
                    )
                }
            )
        }

        composable<AppDestinations.Payment> { entry ->
            val route = entry.toRoute<AppDestinations.Payment>()
            val vm = viewModel<PaymentViewModel>(
                factory = PaymentViewModel.Factory(
                    stone          = app.stonePaymentManager,
                    repository     = app.repository,
                    washOption     = route.washOptionJson.toWashOption(),
                    selectedExtras = route.selectedExtrasJson.toWashOptionExtras(),
                    totalMinutes   = route.totalMinutes,
                    totalPrice     = route.totalPrice,
                    paymentMethod  = route.paymentMethod.toPaymentMethod(),
                    clienteId      = route.clienteId,
                )
            )
            PaymentScreen(
                viewModel = vm,
                onSuccess = { cicloId, acquirerKey ->
                    navController.navigate(
                        AppDestinations.Success(
                            paymentMethod = route.paymentMethod,
                            totalMinutes  = route.totalMinutes,
                            totalPrice    = route.totalPrice,
                            cicloId       = cicloId,
                            transacaoId   = acquirerKey,
                            boxTipo       = route.boxTipo,
                        )
                    ) { popUpTo(AppDestinations.Checkout::class) { inclusive = true } }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<AppDestinations.Success> { entry ->
            val route = entry.toRoute<AppDestinations.Success>()
            val vm = viewModel<SuccessViewModel>(
                factory = SuccessViewModel.factory(
                    repository   = app.repository,
                    bleManager   = app.bleManager,
                    cicloId      = route.cicloId,
                    totalMinutes = route.totalMinutes,
                )
            )
            SuccessScreen(
                viewModel     = vm,
                paymentMethod = route.paymentMethod.toPaymentMethod(),
                totalMinutes  = route.totalMinutes,
                totalPrice    = route.totalPrice,
                onSessionReady = { cicloId ->
                    navController.navigate(
                        AppDestinations.Session(
                            cicloId       = cicloId,
                            totalMinutes  = route.totalMinutes,
                            boxTipo       = route.boxTipo,
                            paymentMethod = route.paymentMethod,
                            totalPrice    = route.totalPrice,
                        )
                    ) { popUpTo(AppDestinations.Success::class) { inclusive = true } }
                },
                onBackToHome = {
                    navController.navigate(AppDestinations.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<AppDestinations.Session> { entry ->
            val route = entry.toRoute<AppDestinations.Session>()
            val vm = viewModel<SessionViewModel>(
                factory = SessionViewModel.factory(
                    repository        = app.repository,
                    bleManager        = app.bleManager,
                    cicloId           = route.cicloId,
                    totalMinutes      = route.totalMinutes,
                    resumeFromSeconds = route.resumeFromSeconds,
                    boxTipo           = route.boxTipo,
                )
            )
            SessionScreen(
                viewModel  = vm,
                onFinished = {
                    navController.navigate(AppDestinations.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<AppDestinations.Support> {
            SupportScreen(onBack = { navController.popBackStack() })
        }
    }
}
